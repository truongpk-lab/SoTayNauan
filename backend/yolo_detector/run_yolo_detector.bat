@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0"

set "YOLO_PORT=8790"

call :check_port "%YOLO_PORT%" YOLO_PORT_BUSY
if "!YOLO_PORT_BUSY!"=="1" (
    call :check_yolo_health "%YOLO_PORT%" YOLO_ALREADY_RUNNING
    if "!YOLO_ALREADY_RUNNING!"=="1" (
        echo YOLO detector da dang chay tai http://127.0.0.1:%YOLO_PORT%.
        echo Khong khoi dong them de tranh xung dot port. Hay dung terminal YOLO dang mo.
        exit /b 0
    )
    echo Port %YOLO_PORT% dang bi chuong trinh khac su dung.
    call :show_port_owner "%YOLO_PORT%"
    echo Hay dong tien trinh dang chiem port roi chay lai.
    exit /b 1
)

if not exist ".venv\Scripts\python.exe" (
    echo [1/3] Tao Python virtual environment...
    python -m venv .venv
    if errorlevel 1 exit /b 1
)

echo [2/3] Cai/cap nhat dependencies...
".venv\Scripts\python.exe" -m pip install -r requirements.txt
if errorlevel 1 exit /b 1

echo [3/3] Chay YOLO detector tai http://127.0.0.1:%YOLO_PORT%
".venv\Scripts\python.exe" -m uvicorn app:app --host 0.0.0.0 --port %YOLO_PORT%

endlocal
exit /b 0

:check_port
set "%~2=0"
set "PORT_TO_CHECK=%~1"
powershell -NoProfile -Command "try { $client = New-Object Net.Sockets.TcpClient; $iar = $client.BeginConnect('127.0.0.1', [int]$env:PORT_TO_CHECK, $null, $null); if ($iar.AsyncWaitHandle.WaitOne(1000, $false)) { $client.EndConnect($iar); $client.Close(); exit 0 } else { $client.Close(); exit 1 } } catch { exit 1 }" >nul 2>nul
if not errorlevel 1 set "%~2=1"
exit /b 0

:check_yolo_health
set "%~2=0"
set "PORT_TO_CHECK=%~1"
powershell -NoProfile -Command "try { $h = Invoke-RestMethod -Uri ('http://127.0.0.1:' + $env:PORT_TO_CHECK + '/health') -TimeoutSec 2; if ($h.ok -eq $true) { exit 0 }; exit 1 } catch { exit 1 }" >nul 2>nul
if not errorlevel 1 set "%~2=1"
exit /b 0

:show_port_owner
set "PORT_TO_CHECK=%~1"
powershell -NoProfile -Command "$port = [int]$env:PORT_TO_CHECK; try { Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction Stop | Select-Object -First 5 LocalAddress,LocalPort,OwningProcess,@{n='ProcessName';e={(Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue).ProcessName}} | Format-Table -AutoSize } catch { netstat -ano | findstr (':' + $port) }"
exit /b 0
