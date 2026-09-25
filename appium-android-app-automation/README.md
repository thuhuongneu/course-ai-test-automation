# appium-android-app-automation

Framework automation **Appium + Java + TestNG** cho app Android **Book** (`com.anhtester.book`) — app hybrid, giao diện nằm trong WebView.

---

## 1. Yêu cầu máy

| Thành phần | Phiên bản | Ghi chú |
|---|---|---|
| JDK | 11 trở lên | Maven và Allure CLI đều chạy trên JVM |
| Node.js | 20 trở lên | Để chạy Appium server |
| Appium | 3.x + driver `uiautomator2` | `npm i -g appium` · `appium driver install uiautomator2` |
| Android SDK | — | Biến `ANDROID_HOME` phải trỏ đúng SDK khi mở Appium |
| Emulator / máy thật | Đã cài app `com.anhtester.book` | Kiểm bằng `adb devices` |

**Không cần cài Maven** — project có Maven Wrapper (`mvnw` / `mvnw.cmd`).
**Không cần cài Allure** — bộ CLI được giải nén vào `.allure/` ngay trong project.

---

## 2. Cài đặt lần đầu

1. Copy `.env.example` thành `.env`, điền tài khoản test:

   ```dotenv
   TEST_USERNAME=<email đăng nhập>
   TEST_PASSWORD=<mật khẩu>
   ```

   `.env` đã nằm trong `.gitignore` — **không commit**, không gửi mật khẩu qua chat.
   Case mẫu hiện tại (đăng nhập bằng email chưa đăng ký) tự sinh dữ liệu nên chưa cần `.env`; các case đăng nhập đúng sau này đọc tài khoản qua `AppConfig.testUsername()` / `testPassword()`.

2. Kiểm `capabilities/devices.json` khớp với device đang chạy (`udid` lấy từ `adb devices`).

---

## 3. Chạy test

**Bước 1 — mở Appium server** (terminal riêng, để chạy suốt lúc test):

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
appium --log-level warn
```

Dùng **cmd** thay vì PowerShell thì đặt biến bằng `set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk` (cú pháp `$env:` chỉ chạy trong PowerShell). Appium bật xong ở mức `warn` sẽ không in gì — kiểm bằng `curl http://127.0.0.1:4723/status` thấy `"ready":true` là được.

Emulator nên mở từ Android Studio (Device Manager) hoặc `emulator -avd Pixel_9 -gpu host` — chạy đồ hoạ phần mềm (SwiftShader) thì WebView của app bị đen màn.

> `--log-level warn` là có chủ đích: ở mức `info`, Appium ghi cả nội dung gõ vào ô nhập — **kể cả mật khẩu**. Chỉ hạ xuống `info` khi cần soi lỗi, và đừng gửi log đó ra ngoài.

**Bước 2 — chạy test:**

```powershell
.\mvnw.cmd clean test
```

macOS / Linux: `./mvnw clean test`.

> ⚠️ Đóng **Appium Inspector / Appium MCP** trên cùng device trước khi chạy. Android chỉ cho **một** phiên UiAutomator2 tại một thời điểm — hai phiên giành nhau sẽ báo lỗi kiểu *instrumentation process is not running*, trông như lỗi locator nhưng không phải.

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
| `Connection refused` tới `127.0.0.1:4723` | Chưa mở Appium server | Làm Bước 1 mục 3 |
| `Could not find a driver for automationName 'UiAutomator2'` | Appium chưa có driver | `appium driver install uiautomator2` |
| `... install ... appium-uiautomator2-server ... timed out after 20000ms` | Emulator cài UiAutomator2 server APK chậm — thường ở lần đầu, hoặc ngay sau khi dùng Appium MCP / Inspector bản driver khác | `devices.json` đã nâng `appium:uiautomator2ServerInstallTimeout` lên 120 s; máy chậm hơn thì tăng tiếp |
| `Neither ANDROID_HOME nor ANDROID_SDK_ROOT…` | Appium mở khi chưa có biến `ANDROID_HOME` | Đặt biến rồi mở lại Appium |
| `instrumentation process is not running` / `could not proxy command` | Phiên khác (Inspector / Appium MCP) đang giữ device | Đóng phiên kia rồi chạy lại |
| `Could not find a connected Android device` dù `adb devices` thấy máy | Appium mở khi thiếu `ANDROID_HOME` (vd. gõ `$env:` trong cmd), hoặc một Appium cũ vẫn giữ cổng 4723 | Tắt Appium cũ, đặt biến đúng cú pháp rồi mở lại (mục 3) |
| Ảnh lỗi đen toàn màn hình | Emulator chạy đồ hoạ phần mềm (SwiftShader) | Mở lại emulator bằng `-gpu host` hoặc từ Android Studio |
| Hộp thoại "System UI isn't responding" che app | Emulator vừa boot, System UI chưa ổn định | Bấm **Wait**, đợi emulator ổn định rồi chạy lại |
| `Thiếu cấu hình TEST_USERNAME` | Chưa có `.env` hoặc để trống | Điền `.env` theo mục 2 |
| `Device ... was not in the list of connected devices` | `udid` trong `devices.json` không khớp | Sửa theo `adb devices` |
