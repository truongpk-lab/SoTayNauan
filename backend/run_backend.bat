@echo off
setlocal

cd /d "%~dp0"

findstr /B /C:"YOLO_DETECT_URL=" ".env" >nul 2>nul
if errorlevel 1 (
    echo YOLO_DETECT_URL=http://127.0.0.1:8790/detect>>".env"
    echo YOLO_TIMEOUT_MS=20000>>".env"
    echo YOLO_MODEL_ID=original_yolov8s>>".env"
)

echo Chay AI backend tai http://127.0.0.1:8787
echo Luu y: neu vua sua .env, terminal nay phai duoc restart de doc cau hinh moi.
node server.js

endlocal
