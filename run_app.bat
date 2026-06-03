@echo off
setlocal enabledelayedexpansion

set "PROJECT_DIR=%~dp0"
set "RUN_DIR=%LOCALAPPDATA%\SoTayNauAnRun"
set "SDK_DIR="
set "AVD_NAME=Medium_Phone"
set "PACKAGE_NAME=com.sotaynauan.ai"
set "APK=%RUN_DIR%\app\build\outputs\apk\debug\app-debug.apk"
set "TARGET_DEVICE="
set "TARGET_KIND="
set "DEBUG_BACKEND_URL="

cd /d "%PROJECT_DIR%"

if exist "%PROJECT_DIR%local.properties" (
    for /f "usebackq tokens=1,* delims==" %%a in ("%PROJECT_DIR%local.properties") do (
        if "%%a"=="sdk.dir" set "SDK_DIR=%%b"
    )
)

if not defined SDK_DIR if defined ANDROID_SDK_ROOT set "SDK_DIR=%ANDROID_SDK_ROOT%"
if not defined SDK_DIR if defined ANDROID_HOME set "SDK_DIR=%ANDROID_HOME%"
if not defined SDK_DIR set "SDK_DIR=%LOCALAPPDATA%\Android\Sdk"

set "SDK_DIR=%SDK_DIR:\\=\%"
set "SDK_DIR=%SDK_DIR:\:=:%"
set "ADB=%SDK_DIR%\platform-tools\adb.exe"
set "EMULATOR=%SDK_DIR%\emulator\emulator.exe"

if not exist "%ADB%" (
    echo Khong tim thay adb.exe tai "%ADB%".
    echo Hay kiem tra sdk.dir trong local.properties.
    exit /b 1
)

if not exist "%EMULATOR%" (
    echo Khong tim thay emulator.exe tai "%EMULATOR%".
    echo Hay kiem tra sdk.dir trong local.properties.
    exit /b 1
)

echo [1/8] Kiem tra AI backend va YOLO tren may tinh...
call :check_port 8787 BACKEND_READY
if not "!BACKEND_READY!"=="1" (
    echo AI backend chua chay tren 127.0.0.1:8787.
    echo Hay mo terminal khac tai D:\SoTayNauAn\backend roi chay: node server.js
    exit /b 1
)
call :check_backend_health BACKEND_HEALTH_READY
if not "!BACKEND_HEALTH_READY!"=="1" (
    echo AI backend dang chay nhung YOLO detector chua san sang.
    if defined BACKEND_HEALTH_ERROR echo Ly do: !BACKEND_HEALTH_ERROR!
    echo Hay chay backend\yolo_detector tren port 8790 va dat YOLO_DETECT_URL=http://127.0.0.1:8790/detect.
    echo Neu vua sua backend\.env, hay tat terminal node server.js va chay lai node server.js.
    exit /b 1
)

echo [2/8] Chon thiet bi chay app...
"%ADB%" start-server >nul
call :find_device

if not defined TARGET_DEVICE (
    call :ensure_avd
    if errorlevel 1 exit /b 1
    echo Khong co dien thoai that. Dang mo emulator %AVD_NAME% nhu cau hinh cu...
    start "" "%EMULATOR%" -avd "%AVD_NAME%" -no-boot-anim
    "%ADB%" wait-for-device
    call :find_device
)

if not defined TARGET_DEVICE (
    echo Khong tim thay thiet bi/emulator nao de cai app.
    "%ADB%" devices -l
    exit /b 1
)

if "%TARGET_KIND%"=="emulator" (
    set "DEBUG_BACKEND_URL=http://10.0.2.2:8787"
    echo Dang dung emulator: %TARGET_DEVICE%
    echo Neu muon camera that tren emulator, hay cau hinh AVD Camera = Webcam0.
) else (
    set "DEBUG_BACKEND_URL=http://127.0.0.1:8787"
    echo Dang dung thiet bi Android that: %TARGET_DEVICE%
)

echo [3/8] Cho Android framework va package manager san sang...
call :wait_android_ready
if errorlevel 1 exit /b 1

if "%TARGET_KIND%"=="real" (
    echo [4/8] Mo adb reverse de dien thoai goi backend qua USB...
    "%ADB%" -s "%TARGET_DEVICE%" reverse tcp:8787 tcp:8787
    if errorlevel 1 (
        echo Khong tao duoc adb reverse tcp:8787. Hay kiem tra USB debugging/RSA prompt.
        exit /b 1
    )
) else (
    echo [4/8] Emulator se goi backend may host qua 10.0.2.2.
)

echo [5/8] Dong bo project sang thu muc local de tranh loi OneDrive...
if not exist "%RUN_DIR%" mkdir "%RUN_DIR%"
robocopy "%PROJECT_DIR%." "%RUN_DIR%" /MIR /XD "%PROJECT_DIR%.git" "%PROJECT_DIR%.gradle" "%PROJECT_DIR%build" "%PROJECT_DIR%app\build" "%PROJECT_DIR%backend\node_modules" /NFL /NDL /NJH /NJS /NC /NS
if !ERRORLEVEL! GEQ 8 exit /b !ERRORLEVEL!

echo AI_BACKEND_BASE_URL=%DEBUG_BACKEND_URL%>>"%RUN_DIR%\local.properties"

echo [6/8] Build debug APK voi backend %DEBUG_BACKEND_URL%...
call "%RUN_DIR%\gradlew.bat" -p "%RUN_DIR%" assembleDebug
if errorlevel 1 exit /b 1

echo [7/8] Cai APK...
"%ADB%" -s "%TARGET_DEVICE%" install -r "%APK%"
if errorlevel 1 exit /b 1

echo [8/8] Mo app...
"%ADB%" -s "%TARGET_DEVICE%" shell am start -n "%PACKAGE_NAME%/%PACKAGE_NAME%.MainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
if errorlevel 1 exit /b 1

echo Xong. App da duoc build, cai va mo tren %TARGET_KIND% %TARGET_DEVICE%.
endlocal
exit /b 0

:find_device
set "TARGET_DEVICE="
set "TARGET_KIND="
for /f "skip=1 tokens=1,2" %%a in ('"%ADB%" devices') do (
    if "%%b"=="device" (
        echo %%a | findstr /b /c:"emulator-" >nul
        if errorlevel 1 (
            if not defined TARGET_DEVICE (
                set "TARGET_DEVICE=%%a"
                set "TARGET_KIND=real"
            )
        )
    )
)
if defined TARGET_DEVICE exit /b 0
for /f "skip=1 tokens=1,2" %%a in ('"%ADB%" devices') do (
    if "%%b"=="device" (
        echo %%a | findstr /b /c:"emulator-" >nul
        if not errorlevel 1 (
            if not defined TARGET_DEVICE (
                set "TARGET_DEVICE=%%a"
                set "TARGET_KIND=emulator"
            )
        )
    )
)
exit /b 0

:ensure_avd
"%EMULATOR%" -list-avds | findstr /x /c:"%AVD_NAME%" >nul
if errorlevel 1 (
    set "FOUND_AVD="
    for /f "usebackq delims=" %%a in (`"%EMULATOR%" -list-avds`) do (
        if not defined FOUND_AVD set "FOUND_AVD=%%a"
    )
    if defined FOUND_AVD (
        set "AVD_NAME=!FOUND_AVD!"
        echo Khong thay AVD Medium_Phone, dung AVD !AVD_NAME! tren may nay.
    ) else (
        echo Khong tim thay AVD nao. Hay tao emulator trong Android Studio Device Manager.
        exit /b 1
    )
)
exit /b 0

:wait_android_ready
powershell -NoProfile -ExecutionPolicy Bypass -Command "$adb = $env:ADB; $serial = $env:TARGET_DEVICE; $deadline = (Get-Date).AddMinutes(6); $last = ''; while ((Get-Date) -lt $deadline) { try { & $adb -s $serial wait-for-device | Out-Null; $sys = (& $adb -s $serial shell getprop sys.boot_completed 2>$null | Out-String).Trim(); $dev = (& $adb -s $serial shell getprop dev.bootcomplete 2>$null | Out-String).Trim(); $anim = (& $adb -s $serial shell getprop init.svc.bootanim 2>$null | Out-String).Trim(); $pm = (& $adb -s $serial shell cmd package list packages android 2>$null | Out-String).Trim(); $last = ('sys=' + $sys + ' dev=' + $dev + ' anim=' + $anim + ' pm=' + (($pm.Length -gt 0))); if ($sys -eq '1' -and ($dev -eq '1' -or $anim -eq 'stopped') -and $pm.Length -gt 0) { Write-Host ('Android ready: ' + $last); exit 0 }; Write-Host ('Waiting Android boot: ' + $last) } catch { $last = $_.Exception.Message; Write-Host ('Waiting Android boot: ' + $last) }; Start-Sleep -Seconds 3 }; Write-Host ('Android boot timeout. Last status: ' + $last); exit 1"
if errorlevel 1 (
    echo Thiet bi/emulator chua san sang sau thoi gian cho.
    echo Goi y: neu emulator bi treo, dong emulator va chay lai .\run_app.bat.
    exit /b 1
)
exit /b 0

:check_port
set "%~2=0"
powershell -NoProfile -Command "try { $client = New-Object Net.Sockets.TcpClient; $iar = $client.BeginConnect('127.0.0.1', %~1, $null, $null); if ($iar.AsyncWaitHandle.WaitOne(1000, $false)) { $client.EndConnect($iar); $client.Close(); exit 0 } else { $client.Close(); exit 1 } } catch { exit 1 }" >nul 2>nul
if not errorlevel 1 set "%~2=1"
exit /b 0

:check_backend_health
set "%~1=0"
powershell -NoProfile -Command "try { $h = Invoke-RestMethod -Uri 'http://127.0.0.1:8787/health' -TimeoutSec 4; if ($h.yoloReady -eq $true) { 'READY'; exit 0 }; 'NOT_READY|' + $h.yoloError; exit 1 } catch { 'NOT_READY|' + $_.Exception.Message; exit 1 }" > "%TEMP%\sotaynauan_backend_health.txt"
set "BACKEND_HEALTH_ERROR="
for /f "usebackq tokens=1,* delims=|" %%a in ("%TEMP%\sotaynauan_backend_health.txt") do (
    if "%%a"=="NOT_READY" set "BACKEND_HEALTH_ERROR=%%b"
)
if not errorlevel 1 set "%~1=1"
exit /b 0
