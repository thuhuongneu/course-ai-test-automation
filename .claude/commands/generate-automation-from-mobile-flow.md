---
description: Sinh automation Appium từ flow mobile chạy thật trên device/emulator — hỗ trợ Native Android, Native iOS, Flutter, Hybrid. Thu locator từ UI hierarchy thật, sinh Screen Object + test, chạy và tự sửa đến khi PASS.
skills:
  - skills-mobile-debug-agent
  - skills-smart-locator-agent
  - skills-qa-automation-engineer
  - skills-test-data-generator
---

# Workflow: Sinh Automation Mobile từ Flow Thật

> **BẮT BUỘC (MANDATORY SKILLS):** Nạp và đọc kỹ trước khi bắt đầu:
> - **`skills-mobile-debug-agent`** (`.claude/skills/skills-mobile-debug-agent/SKILL.md`) — recon hierarchy, nhận diện loại app, thu locator
> - **`skills-smart-locator-agent`** — sinh locator ổn định
> - **`skills-qa-automation-engineer`** — quy tắc automation chung
>
> Và tuân thủ **`.claude/rules/appium_rules.md`** — locator theo nền tảng, gesture, Flutter, parallel multi-device.

Workflow chạy flow mobile **trên device/emulator thật** qua Appium MCP, thu locator từ UI hierarchy, sinh Screen Object + test class, chạy và tự sửa đến khi PASS ổn định.

## Workflow này khác gì các workflow lân cận?

| | Workflow này | `generate-automation-from-ui-flow` | `generate-automation-from-testcases` |
|---|---|---|---|
| **Nền tảng** | Mobile (Appium) | Web (Playwright MCP) | Web hoặc mobile |
| **Input** | App + flow mô tả bằng lời | URL + flow | File manual test cases |
| **Recon** | UI hierarchy trên device | DOM trên browser | Theo TC rồi verify |

> Đã có sẵn bộ manual TC cho app mobile → dùng `/generate-automation-from-testcases`, workflow đó cũng ra Appium. Workflow này dành cho lúc **chưa có TC**, chỉ biết "mở app, đăng nhập, tạo đơn".

## ⚠️ Nguyên tắc thực thi

- **Tất cả output bằng Tiếng Việt**
- **TUYỆT ĐỐI KHÔNG ĐOÁN locator** — lấy từ hierarchy thật, verify bằng `appium_find_element`
- **KHÔNG dùng toạ độ** `tap(x, y)` thay cho locator, kể cả khi bí
- **Nhận diện loại app TRƯỚC khi tìm locator** — Native / Flutter / Hybrid quyết định toàn bộ cách làm phía sau
- Hierarchy chỉ có `FlutterView` rỗng → **DỪNG, báo user**, không bịa locator (xem Bước 2)
- ⚠️ **Rule E3:** test FAIL → tự đọc log → phân tích → sửa → chạy lại, **KHÔNG hỏi user**. Chỉ hỏi khi business rule mâu thuẫn, app/device không truy cập được, hoặc hết 5 vòng auto-heal
- **Artifact `task.md`** — PHẢI tạo để theo dõi tiến độ

## Input cần thu thập

| Input | Bắt buộc? | Ghi chú |
|---|---|---|
| **File app** (`.apk` / `.ipa` / `.app`) hoặc app đã cài trên device | ⭐ | Kèm `appPackage`/`appActivity` (Android) hoặc `bundleId` (iOS) nếu app đã cài |
| **Flow cần automate** | ⭐ | Mô tả từng bước bằng lời |
| **Loại app** | ⭕ | Không biết cũng được — Bước 2 tự nhận diện. Biết trước thì nhanh hơn |
| **Device/emulator** | ⭐ | Tên device, hoặc để agent liệt kê bằng `select_device` |
| **Tài khoản test** | ⭕ | Nếu flow cần đăng nhập |
| **Build mode (Flutter)** | ⭕ | debug/profile hay release — quyết định chọn Đường A hay B |

Thiếu app hoặc device → **hỏi trước khi bắt đầu**, không tự tạo emulator mà không báo.

## Các bước thực hiện

### Bước 1: Chuẩn bị phiên (Session Setup)

1. Tạo `task.md` theo dõi 6 bước
2. `select_device` → liệt kê device/emulator sẵn có, chốt với user nếu có nhiều
3. iOS: `prepare_ios_simulator` (simulator) hoặc `appium_prepare_ios_real_device` (real device — cần WDA đã ký)
4. `appium_session_management` (action=create) với `automationName` theo loại app:

   | Loại app | `automationName` |
   |---|---|
   | Native Android | `UiAutomator2` |
   | Native iOS | `XCUITest` |
   | Chưa rõ / Flutter Đường A | `UiAutomator2` / `XCUITest` |
   | Flutter Đường B | `Flutter` hoặc `FlutterIntegration` (cần build debug/profile) |

5. Capabilities nên bật sẵn: `autoGrantPermissions` (Android) / `autoAcceptAlerts` (iOS) — tránh dialog quyền chặn flow

### Bước 2: Nhận diện loại app (⏸️ CHECKPOINT nếu là Flutter)

`appium_get_page_source` → đối chiếu:

| Hierarchy | Kết luận | Hành động |
|---|---|---|
| Nhiều `android.widget.*` / `XCUIElementType*` | **Native** | Sang Bước 3 |
| **Chỉ** `FlutterView` rỗng | **Flutter, semantics chưa bật** | 🛑 **DỪNG** — xem dưới |
| `FlutterView` + node có `content-desc` | **Flutter, semantics đã bật** | Sang Bước 3, locator dùng `accessibilityId` |
| Có `WebView` | **Hybrid** | Ghi nhận, Bước 3 sẽ đổi context |

**Nếu Flutter chưa bật semantics — DỪNG và báo user đúng 3 việc cần hỏi dev:**

1. Đã bọc `Semantics(label: '...')` cho widget cần test chưa?
2. Đã gọi `SemanticsBinding.instance.ensureSemantics()` chưa?
3. Có bản build debug/profile không? (quyết định dùng được Đường B hay không)

🚨 **Nói rõ ngay:** dev trả lời *"đã thêm `Key('login_btn')`"* thì **vẫn chưa dùng được** — `Key`/`ValueKey` chỉ flutter driver thấy, không sinh `content-desc`. Nêu trước để khỏi mất thêm một vòng trao đổi.

**Công bố kết luận ở đầu output**, ví dụ: *"App Flutter, semantics đã bật → đi Đường A, locator qua accessibility id."*

### Bước 3: Chạy flow thật & thu locator (Recon)

Với **mỗi bước** trong flow user mô tả:

1. `appium_get_page_source` → đọc hierarchy màn hình hiện tại
2. Xác định element cần thao tác, lấy locator theo thứ tự ưu tiên của nền tảng
3. **Verify** bằng `appium_find_element` — phải khớp đúng 1 element, đúng element cần thao tác
4. Thao tác thật (`appium_gesture` tap/swipe, `appium_set_value` nhập liệu) → sang màn hình kế
5. `appium_screenshot` ở các màn hình mốc — làm bằng chứng recon
6. Element ngoài màn hình → `appium_gesture` `action=scroll_to_element`, **không** tăng timeout

**Chạy đủ 2 nền tảng nếu dự án cần cả Android lẫn iOS** — locator thường khác nhau, đừng suy từ nền tảng này sang nền tảng kia.

**Bảng Locator Collection** (đầu ra bắt buộc của bước này):

| Screen | Element | Platform | Locator | Verified |
|---|---|---|---|---|
| LoginScreen | Ô email | Android | `AppiumBy.accessibilityId("email_input")` | ✅ |
| LoginScreen | Ô email | iOS | `AppiumBy.accessibilityId("email_input")` | ✅ |
| LoginScreen | Nút đăng nhập | Android | `AppiumBy.id("com.app:id/btn_login")` | ✅ |

**Xử lý tình huống:**

| Tình huống | Cách xử lý |
|---|---|
| Element không có id, không có content-desc | Báo user + đề xuất dev bổ sung. Tạm dùng locator theo text và **đánh dấu 🟡 mong manh** |
| Danh sách Flutter cuộn được | Item chưa cuộn tới thì chưa render — cuộn rồi tìm lại, không phải locator sai |
| Màn hình có WebView | `appium_context` sang `WEBVIEW_*`, lấy locator web, **quay lại `NATIVE_APP`** trước thao tác native |
| OTP / CAPTCHA / sinh trắc học | Không automate được — báo user, đánh dấu SKIP |
| App crash giữa flow | Chụp màn hình + log, báo user — **không** tự sửa flow để né |

### Bước 4: Thiết kế Screen Objects

1. Mỗi màn hình → 1 **Screen class** (hậu tố `Screen`, không phải `Page`)
2. Cấu trúc: locator ở đầu class → constructor nhận driver → action method mô tả **hành vi nghiệp vụ** → verification method
3. **Locator khác nhau giữa Android/iOS thì tách theo nền tảng ngay trong Screen class** — cấm rải `if (isAndroid)` khắp test class
4. Method đánh dấu step API (`@Step`) → sinh sub-step trong report
5. Kế thừa `BaseScreen` (smart wait, gesture helper) — chưa có thì tạo theo CODE_TEMPLATES § 4

### Bước 5: Sinh Test + Test Data

1. Test class TestNG theo `Arrange` / `Act` / `Assert`, mỗi test ≥ 1 assertion có message rõ ràng
2. **Allure metadata bắt buộc** (theo `.claude/rules/reporting_rules.md`): tên test Tiếng Việt · Description · Severity · Tags · label `testId`
3. Screenshot trạng thái cuối đính ở teardown cho **mọi** test — PASS lẫn FAIL
4. Test data unique + traceable (`auto_<flow>_<timestamp>`), không hardcode
5. Driver theo `ThreadLocal`, `remove()` ở teardown — parallel luôn bật

### Bước 6: Chạy & Auto-Heal (RULE E3)

```bash
mvn test -Dtest=<TestClass>
```

Vòng lặp tối đa **5 vòng**:

| Lỗi | Hành động |
|---|---|
| `NoSuchElementException` | Dump hierarchy lại → verify/thay locator |
| Element không tương tác được | Kiểm `displayed`/`enabled`, scroll tới trước |
| Locator native fail hàng loạt | Kiểm context có đang kẹt ở WEBVIEW không |
| Timeout | Thêm điều kiện chờ trạng thái — **KHÔNG** thêm sleep |
| Phiên iOS không mở được | WDA chưa ký — báo user, không phải lỗi code |
| Trùng port khi chạy nhiều device | Cấp `systemPort`/`wdaLocalPort` riêng |
| Test data trùng | Sinh data unique mới |

**Verify ổn định:** test phải PASS **2 lần liên tiếp** mới coi là xong.

### Bước 7: Cleanup & Delivery

- [ ] Xoá debug log, commented code, locator không dùng
- [ ] Không còn `Thread.sleep()`, không còn toạ độ cứng
- [ ] Locator 🟡 mong manh đã liệt kê trong báo cáo kèm đề xuất cho dev
- [ ] Cập nhật `task.md`: TC nào PASS / SKIP (kèm lý do)

## Output

- **Kết luận loại app** — Native / Flutter (semantics bật chưa) / Hybrid, công bố ngay đầu output
- **Bảng Locator Collection** — tách dòng theo nền tảng, có cột Verified
- **Screen Object classes** — locator đã verify, tách theo nền tảng khi cần
- **Test class** — Allure metadata đủ, đã PASS 2 lần liên tiếp
- **Ảnh recon** các màn hình mốc
- **Danh sách việc cần dev làm** — element thiếu id, Flutter chưa bật semantics
- **Artifact `task.md`**
