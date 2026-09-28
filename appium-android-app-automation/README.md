# appium-android-app-automation

Framework automation **Appium + Java + TestNG** cho app Android **Book** (`com.anhtester.book`) — app hybrid, giao diện nằm trong WebView.

---

## 1. Cài đặt lần đầu (mỗi máy làm 1 lần)

Làm lần lượt từ trên xuống. Lệnh kiểm tra ở cột cuối chạy ra đúng thì mới sang bước sau.

| # | Việc | Cách làm | Kiểm tra |
|---|---|---|---|
| 1 | Cài **JDK 11+** | Microsoft OpenJDK / Temurin | `java -version` |
| 2 | Cài **Node.js 20+** | nodejs.org | `node -v` |
| 3 | Cài **Android Studio** (có sẵn Android SDK + emulator) | developer.android.com/studio | Thư mục `%LOCALAPPDATA%\Android\Sdk` tồn tại |
| 4 | Cài **Appium 3** | `npm install -g appium` | `appium -v` |
| 5 | Cài **driver UiAutomator2** — Appium cài xong **chưa** có driver Android, thiếu bước này sẽ báo `Could not find a driver for automationName 'UiAutomator2'` | `appium driver install uiautomator2` | `appium driver list --installed` thấy `uiautomator2` |
| 6 | Đặt **biến môi trường** (xem ngay dưới bảng) | — | Mở cmd **mới**: `echo %ANDROID_HOME%` · `adb version` · `emulator -version` |
| 7 | Tạo **thiết bị ảo** Pixel_9 | Android Studio → Device Manager → Create Virtual Device. Nên chọn image **Google APIs x86_64** (không Play Store, không bản 16 KB) — ổn định hơn | Device Manager thấy Pixel_9 |
| 8 | Cài **app** lên thiết bị ảo | `adb install <file>.apk` | `adb shell pm path com.anhtester.book` in ra đường dẫn |
| 9 | Tạo file **`.env`** (chỉ cần cho case đăng nhập đúng) | Copy `.env.example` → `.env`, điền `TEST_USERNAME` / `TEST_PASSWORD` | — |

**Không cần cài Maven** (có Maven Wrapper `mvnw.cmd`) và **không cần cài Allure** (CLI nằm sẵn trong `.allure/`).

### Bước 6 — biến môi trường

Appium cần `ANDROID_HOME` để tìm Android SDK. Lệnh `adb` và `emulator` chỉ gõ được khi 2 thư mục SDK nằm trong `Path`.

1. Mở **Start → "Edit environment variables for your account"**
2. **New…** → Name: `ANDROID_HOME` · Value: `C:\Users\<tên-user>\AppData\Local\Android\Sdk`
3. Chọn biến **Path** → **Edit** → **New**, thêm 2 dòng:
   - `%ANDROID_HOME%\platform-tools` (chứa `adb`)
   - `%ANDROID_HOME%\emulator` (chứa `emulator`)
4. **OK** → **đóng hết** terminal và VS Code, mở lại mới có hiệu lực

> Chỉ cần `ANDROID_HOME` thì gõ trong cmd cũng được: `setx ANDROID_HOME "%LOCALAPPDATA%\Android\Sdk"`. **Không** dùng `setx` để sửa `Path` — nó cắt `Path` còn 1024 ký tự, mất các đường dẫn khác.
>
> Chưa kịp đặt `Path` mà cần mở emulator ngay: gọi thẳng đường dẫn đầy đủ `%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe -avd Pixel_9 -gpu host`.

---

## 2. Mỗi lần chạy test — 4 bước

**Bước 1 — bật thiết bị ảo** (một trong hai cách):

- Android Studio → **Device Manager** → bấm ▶ ở Pixel_9
- Hoặc cmd: `emulator -avd Pixel_9 -gpu host`

Đợi tới màn hình chính, kiểm bằng `adb devices` phải thấy `emulator-5554   device`.

> Luôn dùng `-gpu host` khi mở bằng lệnh — thiếu nó emulator có thể chạy đồ hoạ phần mềm (SwiftShader) và WebView của app bị **đen màn**.

**Bước 2 — bật Appium server** (một cửa sổ cmd riêng, để chạy suốt lúc test):

```bash
appium --log-level warn
```

- Bật xong Appium **không in gì cả** — đó là bình thường. Kiểm bằng `curl http://127.0.0.1:4723/status`, thấy `"ready":true` là được.
- ⚠️ **Đừng click vào cửa sổ Appium** trong lúc test chạy. Click làm cmd vào chế độ chọn chữ (tiêu đề hiện "Select") và **tạm dừng Appium** → test đứng mãi. Lỡ click thì nhấn **Esc**. Tắt hẳn: chuột phải thanh tiêu đề → Properties → bỏ tick **QuickEdit Mode**.
- Chưa đặt `ANDROID_HOME` vĩnh viễn (bước 6 mục 1) thì gõ `set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk` **ngay trước** `appium`, trong **cùng** cửa sổ. PowerShell dùng `$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"`.

> `--log-level warn` là có chủ đích: ở mức `info`, Appium ghi cả nội dung gõ vào ô nhập — **kể cả mật khẩu**. Chỉ hạ xuống `info` khi cần soi lỗi, và đừng gửi log đó ra ngoài.

**Bước 3 — chạy test** (terminal khác, trong thư mục project):

```bash
.\mvnw.cmd clean test
```

macOS / Linux: `./mvnw clean test`. Chữ Việt trong console bị lỗi (`M? phiên`) thì chạy `chcp 65001` trước.

**Bước 4 — xem report:** `.\mvnw.cmd allure:serve` (chi tiết ở mục 5).

> ⚠️ Đóng **Appium Inspector / Appium MCP** trên cùng device trước khi chạy. Android chỉ cho **một** phiên UiAutomator2 tại một thời điểm — hai phiên giành nhau sẽ báo lỗi kiểu *instrumentation process is not running*, trông như lỗi locator nhưng không phải.

---

## 3. Thiết bị khác emulator-5554

Kiểm `capabilities/devices.json` khớp với device đang chạy — `udid` lấy từ `adb devices`.

---

## 4. Chạy song song & thêm device — sửa đúng 1 chỗ

Parallel **luôn bật** theo device (`parallel="tests"` trong `testng.xml`): mỗi khối `<test>` là một device, chạy trên một thread riêng. Với Appium, **số luồng = số device đang chạy thật** — hiện chỉ có 1 emulator nên `thread-count="1"`. Không đặt 5 luồng trên 1 emulator.

Thêm một device:

1. Thêm 1 khối vào `capabilities/devices.json` — `udid` và `appium:systemPort` **riêng** (vd. `8202`)
2. Thêm 1 khối `<test>` trong `testng.xml`, tham số `device` trỏ tới khối vừa thêm
3. Tăng `thread-count` trong `testng.xml` bằng số khối `<test>`

---

## 5. Mở report

Kết quả nằm trong `reports/allure-results/`. Mở bằng CLI **có sẵn trong project** — không cài gì lên máy:

```powershell
.\mvnw.cmd allure:serve          # sinh report + mở browser luôn
.\mvnw.cmd allure:report         # chỉ sinh vào reports/allure-report
```

Gọi thẳng CLI, không qua Maven:

```powershell
.allure\allure-2.46.1\bin\allure.bat generate reports\allure-results -o reports\allure-report --clean
.allure\allure-2.46.1\bin\allure.bat open reports\allure-report
```

**Bản 1 file HTML** để gửi người chỉ cần xem:

```powershell
.allure\allure-2.46.1\bin\allure.bat generate reports\allure-results -o reports\allure-report-single --single-file --clean
```

| Máy người xem có | Cách xem |
|---|---|
| JDK | Các lệnh ở trên |
| Chỉ có Node.js | `npx allure generate reports/allure-results` — Allure 3, giao diện **Awesome** (khác giao diện Allure 2, cùng dữ liệu) |
| Chỉ có browser | Gửi file `reports/allure-report-single/index.html` |

> ⚠️ Report chứa **ảnh chụp app thật** — kiểm nội dung trước khi gửi ra ngoài team.

`.allure/` bị xoá thì chạy `.\mvnw.cmd validate` là giải nén lại.

---

## 6. Cấu trúc project

```text
appium-android-app-automation/
├── pom.xml                       # dependency + plugin (surefire, aspectj, Allure CLI cục bộ)
├── testng.xml                    # parallel="tests" — MỖI <test> LÀ MỘT DEVICE
├── capabilities/devices.json     # capabilities từng device: udid, systemPort riêng
├── .env.example                  # mẫu cấu hình — copy thành .env
├── apps/                         # nơi đặt APK (dùng cho CI)
├── src/main/java/com/anhtester/book/
│   ├── config/AppConfig.java             # đọc -D → biến môi trường → .env
│   ├── drivers/AppiumDriverFactory.java  # ThreadLocal<AndroidDriver>
│   ├── drivers/CapabilitiesManager.java  # đọc capabilities/devices.json
│   ├── screens/                          # Screen Object: BaseScreen, DashboardScreen, LoginScreen
│   └── utils/                            # ScreenshotUtil, TestDataGenerator
├── src/test/java/com/anhtester/book/
│   ├── annotations/TcId.java     # mã TC → label testId trong Allure
│   ├── base/BaseTest.java        # mở/đóng phiên, chụp ảnh cuối MỌI test
│   └── tests/LoginTest.java
├── src/test/resources/log4j2.xml # log ra console + reports/logs/
├── reports/                      # TOÀN BỘ output (git-ignored)
└── .github/workflows/appium-android.yml
```

---

## 7. Quy ước

| Thành phần | Quy tắc |
|---|---|
| Screen Object | Hậu tố `Screen`, locator khai ở đầu class — không viết locator trong test |
| Test class / method | Hậu tố `Test` · method bắt đầu bằng `test` + hành vi |
| Metadata mỗi test | `description` Tiếng Việt · `@Description` · `@Severity` · `@Feature`/`@Story` · `@TcId` |
| Thân test | Bọc trong `Allure.step("Arrange: …")` / `Act:` / `Assert:` · method Screen có `@Step` |
| Chờ | Chỉ smart wait (`WebDriverWait`) — **cấm** `Thread.sleep()` |
| Log | Log4j2 — **cấm** `System.out.println()` |
| Dữ liệu nhạy cảm | Chỉ nằm trong `.env` / GitHub Secrets. **Không** truyền mật khẩu làm tham số của method `@Step` — Allure ghi tham số vào `allure-results`. Bọc bằng `Allure.step(...)` như `LoginScreen.enterPassword` |

---

## 8. Ghi chú về app & locator

- **App hybrid, WebView không bật debug** → chỉ có context `NATIVE_APP`, không dùng được CSS. Locator lấy từ cây accessibility mà WebView đưa ra.
- **Không dùng `resource-id` dạng `_r_d_`, `_r_e_`** — do React tự sinh theo thứ tự render, đổi bất kỳ lúc nào.
- Ô Email / Password bám theo **nhãn hiển thị** (XPath tương đối: ô nhập nằm ngay sau nhãn) — `hint` không ổn định, có lần WebView để rỗng. Nút Login bám theo text. Muốn locator bền hơn: nhờ dev đặt `id` cố định cho các ô và nút — id trong trang web được WebView đưa ra thành `resource-id`.
- **`appium:noReset` = `false`**: mỗi phiên xoá dữ liệu app trước khi mở, để test luôn bắt đầu từ trạng thái chưa đăng nhập.
- **Toast lỗi không đọc được bằng automation.** Đăng nhập sai thì app hiện toast (vd. "User not found.") khoảng 3 giây, nhưng toast **không nằm trong cây accessibility** — đã kiểm cả chế độ nhiều cửa sổ. Test chỉ khẳng định được việc ở lại màn Sign in. Muốn kiểm nội dung thông báo: nhờ dev đưa toast vào accessibility (`role="alert"` / `aria-live`) — việc này cũng giúp người dùng screen reader.
- ⚠️ **Page source màn Sign in lộ mật khẩu đã gõ** dưới dạng chữ thường (WebView đưa text của ô Password ra accessibility). Không đính page source vào report, không dán page source màn này ra ngoài.

---

## 9. CI/CD — GitHub Actions

File: `.github/workflows/appium-android.yml` — cài Appium, bật emulator API 34 trên runner, chạy test, sinh Allure report, upload **một** artifact là thư mục `reports/`.

Hai việc phải làm trước khi CI chạy được:

1. **Đặt workflow đúng chỗ.** GitHub chỉ đọc `.github/workflows/` ở **gốc repo**. Project nằm trong thư mục con → chép file sang `<gốc repo>/.github/workflows/` (đường dẫn trong file đã trỏ sẵn vào thư mục project).
2. **Có file APK.** Emulator của GitHub chưa cài app. Kéo APK từ máy đang có app rồi đặt vào `apps/book-app.apk`:

   ```powershell
   adb shell pm path com.anhtester.book     # in ra đường dẫn base.apk
   adb pull <đường dẫn base.apk> apps/book-app.apk
   ```

Tài khoản test khai trong **GitHub Secrets**: `TEST_USERNAME`, `TEST_PASSWORD`.

---

## 10. Xử lý sự cố

| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `Connection refused` tới `127.0.0.1:4723` | Chưa mở Appium server | Làm Bước 2 mục 2 |
| Test đứng mãi sau dòng `Mở phiên Appium…` | Đã click vào cửa sổ cmd của Appium → cmd vào chế độ chọn chữ và tạm dừng Appium | Bấm vào cửa sổ đó, nhấn **Esc**; tắt QuickEdit Mode để khỏi lặp lại |
| `Could not find a driver for automationName 'UiAutomator2'` | Appium chưa có driver | `appium driver install uiautomator2` |
| `... install ... appium-uiautomator2-server ... timed out after 20000ms` | Emulator cài UiAutomator2 server APK chậm — thường ở lần đầu, hoặc ngay sau khi dùng Appium MCP / Inspector bản driver khác | `devices.json` đã nâng `appium:uiautomator2ServerInstallTimeout` lên 120 s; máy chậm hơn thì tăng tiếp |
| `Neither ANDROID_HOME nor ANDROID_SDK_ROOT…` | Appium mở khi chưa có biến `ANDROID_HOME` | Đặt biến rồi mở lại Appium |
| `instrumentation process is not running` / `could not proxy command` | Phiên khác (Inspector / Appium MCP) đang giữ device | Đóng phiên kia rồi chạy lại |
| `Could not find a connected Android device` dù `adb devices` thấy máy | Appium mở khi thiếu `ANDROID_HOME` (vd. gõ `$env:` trong cmd), hoặc một Appium cũ vẫn giữ cổng 4723 | Tắt Appium cũ, đặt biến đúng cú pháp rồi mở lại (Bước 2 mục 2) |
| Ảnh lỗi đen toàn màn hình | Emulator chạy đồ hoạ phần mềm (SwiftShader) | Mở lại emulator bằng `-gpu host` hoặc từ Android Studio |
| Hộp thoại "System UI isn't responding" che app | Emulator vừa boot, System UI chưa ổn định | Bấm **Wait**, đợi emulator ổn định rồi chạy lại |
| `Thiếu cấu hình TEST_USERNAME` | Chưa có `.env` hoặc để trống | Điền `.env` theo bước 9 mục 1 |
| `Device ... was not in the list of connected devices` | `udid` trong `devices.json` không khớp | Sửa theo `adb devices` |
