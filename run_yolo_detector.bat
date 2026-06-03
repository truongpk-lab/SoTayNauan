@echo off
setlocal

call "%~dp0backend\yolo_detector\run_yolo_detector.bat"
exit /b %ERRORLEVEL%
