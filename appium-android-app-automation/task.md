# Tiến độ dựng framework — appium-android-app-automation

- [x] Bước 1: Thu thập yêu cầu — Appium 3 + Java 11 + TestNG, Android, emulator-5554, app `com.anhtester.book`
- [x] Bước 2: Scaffold project structure — Maven Wrapper, `testng.xml`, `capabilities/devices.json`, `.gitignore`, README
- [x] Bước 3: Sinh base classes — `AppConfig`, `AppiumDriverFactory` (ThreadLocal), `CapabilitiesManager`, `BaseScreen`, `BaseTest`
- [x] Bước 4: Sinh example test — `LoginTest` (đăng nhập với email chưa đăng ký), PASS trên emulator-5554
- [x] Bước 5: Cấu hình reporting & CI/CD — Allure CLI cục bộ `.allure/`, GitHub Actions
- [x] Bước 6: Verify & Deliver

## Đã kiểm chứng

| Hạng mục | Kết quả |
|---|---|
| `mvnw validate test-compile` | ✅ BUILD SUCCESS (đã ghim Selenium 4.25.0 cho khớp java-client 9.3.0) |
| `.allure/allure-2.46.1/bin/allure --version` | ✅ 2.46.1 — không cài gì lên máy |
| `mvnw allure:report` | ✅ Sinh `reports/allure-report`, **không** tải CLI từ Internet |
| Workflow CI parse YAML | ✅ push · pull_request · workflow_dispatch · timeout 45' · upload `if: always()` |
| Mở phiên Appium trên emulator-5554 | ✅ sau khi nâng `uiautomator2ServerInstallTimeout` (emulator cài server APK > 20s) |
| Parallel | ✅ `parallel="tests"`, test chạy trên thread `TestNG-tests-1` — 1 luồng vì chỉ có 1 emulator |
| Allure result | ✅ tên Tiếng Việt · description · testId · severity · feature · story · step Arrange/Act/Assert có sub-step · ảnh `trang_thai_cuoi_cua_test` · không có stdout |
| Rò rỉ mật khẩu | ✅ Quét `reports/`, `target/surefire-reports`, log Appium — không có mật khẩu (`.env` hay mật khẩu giả của test) |
| Output | ✅ Toàn bộ nằm trong `reports/`, không có `allure-results/` / `test-output/` lạc ra ngoài |

## Kết quả từng TC

| TC ID | Test | Kết quả | Ghi chú |
|---|---|---|---|
| BOOK_LOGIN_TC_002 | Đăng nhập thất bại với email chưa đăng ký — ở lại màn Sign in | ✅ PASS (40 s) | Toast "User not found." hiện ~3 s nhưng không có trong cây accessibility → chỉ khẳng định ở lại màn Sign in |

## Việc để lại cho lần sau

- Case đăng nhập đúng (dự kiến `BOOK_LOGIN_TC_001`) — cần tài khoản tồn tại trên server; `.env` + `AppConfig.testUsername()/testPassword()` đã sẵn
- CI: chép workflow lên `.github/workflows/` ở gốc repo + đặt APK vào `apps/book-app.apk` (README mục 9)
