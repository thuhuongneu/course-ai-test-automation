# JIM POS — Automation Test (Flutter Windows Desktop)

Automation test cho app **ConnectPOS desktop** của dự án Jim Thompson, build
`Jim - STAG - 26.09.14 - 09.43`.

---

## 1. ⚠️ Lựa chọn driver — ĐÃ CHỐT: giữ driver MSAA tự viết

> **Đính chính:** bản README trước ghi *"UIA rỗng, chỉ thấy 1 node FLUTTERVIEW → Appium/WinAppDriver
> không dùng được"*. **Kết luận đó SAI** — phép đo lúc đó thực hiện **trước khi bật cờ
> screen-reader** (xem mục "Điều kiện tiên quyết" bên dưới), nên UIA đương nhiên rỗng, không phải vì
> UIA "mù" với Flutter. Đo lại **sau khi bật cờ đúng cách**: `FindAll(TreeScope.Descendants)` trả về
> **59 node**, đầy đủ `Button`/`Text`/`Edit` — UIA đọc được app này bình thường.
>
> Vì UIA đọc được, đã benchmark thật (không kết luận bằng suy đoán) trên cùng kịch bản
> tìm+click+gõ text vào Username/Password/LOGIN, **5 vòng mỗi driver, cùng một phiên, liên tiếp,
> restart app giữa mỗi vòng**, trong **điều kiện sạch** (Focus Assist bật, không ai chạm vào máy):

| Driver | Pass rate | TB thời gian | Số lần mất foreground | Ghi chú |
|---|---|---|---|---|
| Appium + WinAppDriver | Không cài được | — | — | UAC chặn cài đặt MSI (máy không có quyền admin). WinAppDriver cũng đã ngừng bảo trì từ 2022. Nhánh khép lại, không đánh giá tiếp |
| FlaUI UIA3 — chốt foreground **giống hệt MSAA** | **0/5** | 8.64s | **0** | Password rớt đúng 1 ký tự (9/10) sau cả 3 lần retry, lặp lại ở **cả 5 vòng** |
| **Driver MSAA tự viết (`driver/AxDriver.cs`)** | **5/5** | 4.73s | **0** | Không lỗi |

### Đã loại trừ giả thuyết "mất focus"

Nghi ngờ hợp lý ban đầu: `SendInput` gửi phím vào cửa sổ đang focus, nên rớt lẻ 1 ký tự có thể chỉ
là do người dùng chạm vào máy giữa lúc đo. Cách kiểm chứng: cấp cho FlaUI **đúng logic foreground
của MSAA** (`EnsureForeground`: alt-trick + `AttachThreadInput`, kiểm tra lại trước **từng ký tự**),
rồi ghi lại handle cửa sổ foreground trước mỗi lần gõ.

Kết quả: **cả 5 vòng đều ghi nhận foreground là ConnectPOS ở mọi thời điểm gõ, không mất lần nào**
(`fg_lost=0`) — mà password vẫn rớt đúng 1 ký tự. Giả thuyết bị bác bỏ bằng số liệu: lỗi nằm ở
đường gõ phím của FlaUI, không phải do nhiễu từ người dùng. Hai bug thật đã tìm và sửa trong lúc
đo (`Keyboard.Press` không tự nhả phím; `ValuePattern.SetValue` ném lỗi COM trên Edit control của
Flutter) cũng không làm thay đổi kết quả.

> ⚠️ **Số liệu benchmark trước đó (10 vòng) đã bị loại bỏ, đừng dùng lại.** Script đo lúc ấy xác
> minh "đã vào Select Register chưa" bằng cách `grep` toàn bộ output của driver — trong khi lệnh
> `quit` **luôn** trả về `@END ok bye`, nên phép kiểm luôn đúng và mọi vòng đều được chấm PASS.
> Con số "MSAA 10/10" sinh ra từ đó **không xác minh gì cả**. Bảng trên dùng phép kiểm đã sửa: chỉ
> đọc đúng dòng trả lời của lệnh `exists`.

> **Kết luận:** giữ nguyên **driver MSAA**, không đổi kiến trúc. FlaUI/UIA3 đọc được cây element
> của app này, nhưng đường gõ phím của nó không tin cậy được trên ô input Flutter — và nguyên nhân
> đó **không** phải nhiễu môi trường.

Bối cảnh kỹ thuật không đổi: app viết bằng **Flutter** → vẽ giao diện lên canvas, không có control
Windows thật. Cả UIA lẫn MSAA đều chỉ đọc được cây element **sau khi bật cờ screen-reader** — đây
là điều kiện tiên quyết chung cho **mọi** driver, không riêng gì MSAA.

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
> hơn để mật khẩu rơi vào cửa sổ khác đang mở. Kiểm tra lại trước **từng ký tự**, mất foreground
> giữa chừng thì dừng ngay và gõ lại cả thao tác từ đầu.

Driver còn **ghi lại cửa sổ foreground ngay trước mỗi lần gõ** (dòng `FG` trên stdout), và khi một
lệnh thất bại thì các dòng đó được đính vào thông báo lỗi phía Java. Trên CI không ai ngồi nhìn màn
hình: không có dấu vết này thì một lần bị cửa sổ khác chen lên chỉ để lại triệu chứng "thiếu 1 ký
tự", rất dễ quy oan cho driver. **Đã dùng chính dấu vết này để bác bỏ giả thuyết "mất focus" ở mục
1** — xem bảng benchmark.

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

**Muốn test bản build khác — chỉ cần sửa `JIM_APP_PATH`, không sửa code.** Trước khi chạy, `setUp()`
tự so sánh **đường dẫn `.exe` thật** của tiến trình đang chạy (nếu có) với giá trị trong `.env`:
lệch nhau thì tự đóng bản đang chạy và mở đúng bản khai trong `.env`, khớp rồi thì dùng nguyên,
không mở lại cho mất thời gian. Không cần tự tay đóng app trước khi đổi `.env`.

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
