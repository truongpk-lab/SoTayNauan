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
node server.js
```

Backend mặc định chạy ở:

```text
http://localhost:8787
```

Emulator Android truy cập máy host qua:

```text
http://10.0.2.2:8787
```

Giá trị này nằm trong `local.properties`:

```text
AI_BACKEND_BASE_URL=http://10.0.2.2:8787
```

API key Gemini nằm ở `backend/.env`, không nhúng vào APK Android.

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
1. Build APK debug.
2. Mở emulator Medium_Phone nếu chưa có thiết bị/emulator đang chạy.
3. Chờ Android boot xong.
4. Cài APK vào emulator hoặc thiết bị.
5. Mở app Sổ Tay Nấu Ăn AI.
```

PATH Windows user đã được cấu hình thêm:

```text
C:\Users\KingSpec Official\AppData\Local\Android\Sdk\platform-tools
C:\Users\KingSpec Official\AppData\Local\Android\Sdk\emulator
```

Sau khi mở terminal mới, có thể gọi trực tiếp:

```bat
adb devices
emulator -list-avds
```

Nếu đang chạy từ WSL, dùng:

```bash
cmd.exe /c run_app.bat
```

Lưu ý: các terminal đã mở trước khi cấu hình PATH có thể chưa nhận `adb` và `emulator`. Hãy đóng terminal cũ và mở Command Prompt/PowerShell mới.

### Chạy bằng Android Studio

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
cd backend
node server.js
