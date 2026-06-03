@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0"

set "BACKEND_PORT=8787"

if not exist ".env" (
    if exist ".env.example" (
        copy ".env.example" ".env" >nul
    )
)

findstr /B /C:"PORT=" ".env" >nul 2>nul
if not errorlevel 1 (
    for /f "tokens=1,* delims==" %%a in ('findstr /B /C:"PORT=" ".env"') do set "BACKEND_PORT=%%b"
)

findstr /B /C:"YOLO_DETECT_URL=" ".env" >nul 2>nul
if errorlevel 1 (
    echo YOLO_DETECT_URL=http://127.0.0.1:8790/detect>>".env"
    echo YOLO_TIMEOUT_MS=20000>>".env"
    echo YOLO_MODEL_ID=original_yolov8s>>".env"
)

call :check_port "%BACKEND_PORT%" BACKEND_PORT_BUSY
if "!BACKEND_PORT_BUSY!"=="1" (
    call :check_backend_health "%BACKEND_PORT%" BACKEND_ALREADY_RUNNING
    if "!BACKEND_ALREADY_RUNNING!"=="1" (
        echo AI backend da dang chay tai http://127.0.0.1:%BACKEND_PORT%.
        echo Khong khoi dong them de tranh xung dot port. Hay dung terminal backend dang mo.
        exit /b 0
    )
    echo Port %BACKEND_PORT% dang bi chuong trinh khac su dung.
    call :show_port_owner "%BACKEND_PORT%"
    echo Hay dong tien trinh dang chiem port hoac doi PORT trong backend\.env roi chay lai.
    exit /b 1
)

echo Chay AI backend tai http://127.0.0.1:%BACKEND_PORT%
echo Luu y: neu vua sua .env, terminal nay phai duoc restart de doc cau hinh moi.
node server.js

endlocal
exit /b 0

:check_port
set "%~2=0"
set "PORT_TO_CHECK=%~1"
powershell -NoProfile -Command "try { $client = New-Object Net.Sockets.TcpClient; $iar = $client.BeginConnect('127.0.0.1', [int]$env:PORT_TO_CHECK, $null, $null); if ($iar.AsyncWaitHandle.WaitOne(1000, $false)) { $client.EndConnect($iar); $client.Close(); exit 0 } else { $client.Close(); exit 1 } } catch { exit 1 }" >nul 2>nul
if not errorlevel 1 set "%~2=1"
exit /b 0

:check_backend_health
set "%~2=0"
set "PORT_TO_CHECK=%~1"
powershell -NoProfile -Command "try { $h = Invoke-RestMethod -Uri ('http://127.0.0.1:' + $env:PORT_TO_CHECK + '/health') -TimeoutSec 2; if ($h.ok -eq $true) { exit 0 }; exit 1 } catch { exit 1 }" >nul 2>nul
if not errorlevel 1 set "%~2=1"
exit /b 0

:show_port_owner
set "PORT_TO_CHECK=%~1"
powershell -NoProfile -Command "$port = [int]$env:PORT_TO_CHECK; try { Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction Stop | Select-Object -First 5 LocalAddress,LocalPort,OwningProcess,@{n='ProcessName';e={(Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue).ProcessName}} | Format-Table -AutoSize } catch { netstat -ano | findstr (':' + $port) }"
exit /b 0
