@echo off
if "%~1"=="" (
  echo Usage: start-service.cmd postgres^|redis^|qdrant^|nginx
  exit /b 1
)
powershell -ExecutionPolicy Bypass -File "%~dp0scripts\manage-services.ps1" -Action start -Service %1
