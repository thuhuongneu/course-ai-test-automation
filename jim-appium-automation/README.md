# JIM POS — Automation Test (Flutter Windows Desktop)

Automation test cho app **ConnectPOS desktop** của dự án Jim Thompson, build
`Jim - STAG - 26.09.14 - 09.43`.

---

## 1. Vì sao KHÔNG dùng Appium — đọc trước khi sửa gì

Tên thư mục là `jim-appium-automation`, nhưng **Appium không điều khiển được app này**. Đây là kết
luận từ đo đạc thực tế, không phải phỏng đoán.

App viết bằng **Flutter** → vẽ toàn bộ giao diện lên một canvas. Không có control Windows thật nào
tồn tại. Kết quả dò trên đúng bản build đang test:

| Kênh accessibility | Kết quả |
|---|---|
| UIA — `ControlViewWalker` | 1 node `FLUTTERVIEW` |
| UIA — `RawViewWalker` | 1 node `FLUTTERVIEW` |
| UIA — `FindAll(TreeScope.Descendants)` | **1 node** |
| **MSAA / `IAccessible`** | **Đầy đủ cây element** ✅ |

**WinAppDriver — nền tảng mà `appium-windows-driver` dựa vào — chạy trên UIA.** UIA không thấy gì
thì Appium cũng không thấy gì.

Vì vậy project dùng đúng mô hình của Appium nhưng thay tầng driver:

```
Tầng test (Java + TestNG + Allure)  ←→  Driver MSAA (C#, driver/AxDriver.cs)
        Page Object                          nói thẳng IAccessible
```

Driver được biên dịch bằng `csc.exe` **có sẵn trong mọi bản Windows** → không phải cài thêm gì.
`AxDriver.java` tự biên dịch lại khi `AxDriver.cs` thay đổi.

### Điều kiện tiên quyết: cờ screen-reader

Flutter **chỉ sinh cây accessibility khi phát hiện có trợ lý màn hình**. Driver tự bật cờ
`SPI_SETSCREENREADER` khi khởi động và **trả lại giá trị cũ khi kết thúc**. Cờ này nằm trong RAM,
không ghi registry, tự mất khi logoff.

> ⚠️ Nếu app **đã chạy trước khi** cờ được bật, phải **khởi động lại app** thì Flutter mới đọc cờ.
> `BaseTest` tự xử lý việc này khi cần.

> 🔑 Vì lý do trên, **driver sống suốt cả suite** (`@BeforeSuite` / `@AfterSuite`), không tạo lại
> theo từng test. Tạo lại mỗi test đồng nghĩa tắt cờ giữa hai test trong khi app vẫn chạy → từ test
> thứ hai trở đi cây semantics không còn đảm bảo, sinh ra test đỏ ngẫu nhiên. **Đừng chuyển nó
> xuống `@BeforeMethod`.**

---

## 2. Yêu cầu môi trường

| Thành phần | Ghi chú |
|---|---|
| Windows | Bắt buộc — app là .exe, driver dùng API Windows |
| Java 11+ | Đã có: `C:\Program Files\Microsoft\jdk-11.0.29.7-hotspot` |
| Maven | Dùng bản đóng sẵn trong repo: `../selenium-test-automation/.maven-local/apache-maven-3.9.9` |
| `csc.exe` | Có sẵn trong Windows (`%SystemRoot%\Microsoft.NET\Framework64\v4.0.30319`) |
| App JIM POS | Khai đường dẫn trong `.env` |

### 🔴 Máy KHÔNG được khoá màn hình khi chạy

Automation desktop điều khiển **chuột và bàn phím thật**, và app phải ở foreground. Máy khoá màn
hình thì suite dừng ngay với thông báo rõ ràng. Driver nhận biết bằng `OpenInputDesktop` —
**không** dựa vào sự tồn tại của tiến trình `LockApp`, vì trên Windows 11 tiến trình đó vẫn nằm
trong danh sách (ở trạng thái treo) sau khi đã mở khoá.

**Trước khi chạy suite:** tắt tự động khoá màn hình, và **không dùng máy trong lúc chạy** — click
sang cửa sổ khác sẽ làm app mất foreground.

> Driver có chốt an toàn: **app không ở foreground thì tuyệt đối không gõ phím**. Thà dừng test còn
> hơn để mật khẩu rơi vào cửa sổ khác đang mở.

---

## 3. Cấu hình

```bash
cp .env.example .env
```

Điền vào `.env` (file này đã bị `.gitignore` chặn, **không commit**):

```
JIM_APP_PATH=C:\...\Jim - STAG - 26.09.14 - 09.43\ConnectPOS.exe
JIM_USERNAME=<username>
JIM_PASSWORD=<password>
```

---

## 4. Chạy test

Terminal của VS Code mặc định là **PowerShell** — dùng hai script dưới đây, không phải gõ `mvn`
tay. Script tự dò `JAVA_HOME`, tự đứng đúng thư mục project dù được gọi từ đâu.

**Chạy toàn bộ test:**

```powershell
.\run-tests.ps1
```

**Mở report** (sinh report rồi tự mở trình duyệt):

```powershell
.\show-report.ps1
```

**Chỉ sinh report ra đĩa, không mở trình duyệt** — dùng khi cần gửi report đi:

```powershell
.\show-report.ps1 -NoOpen
```

**Chạy một test lẻ** — tham số thêm vào được chuyển thẳng cho Maven:

```powershell
.\run-tests.ps1 -Dtest=LoginTest#login_validCredentials_opensSelectRegister -DfailIfNoSpecifiedTests=false
```

Toàn bộ output nằm trong `reports/` (đã `.gitignore`). Allure CLI được giải nén sẵn vào `.allure/`
từ kho `.m2` lúc build, nên **sinh report không cần mạng** — đã kiểm chứng bằng cờ `-o` (offline).

> Muốn gõ lệnh Maven tay thay vì dùng script (ví dụ chạy trong Command Prompt thay vì
> PowerShell) thì cú pháp nối lệnh khác nhau — PowerShell dùng `;`, CMD dùng `&&`:
> ```powershell
> $env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-11.0.29.7-hotspot"
> ..\selenium-test-automation\.maven-local\apache-maven-3.9.9\bin\mvn.cmd test
> ```

---

## 5. Cấu trúc

```
jim-appium-automation/
├── driver/AxDriver.cs                  ← Driver MSAA (thay cho WinAppDriver)
├── src/test/java/com/jim/pos/
│   ├── core/AxDriver.java              ← Client Java ↔ driver, giao thức stdin/stdout
│   ├── core/AxNode.java
│   ├── config/ConfigReader.java        ← đọc .env
│   ├── base/BaseTest.java              ← vòng đời app, đưa về màn Login, screenshot
│   ├── screens/LoginScreen.java        ← Page Object màn Login
│   ├── screens/SelectRegisterScreen.java
│   ├── screens/UpdatePromptDialog.java ← hộp thoại cập nhật chen ngang
│   └── tests/LoginTest.java
├── pom.xml · testng.xml · .env.example
└── reports/                            ← Allure output (gitignored)
```

---

## 6. Chiến lược locator — phần dễ sai nhất

### ❌ KHÔNG dùng tên để tìm ô nhập liệu

`accName` của ô nhập **chính là placeholder**, và nó **biến mất ngay khi ô có nội dung**:

| Trạng thái ô Username | `name` | `value` |
|---|---|---|
| Rỗng | `Username` | *(rỗng)* |
| Đã nhập | *(rỗng)* | `<username>` |

Dùng `name='Username'` làm locator thì test chạy được lần đầu và gãy ở lần thứ hai.

### ✅ Dùng role + thứ tự trên màn hình

`findByRole("TEXT")` trả về các ô đã **sắp xếp theo toạ độ** (trên→dưới, trái→phải):
ô `[0]` là Username, ô `[1]` là Password. Thứ tự này ổn định theo bố cục.

### ✅ Nút và thông báo thì dùng tên

`LOGIN`, `Logout`, `Select Register`, `Username is required!`… — tên cố định, dùng trực tiếp.

### Toạ độ click lấy từ element, không hardcode

Driver đọc `accLocation` của chính element rồi click vào tâm nó. Đổi kích thước cửa sổ vẫn đúng.

---

## 7. Những gì đã kiểm chứng trên app

| Tình huống | Hành vi thật |
|---|---|
| Đăng nhập đúng | Chuyển sang màn **Select Register** |
| **Sai mật khẩu** | Ở lại màn Login — **KHÔNG có thông báo lỗi nào** trong cây accessibility (đã đọc 5 lần liên tiếp sau khi bấm LOGIN) |
| Bỏ trống cả hai ô | Hiện `Username is required!` · `Password is required` · sau khi bấm LOGIN thì hiện `Please fill in your username and password to log in` |
| Ô mật khẩu | Phơi ra chuỗi dấu chấm tròn — **một dấu cho một ký tự**, nên kiểm chứng được độ dài mà không lộ mật khẩu |
| Hộp thoại `Do you want to update` | Bật lên **bất kỳ lúc nào**. Khi nó hiện, **toàn bộ màn hình phía sau biến mất khỏi cây semantics** → mọi phép tìm element trả về rỗng |

### Hai điểm nên báo dev

1. **Sai mật khẩu không có phản hồi nào trong accessibility tree.** Người dùng trình đọc màn hình
   không có cách nào biết vì sao đăng nhập thất bại.
2. **Thông báo không nhất quán dấu câu:** `Username is required!` có dấu chấm than,
   `Password is required` thì không.

---

## 8. Test cases

| TC ID | Nội dung | Severity |
|---|---|---|
| `JIM_LOGIN_TC_001` | Đăng nhập thành công → vào Select Register | blocker |
| `JIM_LOGIN_TC_002` | Sai mật khẩu → bị từ chối, ở lại màn Login | critical |
| `JIM_LOGIN_TC_003` | Bỏ trống → hiện thông báo bắt buộc nhập | normal |

TC_002 **cố ý không** khẳng định có thông báo lỗi — vì app không có. Test chỉ khẳng định điều quan
sát được, không bịa ra hành vi không tồn tại.

---

## 9. Giới hạn đã biết

- **Không chạy song song.** Chỉ có một chuột, một bàn phím, một foreground. `testng.xml` để tuần tự
  là cố ý, không phải để giấu test đỏ.
- **Không chạy headless / không chạy được trên CI thông thường.** Cần một phiên desktop thật, đã
  đăng nhập và không khoá màn hình.
- **Phụ thuộc cây semantics của Flutter.** Dev gỡ `Semantics` khỏi widget nào thì locator của widget
  đó biến mất. Build mới nên chạy lại `dump` để đối chiếu trước khi nghi ngờ test hỏng.
- **Dùng chung môi trường staging.** Test đăng xuất/đăng nhập lại tài khoản `<username>` — tránh chạy
  lúc người khác đang thao tác trên cùng tài khoản.

### Công cụ dò tay khi debug

```bash
printf 'dump\nquit\n' | target/driver/AxDriver.exe
printf 'findRole\tTEXT\nquit\n' | target/driver/AxDriver.exe
```
