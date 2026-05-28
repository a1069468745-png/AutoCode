@echo off
if "%~1"=="" (
  echo Usage: stop-service.cmd postgres^|redis^|qdrant^|nginx
  exit /b 1
)
powershell -ExecutionPolicy Bypass -File "%~dp0scripts\manage-services.ps1" -Action stop -Service %1
