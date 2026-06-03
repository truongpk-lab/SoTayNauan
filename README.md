# README.md — Bộ hướng dẫn Agent xây app hoàn chỉnh

## 1. Đặt file

Đặt các file này ở root project:

```text
project-root/
├── app/
├── READ_frontend/
├── AGENT.md
├── BUILD.md
├── RG.md
├── RIPGREP.md
├── PHASE_PROMPTS.md
├── PHASE_MAP.md
├── APP_COMPLETION_REQUIREMENTS.md
├── PROMPTS.md
└── README.md
```

## 2. Lệnh dùng

```text
Bạn hãy đọc AGENT.md, BUILD.md, RG.md, RIPGREP.md, PHASE_PROMPTS.md, PHASE_MAP.md, APP_COMPLETION_REQUIREMENTS.md, README.md và thực hiện PHASE 1. Đảm bảo đây là app hoàn chỉnh chạy được sau mỗi Phase, không phải frontend demo. Dữ liệu phải qua Repository/local storage, có model/adapter/viewmodel đúng cấu trúc, build được, bám sát READ_frontend/assets tương ứng.
```

Chỉ thay số Phase.

## 3. Ý nghĩa “app hoàn chỉnh”

Không có backend vẫn phải chạy được bằng local-first:

```text
Room/DataStore
Seed data
Repository thật
Timer thật
TTS thật nếu hỗ trợ
Shopping/Profile/Settings lưu local
```

## 4. Cách chạy app

### Chạy AI backend Gemini (tùy chọn)

App Android chạy được bằng dữ liệu local-first mà không cần backend. AI matching chính trong app dùng thuật toán local và Room/SharedPreferences.

Nếu muốn thử phần gợi ý/giọng nói có Gemini ở tầng backend, Gemini API không được gọi trực tiếp từ app Android. App gọi backend local:

```text
Android app → AI backend → Gemini API
```

Trên Windows, mở terminal tại `D:\SoTayNauAn`, chạy:

```bat
cd backend
run_backend.bat
```

Backend mặc định chạy ở:

```text
http://localhost:8787
```

Nếu chạy trên điện thoại Android thật, app truy cập backend máy tính qua USB `adb reverse`. `run_app.bat` tự chạy:

```text
adb reverse tcp:8787 tcp:8787
```

Khi đó debug app gọi:

```text
AI_BACKEND_BASE_URL=http://127.0.0.1:8787
```

Nếu chạy trên emulator, debug app sẽ gọi:

```text
AI_BACKEND_BASE_URL=http://10.0.2.2:8787
```

`10.0.2.2` chỉ dành cho emulator. Muốn emulator dùng webcam máy tính thay vì cảnh ảo, cấu hình AVD Camera = `Webcam0`.

Với bản release/production, không dùng HTTP cleartext. Cấu hình endpoint HTTPS riêng:

```text
AI_BACKEND_BASE_URL_RELEASE=https://your-ai-backend.example.com
```

Debug build có cấu hình network security riêng để cho phép HTTP local khi phát triển. Release build không bật cleartext.

API key Gemini nằm ở `backend/.env`, không nhúng vào APK Android.

### Chạy YOLO detector service

Backend Node chỉ proxy nhận diện nguyên liệu sang YOLO khi có `YOLO_DETECT_URL`. Không cấu hình biến này thì app sẽ nhận lỗi rõ ràng thay vì fallback âm thầm.

Tạo môi trường Python và chạy service:

```bat
cd backend\yolo_detector
run_yolo_detector.bat
```

Hoặc chạy thủ công:

```powershell
cd backend\yolo_detector
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8790
```

Nếu PowerShell chặn script activate, chạy một lần trong terminal hiện tại:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\.venv\Scripts\Activate.ps1
```

Hoặc không cần activate, chạy trực tiếp:

```powershell
.\.venv\Scripts\python.exe -m uvicorn app:app --host 0.0.0.0 --port 8790
```

Trong `backend\.env`:

```text
YOLO_DETECT_URL=http://127.0.0.1:8790/detect
YOLO_TIMEOUT_MS=20000
YOLO_MODEL_ID=original_yolov8s
```

Model mặc định đã được đặt theo registry tại:

```text
backend\yolo_detector\models\original_yolov8s\model.pt
backend\yolo_detector\models\models.json
```

`original_yolov8s` dùng file weight `original_yolov8s.pt` đã đưa vào cấu trúc chuẩn. Khi muốn nâng cấp model, tạo thư mục mới trong `backend\yolo_detector\models\`, đặt `model.pt`, thêm metadata/entry vào `models.json`, rồi đổi `YOLO_MODEL_ID`.

Khi deploy production, đặt YOLO service sau HTTPS và dùng:

```text
YOLO_DETECT_URL=https://your-yolo-detector.example.com/detect
```

Train custom model cho nguyên liệu:

```bat
cd backend\yolo_detector
python train.py --data ingredients_v1.yaml --model yolo26s.pt --epochs 100 --imgsz 960
```

Dataset theo format YOLO đặt tại `datasets\ingredients_v1`, gồm `images\train`, `images\val`, `images\test` và thư mục `labels` tương ứng. Sau khi train xong, thêm model mới vào registry:

```text
backend\yolo_detector\models\ingredients_v1\model.pt
backend\yolo_detector\models\ingredients_v1\metadata.json
backend\yolo_detector\models\models.json
YOLO_MODEL_ID=ingredients_v1
```

### Một lệnh build, cài và mở app

Trên Windows, mở terminal tại `D:\SoTayNauAn`.

Nếu dùng Command Prompt, chạy:

```bat
run_app.bat
```

Nếu dùng PowerShell, chạy:

```powershell
.\run_app.bat
```

PowerShell không tự chạy script trong thư mục hiện tại bằng tên trần `run_app.bat`, nên cần thêm `.\` phía trước.

Lệnh này sẽ:

```text
1. Kiểm tra backend đang chạy ở 127.0.0.1:8787 và YOLO detector sẵn sàng qua `/health`.
2. Ưu tiên thiết bị Android thật nếu có.
3. Nếu không có thiết bị thật, tự mở emulator như cấu hình cũ.
4. Tự chọn backend URL đúng: `127.0.0.1` cho thiết bị thật, `10.0.2.2` cho emulator.
5. Build, cài APK và mở app.
```

PATH Windows user đã được cấu hình thêm:

```text
C:\Users\KingSpec Official\AppData\Local\Android\Sdk\platform-tools
```

Sau khi mở terminal mới, có thể gọi trực tiếp:

```bat
adb devices
```

Nếu đang chạy từ WSL, dùng:

```bash
cmd.exe /c run_app.bat
```

Lưu ý: emulator mặc định có thể đang dùng Virtual Scene. Nếu muốn lấy hình từ webcam máy tính, mở Android Studio → Device Manager → Edit AVD → Show Advanced Settings → Camera → chọn `Webcam0`.

### Chạy bằng Android Studio

Nếu chọn emulator và muốn quét bằng webcam máy tính, hãy cấu hình AVD Camera = `Webcam0` trước khi chạy.

1. Mở Android Studio.
2. Chọn `Open` và trỏ tới thư mục `D:\SoTayNauAn`.
3. Chờ Gradle Sync hoàn tất.
4. Chọn emulator hoặc thiết bị Android thật.
5. Bấm `Run` để build, cài và mở app.

### Build APK debug bằng dòng lệnh

Trên Windows, mở Command Prompt hoặc PowerShell tại `D:\SoTayNauAn`, rồi chạy:

```bat
gradlew.bat assembleDebug
```

APK sau khi build nằm ở:

```text
D:\SoTayNauAn\app\build\outputs\apk\debug\app-debug.apk
```

### Cài APK vào emulator hoặc thiết bị

Emulator dùng được cho luồng camera nếu AVD Camera được đặt là `Webcam0`. Nếu để mặc định `Virtual Scene`, màn quét sẽ vẫn là cảnh ảo.

Đảm bảo emulator đang chạy hoặc thiết bị đã bật USB debugging, rồi chạy:

```bat
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Nếu emulator đang chạy là `emulator-5554`, có thể chỉ định rõ thiết bị:

```bat
adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
```

Mở app trên `emulator-5554` bằng package:

```bat
adb -s emulator-5554 shell monkey -p com.sotaynauan.ai -c android.intent.category.LAUNCHER 1
```

Hoặc mở bằng activity launcher:

```bat
adb -s emulator-5554 shell am start -n com.sotaynauan.ai/.MainActivity
```

Mở app trên thiết bị với tên:

```text
Sổ Tay Nấu Ăn AI
```

### Build, cài và mở trực tiếp trên emulator-5554

Nếu muốn kiểm tra camera/YOLO trên emulator, hãy cấu hình AVD Camera = `Webcam0` trước khi chạy.

Khi `emulator-5554` đã chạy sẵn, có thể dùng lần lượt:

```bat
gradlew.bat assembleDebug
adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
adb -s emulator-5554 shell monkey -p com.sotaynauan.ai -c android.intent.category.LAUNCHER 1
```

Trong PowerShell, nếu gọi Gradle wrapper từ thư mục hiện tại, dùng:

```powershell
.\gradlew.bat assembleDebug
adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
adb -s emulator-5554 shell monkey -p com.sotaynauan.ai -c android.intent.category.LAUNCHER 1
```

### Ghi chú khi chạy từ WSL

Nếu chạy trong WSL mà gặp lỗi `JAVA_HOME is not set`, dùng lệnh chạy app một bước:

```bash
cmd.exe /c run_app.bat
```
