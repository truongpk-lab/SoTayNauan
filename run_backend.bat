@echo off
setlocal

call "%~dp0backend\run_backend.bat"
exit /b %ERRORLEVEL%
