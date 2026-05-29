$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $repoRoot "backend"
$frontendDir = Join-Path $repoRoot "frontend/web-console"

Write-Host "[1/4] project-service tests (PS-03)"
& mvn -pl project-service -am test -q -f (Join-Path $backendDir "pom.xml")

Write-Host "[2/4] api-gateway tests (GW-03)"
& mvn -pl api-gateway -am test -q -f (Join-Path $backendDir "pom.xml")

Write-Host "[3/4] context-service tests (IT-05/06/07)"
& mvn -pl context-service -am test -q -f (Join-Path $backendDir "pom.xml")

Write-Host "[4/4] web-console build (IT-08/09 frontend gate)"
& pnpm.cmd --dir $frontendDir build

Write-Host "MVP smoke suite passed."
