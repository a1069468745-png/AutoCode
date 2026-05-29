$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $repoRoot "backend"

Write-Host "[1/3] project-service tests (PS-03)"
& mvn -pl project-service -am test -q -f (Join-Path $backendDir "pom.xml")

Write-Host "[2/3] api-gateway tests (GW-03)"
& mvn -pl api-gateway -am test -q -f (Join-Path $backendDir "pom.xml")

Write-Host "[3/3] context-service tests (IT-05/06/07)"
& mvn -pl context-service -am test -q -f (Join-Path $backendDir "pom.xml")

Write-Host "MVP smoke suite passed."
