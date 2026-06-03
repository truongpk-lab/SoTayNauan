# So Tay Nau An AI

Ung dung Android goi y mon an, quan ly cong thuc, danh sach mua sam, che do nau an, tro ly giong noi va nhan dien nguyen lieu bang camera. App co du lieu local-first, backend Node de proxy Gemini va YOLO detector FastAPI chay local cho tinh nang nhan dien anh.

## Cai moi tren may khac

Xem day du trong [HDCD.MD](HDCD.MD). Tom tat moi truong can co:

- Android Studio + Android SDK Platform/Build-Tools/Platform-Tools/Emulator.
- Node.js LTS tu 18 tro len.
- Python 3.10 hoac 3.11.
- AVD Android hoac dien thoai that da bat USB debugging.

Sau khi clone/pull, Android Studio se tao `local.properties` theo may. Neu can tao tay:

```properties
sdk.dir=C\:\\Users\\<TEN_USER>\\AppData\\Local\\Android\\Sdk
```

Backend dung file rieng `backend\.env` va khong commit API key. Tao tu template:

```powershell
cd D:\SoTayNauAn\backend
copy .env.example .env
```

Neu can day du chuc nang Gemini/voice, dien `GEMINI_API_KEY` trong `backend\.env`. Cac bien YOLO mac dinh:

```env
YOLO_DETECT_URL=http://127.0.0.1:8790/detect
YOLO_TIMEOUT_MS=20000
YOLO_MODEL_ID=original_yolov8s
```

## Khoi chay moi bang 3 terminal

Mo 3 terminal Windows rieng biet. De Terminal 1 va Terminal 2 tiep tuc chay, roi chay Terminal 3.

Terminal 1 - YOLO detector:

```powershell
cd D:\SoTayNauAn\backend\yolo_detector
.\run_yolo_detector.bat
```

Terminal 2 - AI backend Node:

```powershell
cd D:\SoTayNauAn\backend
.\run_backend.bat
```

Terminal 3 - build, cai va mo app Android:

```powershell
cd D:\SoTayNauAn
.\run_app.bat
```

`run_app.bat` se kiem tra backend va YOLO qua `/health`, uu tien dien thoai that neu co, tu mo emulator neu can, build debug APK, cai APK va mo app.

## Endpoint local

```text
YOLO detector: http://127.0.0.1:8790
AI backend:    http://127.0.0.1:8787
```

Khi chay emulator, app debug goi backend host qua `http://10.0.2.2:8787`. Khi chay dien thoai that, `run_app.bat` tu tao:

```text
adb reverse tcp:8787 tcp:8787
```

## Build APK debug rieng

```powershell
cd D:\SoTayNauAn
.\gradlew.bat assembleDebug
```

APK sau build:

```text
D:\SoTayNauAn\app\build\outputs\apk\debug\app-debug.apk
```

## Model YOLO

Model baseline da duoc dat trong repo:

```text
backend\yolo_detector\models\original_yolov8s\model.pt
backend\yolo_detector\models\models.json
```

Khi them model moi, tao thu muc `backend\yolo_detector\models\<model_id>\`, dat weight thanh `model.pt`, them `metadata.json`, cap nhat `models.json`, roi dat `YOLO_MODEL_ID=<model_id>` trong `backend\.env`.
