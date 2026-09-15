# Chạy regression tự động trên GitHub Actions

Mỗi lần push code lên `main` (hoặc mở PR) chạm vào `jim-app-automation/`, GitHub sẽ tự chạy lại
toàn bộ test và báo kết quả về PR. Nhưng test này điều khiển **chuột và bàn phím thật** trên một
app desktop, nên không chạy được trên máy ảo của GitHub — phải đăng ký **máy Windows của mình**
làm nơi chạy (self-hosted runner).

Cài một lần, dùng mãi. Mất khoảng 15 phút.

---

## Phần 1 — Chuẩn bị máy chạy test

Máy này sẽ tự chạy test mỗi khi có người push code, nên:

| Yêu cầu | Lý do |
|---|---|
| Đã cài sẵn app ConnectPOS + JDK 11 trở lên | Test cần app thật để điều khiển |
| **Không khóa màn hình** khi chạy | Driver dừng ngay nếu màn hình khóa |
| Tắt sleep / screensaver | Máy ngủ giữa chừng là test đứt |
| Không ai dùng chuột/bàn phím lúc test chạy (~3 phút) | Chạm vào là app mất foreground, test đỏ oan |

Tắt sleep và screensaver: **Settings → System → Power & battery → Screen and sleep** đặt *Never*;
**Settings → Personalization → Lock screen → Screen saver** đặt *None*.

---

## Phần 2 — Đăng ký máy làm runner

1. Mở repo trên GitHub → **Settings** → **Actions** → **Runners** → **New self-hosted runner**
2. Chọn **Windows** / **x64**. GitHub hiện ra một loạt lệnh — mở **PowerShell** và chạy theo đúng
   thứ tự đó (tải về, giải nén, rồi `.\config.cmd --url ... --token ...`)
3. Khi `config.cmd` hỏi từng câu, trả lời:

   | Câu hỏi | Trả lời |
   |---|---|
   | Enter the name of the runner group | Enter (bỏ qua) |
   | Enter the name of runner | Enter (bỏ qua) |
   | Enter any additional labels | **`jim-pos`** ← bắt buộc, workflow tìm máy theo nhãn này |
   | Enter name of work folder | Enter (bỏ qua) |
   | Would you like to run the runner as service? | **N** ← bắt buộc phải là **No** |

> ⚠️ **Phải trả lời N ở câu cuối.** Nếu cài dạng service, runner chạy ở phiên nền của Windows,
> nơi không có màn hình thật — lệnh giả lập chuột/bàn phím sẽ không tới được app, test luôn đỏ.

4. Khởi động runner:

```powershell
.\run.cmd
```

Cửa sổ này **phải để mở** thì GitHub mới giao việc được. Thấy dòng `Listening for Jobs` là xong.

---

## Phần 3 — Khai báo tài khoản test (GitHub Secrets)

File `.env` không được đẩy lên git (chứa mật khẩu), nên phải khai riêng để CI dùng.

Vào repo → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**,
tạo 3 secret sau (lấy đúng giá trị trong file `.env` ở máy):

| Tên secret | Ví dụ giá trị |
|---|---|
| `JIM_APP_PATH` | `C:\Users\<tên-máy>\Desktop\JIM\Staging\Jim - STAG - ...\ConnectPOS.exe` |
| `JIM_USERNAME` | tài khoản đăng nhập app |
| `JIM_PASSWORD` | mật khẩu tài khoản đó |

GitHub che các giá trị này trong log, và workflow tự xoá file `.env` sau khi chạy xong.

---

## Phần 4 — Dùng hằng ngày

### Chạy tự động
Push code lên `main` hoặc mở PR có sửa trong `jim-app-automation/` → GitHub tự chạy.
Kết quả hiện ngay trong tab **Actions** và ở cuối PR (✅ xanh / ❌ đỏ).

### Chạy thủ công
Repo → tab **Actions** → chọn **JIM POS Desktop Regression** → **Run workflow**.

### Xem test đỏ vì lý do gì
Vào lần chạy đó → mục **Artifacts** → tải `test-reports-<tên test>` → giải nén.
Trong đó có ảnh chụp màn hình đúng thời điểm test đỏ, và dữ liệu để mở báo cáo Allure:

```powershell
.\mvnw.cmd allure:serve
```

### Chạy tay trên máy mình (không qua GitHub)

```powershell
.\mvnw.cmd -B test "-Dtest=CreateOrderTest" -DfailIfNoSpecifiedTests=false
```

```powershell
.\mvnw.cmd -B test "-Dtest=LoginTest,CreateOrderTest" -DfailIfNoSpecifiedTests=false
```

---

## Hạn chế đã biết

**1. Chạy hai test class trong cùng một lần Maven còn chập chờn.**
Đo thực tế: chạy chung 2 lần thì 1 lần xanh, 1 lần đỏ. Nguyên nhân nằm ở
`BaseTest.goToLoginScreen()` — sau khi dọn hộp thoại cập nhật, hàm này không kiểm tra lại xem app
còn ở màn Login không, nên đôi khi báo "đã về Login" trong khi màn hình đã đổi; test chạy tiếp
liền đỏ ngay ở bước đầu.

Workflow hiện đã **né** vấn đề này bằng cách cho mỗi test class chạy trong một job riêng (app khởi
động lại sạch cho từng class). Muốn chạy chung một lần cho nhanh thì phải sửa
`goToLoginScreen()` thành vòng lặp có kiểm tra lại sau mỗi bước.

**2. Toạ độ click cố định ở màn Receipt List.**
Bảng kết quả tìm kiếm không lộ ra cho công cụ tự động đọc được (hạn chế của Flutter), nên
`ReceiptListScreen` phải bấm theo toạ độ đo sẵn `(200, 235)`. Nếu app đổi bố cục màn này, phải
đo lại toạ độ đó.

**3. Máy tắt thì không tự chạy được.**
Runner chỉ nhận việc khi máy đang bật, đã đăng nhập và cửa sổ `run.cmd` đang mở. Muốn chạy đêm
(ví dụ 2h sáng) khi máy đã tắt thì cần thêm: BIOS bật *Power On by RTC Alarm* (hoặc để máy ở chế
độ Sleep thay vì tắt hẳn, rồi dùng Task Scheduler với tuỳ chọn *Wake the computer to run this
task*), cộng với bật tự động đăng nhập Windows. Chưa cấu hình trong phạm vi hiện tại.
