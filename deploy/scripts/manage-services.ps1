param(
    [ValidateSet("start-all", "stop-all", "start", "stop", "restart", "status", "validate")]
    [string]$Action = "status",

    [ValidateSet("postgres", "redis", "qdrant", "api-gateway", "project-service", "context-service", "codegraph-runner", "history-indexer", "knowledge-indexer", "llm-gateway", "dev-agent-service", "web-console", "nginx")]
    [string]$Service
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$deployRoot = (Resolve-Path (Join-Path $scriptRoot "..")).Path
$envFile = Join-Path $deployRoot ".env"
$composeFile = Join-Path $deployRoot "docker-compose.yml"

function Read-DotEnv {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        throw "Environment file not found: $Path"
    }

    $result = @{}
    foreach ($line in Get-Content $Path) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith("#")) {
            continue
        }

        $parts = $trimmed -split "=", 2
        if ($parts.Count -ne 2) {
            continue
        }

        $result[$parts[0].Trim()] = $parts[1].Trim()
    }

    return $result
}

$config = Read-DotEnv -Path $envFile
$projectName = $config["COMPOSE_PROJECT_NAME"]
$postgresContainer = "$projectName-postgres"
$redisContainer = "$projectName-redis"
$servicePorts = @{
    "api-gateway" = $config["API_GATEWAY_PORT"]
    "project-service" = $config["PROJECT_SERVICE_PORT"]
    "context-service" = $config["CONTEXT_SERVICE_PORT"]
    "codegraph-runner" = $config["CODEGRAPH_RUNNER_PORT"]
    "history-indexer" = $config["HISTORY_INDEXER_PORT"]
    "knowledge-indexer" = $config["KNOWLEDGE_INDEXER_PORT"]
    "llm-gateway" = $config["LLM_GATEWAY_PORT"]
    "dev-agent-service" = $config["DEV_AGENT_SERVICE_PORT"]
    "web-console" = $config["WEB_CONSOLE_PORT"]
}

function Invoke-Compose {
    param([string[]]$ComposeArgs)

    & docker compose -p $projectName --env-file $envFile -f $composeFile @ComposeArgs
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose failed: $($ComposeArgs -join ' ')"
    }
}

function Wait-Http {
    param(
        [string]$Name,
        [string]$Uri,
        [int]$RetryCount = 20
    )

    for ($index = 1; $index -le $RetryCount; $index++) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $Uri -TimeoutSec 3
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400) {
                return $response
            }
        }
        catch {
        }

        Start-Sleep -Seconds 2
    }

    throw "$Name health check failed: $Uri"
}

function Get-ResponseText {
    param($Content)

    if ($Content -is [byte[]]) {
        return [System.Text.Encoding]::UTF8.GetString($Content)
    }

    return [string]$Content
}

function Validate-Postgres {
    $postgresUser = $config["POSTGRES_USER"]
    $postgresDb = $config["POSTGRES_DB"]
    $readyArgs = @("exec", $postgresContainer, "pg_isready", "-U", $postgresUser, "-d", $postgresDb)
    $queryArgs = @(
        "exec",
        $postgresContainer,
        "psql",
        "-U",
        $postgresUser,
        "-d",
        $postgresDb,
        "-tAc",
        "select marker_value from app.platform_bootstrap_marker where marker_key = 'platform';"
    )

    & docker @readyArgs | Out-Host
    if ($LASTEXITCODE -ne 0) {
        throw "PostgreSQL readiness check failed"
    }

    $marker = & docker @queryArgs
    if ($LASTEXITCODE -ne 0) {
        throw "PostgreSQL bootstrap marker query failed"
    }

    if ($marker.Trim() -ne "autocode") {
        throw "PostgreSQL bootstrap marker mismatch: $marker"
    }

    Write-Host "PostgreSQL validation passed."
}

function Validate-Redis {
    $redisPassword = $config["REDIS_PASSWORD"]
    $redisArgs = @("exec", $redisContainer, "redis-cli", "-a", $redisPassword, "ping")
    $pong = & docker @redisArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Redis connectivity check failed"
    }

    if ($pong.Trim() -ne "PONG") {
        throw "Redis ping returned unexpected result: $pong"
    }

    Write-Host "Redis validation passed."
}

function Validate-Qdrant {
    $response = Wait-Http -Name "Qdrant" -Uri "http://127.0.0.1:$($config["QDRANT_HTTP_PORT"])/healthz"
    $responseText = (Get-ResponseText -Content $response.Content).Trim()
    if ($responseText -ne "healthz check passed") {
        Write-Host "Qdrant health response: $responseText"
    }

    Write-Host "Qdrant validation passed."
}

function Validate-PlaceholderService {
    param(
        [string]$Name,
        [string]$Uri
    )

    $response = Wait-Http -Name $Name -Uri $Uri
    if ($response.StatusCode -lt 200 -or $response.StatusCode -ge 400) {
        throw "$Name health check returned unexpected status: $($response.StatusCode)"
    }

    Write-Host "$Name validation passed."
}

function Validate-Nginx {
    $health = Wait-Http -Name "Nginx" -Uri "http://127.0.0.1:$($config["NGINX_HOST_PORT"])/healthz"
    $healthText = (Get-ResponseText -Content $health.Content).Trim()
    if ($healthText -ne "ok") {
        throw "Nginx health endpoint returned unexpected content: $healthText"
    }

    $landing = Wait-Http -Name "Nginx landing page" -Uri "http://127.0.0.1:$($config["NGINX_HOST_PORT"])/"
    $landingText = Get-ResponseText -Content $landing.Content
    if ($landingText -notmatch "autocode-bootstrap-ready") {
        throw "Nginx landing page content does not match expectation"
    }

    Write-Host "Nginx validation passed."
}

function Validate-Service {
    param([string]$Target)

    switch ($Target) {
        "postgres" { Validate-Postgres }
        "redis" { Validate-Redis }
        "qdrant" { Validate-Qdrant }
        "api-gateway" { Validate-PlaceholderService -Name "API Gateway" -Uri "http://127.0.0.1:$($servicePorts["api-gateway"])/healthz" }
        "project-service" { Validate-PlaceholderService -Name "Project Service" -Uri "http://127.0.0.1:$($servicePorts["project-service"])/healthz" }
        "context-service" { Validate-PlaceholderService -Name "Context Service" -Uri "http://127.0.0.1:$($servicePorts["context-service"])/healthz" }
        "codegraph-runner" { Validate-PlaceholderService -Name "CodeGraph Runner" -Uri "http://127.0.0.1:$($servicePorts["codegraph-runner"])/healthz" }
        "history-indexer" { Validate-PlaceholderService -Name "History Indexer" -Uri "http://127.0.0.1:$($servicePorts["history-indexer"])/healthz" }
        "knowledge-indexer" { Validate-PlaceholderService -Name "Knowledge Indexer" -Uri "http://127.0.0.1:$($servicePorts["knowledge-indexer"])/healthz" }
        "llm-gateway" { Validate-PlaceholderService -Name "LLM Gateway" -Uri "http://127.0.0.1:$($servicePorts["llm-gateway"])/healthz" }
        "dev-agent-service" { Validate-PlaceholderService -Name "Dev Agent Service" -Uri "http://127.0.0.1:$($servicePorts["dev-agent-service"])/healthz" }
        "web-console" { Validate-PlaceholderService -Name "Web Console" -Uri "http://127.0.0.1:$($servicePorts["web-console"])/healthz" }
        "nginx" { Validate-Nginx }
        default { throw "Unsupported service name: $Target" }
    }
}

function Validate-All {
    Validate-Postgres
    Validate-Redis
    Validate-Qdrant
    Validate-PlaceholderService -Name "API Gateway" -Uri "http://127.0.0.1:$($servicePorts["api-gateway"])/healthz"
    Validate-PlaceholderService -Name "Project Service" -Uri "http://127.0.0.1:$($servicePorts["project-service"])/healthz"
    Validate-PlaceholderService -Name "Context Service" -Uri "http://127.0.0.1:$($servicePorts["context-service"])/healthz"
    Validate-PlaceholderService -Name "CodeGraph Runner" -Uri "http://127.0.0.1:$($servicePorts["codegraph-runner"])/healthz"
    Validate-PlaceholderService -Name "History Indexer" -Uri "http://127.0.0.1:$($servicePorts["history-indexer"])/healthz"
    Validate-PlaceholderService -Name "Knowledge Indexer" -Uri "http://127.0.0.1:$($servicePorts["knowledge-indexer"])/healthz"
    Validate-PlaceholderService -Name "LLM Gateway" -Uri "http://127.0.0.1:$($servicePorts["llm-gateway"])/healthz"
    Validate-PlaceholderService -Name "Dev Agent Service" -Uri "http://127.0.0.1:$($servicePorts["dev-agent-service"])/healthz"
    Validate-PlaceholderService -Name "Web Console" -Uri "http://127.0.0.1:$($servicePorts["web-console"])/healthz"
    Validate-Nginx
}

switch ($Action) {
    "start-all" {
        Invoke-Compose -ComposeArgs @("up", "-d")
        Validate-All
        Invoke-Compose -ComposeArgs @("ps")
    }
    "stop-all" {
        Invoke-Compose -ComposeArgs @("stop")
        Invoke-Compose -ComposeArgs @("ps")
    }
    "start" {
        if (-not $Service) {
            throw "Action=start requires -Service"
        }

        Invoke-Compose -ComposeArgs @("up", "-d", $Service)
        Validate-Service -Target $Service
        Invoke-Compose -ComposeArgs @("ps", $Service)
    }
    "stop" {
        if (-not $Service) {
            throw "Action=stop requires -Service"
        }

        Invoke-Compose -ComposeArgs @("stop", $Service)
        Invoke-Compose -ComposeArgs @("ps", $Service)
    }
    "restart" {
        if (-not $Service) {
            throw "Action=restart requires -Service"
        }

        Invoke-Compose -ComposeArgs @("restart", $Service)
        Validate-Service -Target $Service
        Invoke-Compose -ComposeArgs @("ps", $Service)
    }
    "status" {
        Invoke-Compose -ComposeArgs @("ps")
    }
    "validate" {
        Validate-All
        Invoke-Compose -ComposeArgs @("ps")
    }
}
