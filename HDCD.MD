# HDCD.MD - Huong dan cai dat moi sau khi clone Git

Tai lieu nay danh cho may moi vua clone repo `SoTayNauAn`. Lam theo cac buoc ben duoi de co moi truong giong may phat trien: Android app, AI backend Node va YOLO detector local.

## 1. Phan mem can cai

- Git for Windows.
- Visual Studio Code.
- Android Studio.
- Android SDK Platform, Build-Tools, Platform-Tools va Android Emulator trong Android Studio SDK Manager.
- Mot AVD Android, khuyen nghi ten `Medium_Phone`.
- Node.js LTS tu 18 tro len.
- Python 3.10 hoac 3.11.

Android Studio thuong co san JDK. Neu Gradle bao `JAVA_HOME is not set`, hay build bang terminal Windows/Android Studio hoac cau hinh `JAVA_HOME` den JDK cua Android Studio.

## 2. Clone hoac cap nhat repo

Mo PowerShell/CMD tai thu muc muon dat project:

```powershell
git clone <GIT_REPO_URL> SoTayNauAn
cd SoTayNauAn
```

Neu da co repo:

```powershell
cd D:\SoTayNauAn
git pull
```

Sau khi pull, can co cac file/thu muc chinh:

```text
app\
backend\
backend\yolo_detector\models\original_yolov8s\model.pt
gradlew.bat
run_app.bat
README.md
HDCD.MD
```

## 3. Mo project trong Android Studio

1. Mo Android Studio.
2. Chon `Open`.
3. Chon thu muc `D:\SoTayNauAn` hoac thu muc vua clone.
4. Doi Gradle Sync hoan tat.
5. Vao `Tools > SDK Manager`, cai SDK/Build-Tools/Platform-Tools/Emulator neu Android Studio yeu cau.
6. Vao `Tools > Device Manager`, tao AVD neu chua co. Ten mac dinh script uu tien la `Medium_Phone`, nhung neu khong co script se dung AVD dau tien tim thay.

Neu muon camera emulator lay webcam may tinh, vao AVD `Edit > Show Advanced Settings > Camera` va chon `Webcam0`.

## 4. Kiem tra `local.properties`

File `local.properties` khong duoc commit vi phu thuoc may. Neu Android Studio chua tao, tao file tai root project:

```properties
sdk.dir=C\:\\Users\\<TEN_USER>\\AppData\\Local\\Android\\Sdk
```

Khong can tu dat `AI_BACKEND_BASE_URL` khi chay bang `run_app.bat`. Script se tu tao build debug dung:

- `http://10.0.2.2:8787` khi chay emulator.
- `http://127.0.0.1:8787` khi chay dien thoai that qua `adb reverse`.

## 5. Tao cau hinh backend

Backend Node doc cau hinh tu `backend\.env`. File nay khong commit de tranh lo API key. Tren may moi, tao tu template:

```powershell
cd D:\SoTayNauAn\backend
copy .env.example .env
```

Mo `backend\.env` va dien `GEMINI_API_KEY` neu can day du chuc nang Gemini/voice. Cac bien YOLO nen giu mac dinh:

```env
GEMINI_API_KEY=<API_KEY_CUA_BAN>
PORT=8787
GEMINI_MODEL=gemini-2.5-flash
YOLO_DETECT_URL=http://127.0.0.1:8790/detect
YOLO_TIMEOUT_MS=20000
YOLO_MODEL_ID=original_yolov8s
```

Neu chua co Gemini key, backend van chay va detector YOLO van hoat dong, nhung cac API goi Gemini se bao thieu key.

## 6. Cai va chay bang 3 terminal

Mo 3 terminal rieng biet tai Windows. Hay de Terminal 1 va Terminal 2 tiep tuc chay, sau do moi chay Terminal 3.

Terminal 1 - chay YOLO detector:

```powershell
cd D:\SoTayNauAn\backend\yolo_detector
.\run_yolo_detector.bat
```

Lan dau script se tao `.venv`, cai Python packages tu `requirements.txt`, nap model `models\original_yolov8s\model.pt` va chay service tai:

```text
http://127.0.0.1:8790
```

Terminal 2 - chay AI backend Node:

```powershell
cd D:\SoTayNauAn\backend
.\run_backend.bat
```

Backend chay tai:

```text
http://127.0.0.1:8787
```

Terminal 3 - build, cai va mo app:

```powershell
cd D:\SoTayNauAn
.\run_app.bat
```

`run_app.bat` se kiem tra backend `/health`, chon thiet bi/emulator, build debug APK, cai APK va mo app `com.sotaynauan.ai`.

## 7. Build APK rieng

Neu chi muon build APK debug:

```powershell
cd D:\SoTayNauAn
.\gradlew.bat assembleDebug
```

APK nam tai:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Neu dang o WSL va chua co Java/Linux Android SDK, goi wrapper Windows:

```bash
cmd.exe /c gradlew.bat assembleDebug
```

## 8. Kiem tra nhanh sau khi cai moi

```text
[ ] Android Studio sync thanh cong.
[ ] local.properties co sdk.dir dung may.
[ ] backend\.env da tao tu .env.example.
[ ] backend\yolo_detector\models\original_yolov8s\model.pt ton tai sau khi pull.
[ ] Terminal 1 YOLO detector dang chay port 8790.
[ ] Terminal 2 AI backend dang chay port 8787 va /health bao yoloReady=true.
[ ] Terminal 3 .\run_app.bat build, cai va mo app thanh cong.
```

## 9. Loi thuong gap

### `adb is not recognized`

Them Platform-Tools vao PATH:

```text
C:\Users\<TEN_USER>\AppData\Local\Android\Sdk\platform-tools
```

Hoac chay bang `run_app.bat`, script tu tim `adb.exe` theo `local.properties`.

### Khong thay AVD `Medium_Phone`

Tao AVD trong Android Studio Device Manager, hoac de script tu dung AVD dau tien tim thay.

### YOLO detector loi khi cai package

Kiem tra Python version 3.10/3.11 va internet de `pip install -r requirements.txt`. Neu PowerShell chan activate thi khong sao, script chay truc tiep `.venv\Scripts\python.exe`.

### Backend bao `yoloReady=false`

Kiem tra Terminal 1 dang chay, `backend\.env` co:

```env
YOLO_DETECT_URL=http://127.0.0.1:8790/detect
YOLO_MODEL_ID=original_yolov8s
```

Sau khi sua `.env`, tat Terminal 2 va chay lai `.\run_backend.bat`.

### Gemini API loi

Kiem tra `GEMINI_API_KEY` trong `backend\.env`. Khong commit `.env`; chi commit `.env.example`.
