# HDCD.MD - Huong dan cai dat moi sau khi clone Git

Tai lieu nay danh cho may moi vua clone repo `SoTayNauAn`. Lam theo cac buoc ben duoi de co moi truong giong may phat trien: Android app, AI backend Node va YOLO detector local.

Luu y quan trong: backend Node, YOLO detector, requirements va model baseline da nam trong repo nay. May khac pull ve khong can clone/copy them backend hoac YOLO rieng. Thu can co san tren may la Android Studio/SDK, Node.js, Python va ket noi internet cho lan dau YOLO cai Python packages.

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

    git clone <GIT_REPO_URL> SoTayNauAn
    cd SoTayNauAn

Neu da co repo:

    cd D:\SoTayNauAn
    git pull

Sau khi pull, can co cac file/thu muc chinh:

```text
app\
backend\
backend\server.js
backend\run_backend.bat
backend\.env.example
backend\yolo_detector\app.py
backend\yolo_detector\run_yolo_detector.bat
backend\yolo_detector\requirements.txt
backend\yolo_detector\models\original_yolov8s\model.pt
gradlew.bat
run_app.bat
run_backend.bat
run_yolo_detector.bat
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
- IP LAN cua PC, vi du `http://192.168.1.25:8787`, khi chay dien thoai that. Cach nay giup cac dien thoai/tai khoan cung Wi-Fi dung chung cong dong bep nha.

Neu may co nhieu card mang va script chon sai IP, chi dinh thu cong truoc khi chay:

```bat
set LAN_BACKEND_URL=http://192.168.1.25:8787
.\run_app.bat
```

## 5. Tao cau hinh backend

Backend Node doc cau hinh tu `backend\.env`. File nay khong commit de tranh lo API key. Tren may moi, co 2 cach:

- Cach de nhat: chay `.\run_backend.bat`; script tu copy `backend\.env.example` thanh `backend\.env` neu chua co.
- Cach thu cong: tao file tu template:

    cd D:\SoTayNauAn\backend
    copy .env.example .env

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

Cong dong bep nha LAN cung chay tren backend Node port `8787`. Backend se tu tao `backend\data\community.json` de luu loi moi ket ban QR, ban be, chia se cong thuc, like, luu mon va binh luan. File nay la du lieu runtime local cua may chay backend, khong commit len git.

Backend hien dung Node built-in modules trong `backend\server.js`, nen khong can `npm install` de chay ban hien tai. Neu sau nay them dependencies vao `backend\package.json`, chay `npm install` trong thu muc `backend` truoc khi mo backend.

## 6. Cai va chay bang 3 terminal

Mo 3 terminal rieng biet tai Windows. Hay de Terminal 1 va Terminal 2 tiep tuc chay, sau do moi chay Terminal 3.
Neu PowerShell hien dau nhac `>>` sau khi lo copy ba dau backtick tu Markdown, bam `Ctrl+C` de thoat ve `PS D:\SoTayNauAn>`.

Terminal 1 - chay YOLO detector:

    cd D:\SoTayNauAn
    .\run_yolo_detector.bat

Lan dau script se vao `backend\yolo_detector`, tao `.venv`, cai Python packages tu `requirements.txt`, nap model `models\original_yolov8s\model.pt` co san trong repo va chay service tai:

```text
http://127.0.0.1:8790
```

Terminal 2 - chay AI backend Node:

    cd D:\SoTayNauAn
    .\run_backend.bat

Lan dau script se vao `backend`, tao `backend\.env` tu `.env.example` neu chua co, bo sung cau hinh YOLO mac dinh neu thieu, va chay backend tai:

```text
http://127.0.0.1:8787
```

Terminal 3 - build, cai va mo app:

    cd D:\SoTayNauAn
    .\run_app.bat

`run_app.bat` se kiem tra backend `/health` va yeu cau `yoloReady=true`, chon thiet bi/emulator, build debug APK, cai APK va mo app `com.sotaynauan.ai`. Script nay khong tu bat Terminal 1/2; neu backend hoac YOLO chua chay, hay mo Terminal 1 va Terminal 2 nhu tren truoc.

Thu tu bat buoc tren may moi:

```text
1. .\run_yolo_detector.bat  -> giu terminal nay mo
2. .\run_backend.bat        -> giu terminal nay mo
3. .\run_app.bat            -> build/cai/mo Android app
```

## 7. Du lieu local va DB nguyen lieu

App dung Room local-first. Khi tao/kiem tra ke hoach nau, app tu seed bang `ingredients` voi cac nhom pho bien nhu `meat`, `seafood`, `protein`, `grain`, `noodle`, `vegetable`, `fruit`, `spice`, `sauce`, `oil`, `liquid`, `dairy`, `frozen`.

Khong can chay script DB rieng tren may moi. Neu may da cai ban cu, app se bo sung seed moi va cap nhat lai cac ingredient seed co cung `id` de phan nhom nguyen lieu/gia vi dung hon.

## 8. Build APK rieng

Neu chi muon build APK debug:

    cd D:\SoTayNauAn
    .\gradlew.bat assembleDebug

APK nam tai:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Neu dang o WSL va chua co Java/Linux Android SDK, goi wrapper Windows:

```bash
cmd.exe /c gradlew.bat assembleDebug
```

## 9. Kiem tra nhanh sau khi cai moi

```text
[ ] Android Studio sync thanh cong.
[ ] local.properties co sdk.dir dung may.
[ ] backend\.env da tao tu .env.example.
[ ] backend\yolo_detector\models\original_yolov8s\model.pt ton tai sau khi pull.
[ ] Terminal 1 YOLO detector dang chay port 8790.
[ ] Terminal 2 AI backend dang chay port 8787 va /health bao yoloReady=true.
[ ] Terminal 3 .\run_app.bat build, cai va mo app thanh cong.
[ ] Neu dung dien thoai that cho cong dong LAN, dien thoai va PC o cung Wi-Fi, Windows Firewall cho phep port 8787.
[ ] Vao mot cong thuc, bam chuan bi/bat dau nau de Room seed nhom nguyen lieu va gia vi.
```

## 10. Loi thuong gap

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
