@echo off
setlocal enabledelayedexpansion

set "PROJECT_DIR=%~dp0"
set "SDK_DIR=C:\Users\KingSpec Official\AppData\Local\Android\Sdk"
set "ADB=%SDK_DIR%\platform-tools\adb.exe"
set "EMULATOR=%SDK_DIR%\emulator\emulator.exe"
set "AVD_NAME=Medium_Phone"
set "PACKAGE_NAME=com.sotaynauan.ai"
set "APK=%PROJECT_DIR%app\build\outputs\apk\debug\app-debug.apk"

cd /d "%PROJECT_DIR%"

if not exist "%ADB%" (
    echo Khong tim thay adb.exe tai "%ADB%".
    exit /b 1
)

if not exist "%EMULATOR%" (
    echo Khong tim thay emulator.exe tai "%EMULATOR%".
    exit /b 1
)

echo [1/5] Build debug APK...
call "%PROJECT_DIR%gradlew.bat" assembleDebug
if errorlevel 1 exit /b 1

echo [2/5] Kiem tra thiet bi/emulator...
"%ADB%" start-server >nul
"%ADB%" get-state >nul 2>nul
if errorlevel 1 (
    echo Khong co thiet bi dang chay. Dang mo emulator %AVD_NAME%...
    start "" "%EMULATOR%" -avd "%AVD_NAME%"
    "%ADB%" wait-for-device
)

echo [3/5] Cho Android boot xong...
set "BOOTED="
for /l %%i in (1,1,120) do (
    for /f "usebackq delims=" %%b in (`"%ADB%" shell getprop sys.boot_completed 2^>nul`) do set "BOOTED=%%b"
    if "!BOOTED!"=="1" goto boot_complete
    timeout /t 2 /nobreak >nul
)

echo Emulator chua boot xong sau thoi gian cho.
exit /b 1

:boot_complete
echo [4/5] Cai APK...
"%ADB%" install -r "%APK%"
if errorlevel 1 exit /b 1

echo [5/5] Mo app...
"%ADB%" shell monkey -p "%PACKAGE_NAME%" -c android.intent.category.LAUNCHER 1
if errorlevel 1 exit /b 1

echo Xong. App da duoc build, cai va mo tren emulator/thiet bi.
endlocal
