@echo off
powershell -ExecutionPolicy Bypass -File "%~dp0scripts\manage-services.ps1" -Action validate
