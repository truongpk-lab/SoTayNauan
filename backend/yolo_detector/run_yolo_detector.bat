@echo off
setlocal

cd /d "%~dp0"

if not exist ".venv\Scripts\python.exe" (
    echo [1/3] Tao Python virtual environment...
    python -m venv .venv
    if errorlevel 1 exit /b 1
)

echo [2/3] Cai/cap nhat dependencies...
".venv\Scripts\python.exe" -m pip install -r requirements.txt
if errorlevel 1 exit /b 1

echo [3/3] Chay YOLO detector tai http://127.0.0.1:8790
".venv\Scripts\python.exe" -m uvicorn app:app --host 0.0.0.0 --port 8790

endlocal
