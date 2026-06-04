# So Tay Nau An AI

Ung dung Android goi y mon an, quan ly cong thuc, danh sach mua sam, che do nau an, tro ly giong noi va nhan dien nguyen lieu bang camera. App co du lieu local-first, backend Node de proxy Gemini va YOLO detector FastAPI chay local cho tinh nang nhan dien anh.

## Cai moi tren may khac

Xem day du trong [HDCD.MD](HDCD.MD). Repo da co san Android app, backend Node, YOLO detector va model baseline. May khac pull ve khong can lay them repo backend/YOLO rieng, chi can cai dung phan mem nen:

- Android Studio + Android SDK Platform/Build-Tools/Platform-Tools/Emulator.
- Node.js LTS tu 18 tro len.
- Python 3.10 hoac 3.11.
- AVD Android hoac dien thoai that da bat USB debugging.

Sau khi clone/pull, Android Studio se tao `local.properties` theo may. Neu can tao tay:

```properties
sdk.dir=C\:\\Users\\<TEN_USER>\\AppData\\Local\\Android\\Sdk
```

Backend dung file rieng `backend\.env` va khong commit API key. Khi chay `.\run_backend.bat`, script se tu tao `backend\.env` tu `backend\.env.example` neu file nay chua ton tai. Neu muon tao/sua thu cong:

    cd D:\SoTayNauAn\backend
    copy .env.example .env

Neu can day du chuc nang Gemini/voice, dien `GEMINI_API_KEY` trong `backend\.env`. Neu chua dien key, backend van chay, cong dong LAN va YOLO van dung duoc; cac tinh nang goi Gemini se bao thieu key. Cac bien YOLO mac dinh:

```env
YOLO_DETECT_URL=http://127.0.0.1:8790/detect
YOLO_TIMEOUT_MS=20000
YOLO_MODEL_ID=original_yolov8s
```

Anh mon an cho cong thuc AI co pipeline nhieu tang. Neu khong dien key, backend van co gang lay anh tu structured data cua trang cong thuc va Wikimedia/Wikipedia. De tang ti le dung anh mon that, co the dien them:

```env
IMAGE_SEARCH_TIMEOUT_MS=7000
RECIPE_IMAGE_MIN_SCORE=58
RECIPE_IMAGE_SOURCE_SITES=cooky.vn,dienmayxanh.com,bachhoaxanh.com,ngonaz.com,thatlangon.com,afamily.vn
GOOGLE_CSE_API_KEY=
GOOGLE_CSE_ID=
PIXABAY_API_KEY=
PEXELS_API_KEY=
UNSPLASH_ACCESS_KEY=
```

Thu tu uu tien anh: anh trong JSON-LD `schema.org/Recipe` cua trang cong thuc, Open Graph cua trang cong thuc, Google Custom Search image neu co key, Pixabay/Pexels/Unsplash neu co key, roi Wikimedia/Wikipedia fallback. App Android se tai anh duoc chon ve local truoc khi luu cong thuc.

## Khoi chay moi bang 3 terminal

Sau khi clone/pull tren may khac, mo 3 terminal Windows rieng biet. De Terminal 1 va Terminal 2 tiep tuc chay, roi chay Terminal 3.
Neu PowerShell hien dau nhac `>>` sau khi lo copy ba dau backtick tu Markdown, bam `Ctrl+C` de thoat ve `PS D:\SoTayNauAn>`.

Terminal 1 - YOLO detector:

    cd D:\SoTayNauAn
    .\run_yolo_detector.bat

Lan dau chay, script tu tao `backend\yolo_detector\.venv`, cai Python packages tu `backend\yolo_detector\requirements.txt`, nap model co san `backend\yolo_detector\models\original_yolov8s\model.pt`, roi mo service port `8790`.

Terminal 2 - AI backend Node:

    cd D:\SoTayNauAn
    .\run_backend.bat

Lan dau chay, script tu tao `backend\.env` tu `.env.example` neu chua co, cau hinh mac dinh de backend goi YOLO tai `http://127.0.0.1:8790/detect`, roi mo service port `8787`.

Terminal 3 - build, cai va mo app Android:

    cd D:\SoTayNauAn
    .\run_app.bat

`run_app.bat` khong tu bat backend/YOLO; no se kiem tra backend va YOLO qua `/health`, uu tien dien thoai that neu co, tu mo emulator neu can, build debug APK, cai APK va mo app. Khi cai tren dien thoai that, script se build app voi IP LAN cua may PC chay backend de cac dien thoai/tai khoan cung Wi-Fi co the dung chung cong dong bep nha.

## Endpoint local

```text
YOLO detector: http://127.0.0.1:8790
AI backend:    http://127.0.0.1:8787
```

Khi chay emulator, app debug goi backend host qua `http://10.0.2.2:8787`. Khi chay dien thoai that, `run_app.bat` tu lay IP LAN cua PC, vi du `http://192.168.1.25:8787`, de cac may trong cung Wi-Fi ket noi chung backend. Neu muon chi dinh IP thu cong:

```bat
set LAN_BACKEND_URL=http://192.168.1.25:8787
.\run_app.bat
```

Cong dong bep nha LAN dung chung backend Node de dong bo loi moi ket ban QR, danh sach ban be, chia se cong thuc, like, luu mon va binh luan. Du lieu cong dong duoc luu o `backend\data\community.json`.

## Build APK debug rieng

    cd D:\SoTayNauAn
    .\gradlew.bat assembleDebug

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
