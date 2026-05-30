@echo off
setlocal enabledelayedexpansion

set "PROJECT_DIR=%~dp0"
set "RUN_DIR=%LOCALAPPDATA%\SoTayNauAnRun"
set "SDK_DIR="
set "AVD_NAME=Medium_Phone"
set "PACKAGE_NAME=com.sotaynauan.ai"
set "APK=%RUN_DIR%\app\build\outputs\apk\debug\app-debug.apk"

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

"%EMULATOR%" -list-avds | findstr /x /c:"%AVD_NAME%" >nul
if errorlevel 1 (
    for /f "usebackq delims=" %%a in (`"%EMULATOR%" -list-avds`) do (
        if not defined FOUND_AVD set "FOUND_AVD=%%a"
    )
    if defined FOUND_AVD (
        set "AVD_NAME=!FOUND_AVD!"
        echo Khong thay AVD Medium_Phone, dung AVD !AVD_NAME! tren may nay.
    )
)

echo [1/6] Dong bo project sang thu muc local de tranh loi OneDrive...
if not exist "%RUN_DIR%" mkdir "%RUN_DIR%"
robocopy "%PROJECT_DIR%." "%RUN_DIR%" /MIR /XD "%PROJECT_DIR%.git" "%PROJECT_DIR%.gradle" "%PROJECT_DIR%build" "%PROJECT_DIR%app\build" "%PROJECT_DIR%backend\node_modules" /NFL /NDL /NJH /NJS /NC /NS
if !ERRORLEVEL! GEQ 8 exit /b !ERRORLEVEL!

echo [2/6] Build debug APK...
call "%RUN_DIR%\gradlew.bat" -p "%RUN_DIR%" assembleDebug
if errorlevel 1 exit /b 1

echo [3/6] Kiem tra thiet bi/emulator...
"%ADB%" start-server >nul
"%ADB%" devices | findstr /R "device$" >nul
if errorlevel 1 (
    echo Khong co thiet bi dang chay. Dang mo emulator %AVD_NAME%...
    start "" "%EMULATOR%" -avd "%AVD_NAME%" -no-boot-anim
    "%ADB%" wait-for-device
)

"%ADB%" wait-for-device

echo [4/6] Cho Android boot xong...
set "BOOTED="
for /l %%i in (1,1,120) do (
    for /f "usebackq delims=" %%b in (`"%ADB%" shell getprop sys.boot_completed 2^>nul`) do set "BOOTED=%%b"
    if "!BOOTED!"=="1" goto boot_complete
    timeout /t 2 /nobreak >nul
)

echo Emulator chua boot xong sau thoi gian cho.
exit /b 1

:boot_complete
echo [5/6] Cai APK...
"%ADB%" install -r "%APK%"
if errorlevel 1 exit /b 1

echo [6/6] Mo app...
"%ADB%" shell am start -n "%PACKAGE_NAME%/%PACKAGE_NAME%.MainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
if errorlevel 1 exit /b 1

echo Xong. App da duoc build, cai va mo tren emulator/thiet bi.
endlocal
