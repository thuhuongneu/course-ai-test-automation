---
description: Thực thi manual test cases trên browser thật qua Playwright MCP — chấm PASS/FAIL/BLOCKED/SKIPPED, thu evidence, xuất execution report. Dành cho tester manual, KHÔNG sinh code automation.
skills:
  - skills-manual-test-executor
  - skills-bug-reporter
---

> **BẮT BUỘC (MANDATORY SKILL):** Bạn PHẢI nạp và đọc kỹ nội dung của skill **`skills-manual-test-executor`** (tại `.claude/skills/skills-manual-test-executor/SKILL.md`) trước khi bắt đầu tác vụ này.

# Command: Thực Thi Manual Test Cases

Chạy bộ manual test cases trực tiếp trên browser thật, đối chiếu Expected vs Actual, chấm trạng thái và xuất báo cáo kết quả.

## ⚠️ Nguyên tắc

- **Chạy thật, không suy đoán** — mọi PASS/FAIL phải dựa trên quan sát UI qua `snapshot`
- **TC là chuẩn** — thực tế lệch TC thì ghi FAIL, **TUYỆT ĐỐI KHÔNG sửa TC** cho khớp
- **Headed mode, viewport theo `--viewport-size` lúc launch** (bảng ở `.claude/rules/playwright_rules.md` mục 1) — tester phải nhìn được agent đang thao tác, KHÔNG resize phóng vượt cửa sổ
- Tất cả output bằng **Tiếng Việt**

## Input cần thu thập

| Input | Bắt buộc? | Ghi chú |
|---|---|---|
| File TC hoặc checklist | ⭐ Bắt buộc | VD `docs/testcases/customers/parts/part_01_danh_sach.md` |
| Phạm vi chạy | ⭐ Bắt buộc | Toàn bộ · theo tag (`@Smoke`) · theo TC ID range · theo nhóm chức năng. **Không có → agent hỏi**, không tự chạy hết |
| URL & tài khoản | ⭐ Bắt buộc | Môi trường đang test + tài khoản đăng nhập |
| Môi trường dùng chung? | ⭐ Bắt buộc | Có → bật auto-skip TC phá huỷ; Không → chạy đầy đủ, ghi rõ trong report |
| Build / Version | Khuyến nghị | Để truy vết kết quả về đúng bản build |

## Các bước thực hiện

### Bước 1: Xác nhận phạm vi
1. Đọc file TC, lọc theo phạm vi user chọn
2. Đếm số TC sẽ chạy, ước tính thời gian
3. Công bố kế hoạch trước khi bắt đầu:
   ```
   Sẽ chạy: 36 TC (Part 1 — Danh sách khách hàng)
   Dự kiến SKIPPED: 2 TC (thao tác phá huỷ)
   Ước tính: ~60 phút
   ```

### Bước 2: Chuẩn bị
1. Tạo `run_id` dạng `run_<timestamp>`
2. Tạo thư mục evidence `docs/executions/<module>/<run_id>/evidence/`
3. Khởi tạo file report `docs/executions/<module>/<run_id>/execution_report.md` với phần header

### Bước 3: Khởi tạo browser & đăng nhập
1. `browser_navigate(url)`
2. **KHÔNG gọi `browser_resize`** — viewport đã đúng từ `--viewport-size` lúc launch MCP server
3. Chờ trang load xong → `browser_snapshot()`
4. Đăng nhập, verify đã vào được hệ thống
5. **Không đăng nhập được → DỪNG NGAY**, báo user, không chạy tiếp

### Bước 4: Chạy từng TC
Với mỗi TC theo thứ tự:
1. **Kiểm tra Auto-Skip** — TC có tag `@ManualOnly`/`Automatable = No` do rủi ro, hoặc steps chứa Mass Delete / Select All / xoá qua URL GET / upload `.exe` → `⏭️ SKIPPED` + ghi lý do, sang TC tiếp
2. **Dựng Pre-Condition** — không dựng được → `⚠️ BLOCKED` + ghi nguyên nhân
3. **Thực hiện đúng từng bước** trong Test Steps — không tự thêm/bớt bước
4. **Verify từng Expected** bằng `snapshot` sau mỗi bước có kết quả quan sát được
5. **Chấm trạng thái:**
   - Mọi Expected khớp → `✅ PASS`
   - Có Expected không khớp → `❌ FAIL` + **screenshot ngay tại bước fail** + ghi Actual vs Expected
   - Có Expected không verify được → `⚠️ BLOCKED` (KHÔNG phải PASS)
6. **Ghi kết quả vào report ngay** (không dồn tới cuối buổi)
7. **Khôi phục trạng thái sạch** (đóng modal, về trang danh sách) rồi chạy TC tiếp theo

### Bước 5: Báo tiến độ
Sau mỗi 5–10 TC, báo 1 dòng:
```
Đã chạy 12/36 — PASS 10 · FAIL 1 · BLOCKED 0 · SKIPPED 1
```

### Bước 6: Teardown
1. Xoá toàn bộ dữ liệu đã tạo trong buổi chạy
2. Đối chiếu số liệu hệ thống trước/sau để xác nhận môi trường trả về nguyên trạng
3. Bản nào không xoá được → ghi rõ vào report để người khác xử lý

### Bước 7: Xuất report
Xuất `execution_report.md` theo template trong skill, gồm 6 mục: Tổng kết · Kết quả từng TC · Chi tiết FAIL · TC BLOCKED · Dữ liệu đã tạo & dọn dẹp · Đề xuất bước tiếp theo.

## Điều kiện DỪNG giữa chừng

Agent chạy tiếp qua TC fail (fail-forward), **chỉ dừng** khi:

| Điều kiện | Vì sao |
|---|---|
| 3 TC liên tiếp FAIL cùng nguyên nhân hạ tầng | Đăng nhập hỏng / server 500 / mất mạng — chạy tiếp tạo báo cáo rác |
| Không đăng nhập được từ đầu | Toàn bộ suite không chạy được |
| Phát hiện đã ảnh hưởng dữ liệu người khác | Ưu tiên dừng và khắc phục |

## Quy tắc quan trọng

- **Pass rate không tính SKIPPED vào mẫu số** — báo cả 2 con số: tổng số TC và số TC thực chạy
- Screenshot **chỉ** khi FAIL hoặc milestone quan trọng — không chụp mọi bước
- Mọi dữ liệu tạo ra phải traceable (`Auto_<Module>_<timestamp>`) và có kế hoạch xoá
- KHÔNG `browser_navigate` lại nếu đã ở đúng trang — tránh reload làm mất trạng thái đang test

## Bước tiếp theo sau khi có report

| Tình huống | Làm gì |
|---|---|
| Có TC FAIL | `/create-bug-report` — sinh bug report chuẩn từ evidence đã thu |
| Nhiều TC FAIL cần gom nhóm root cause | `/analyze-test-report` |
| Cần đẩy kết quả lên Xray | `/import-test-results-xray` |
| TC FAIL do TC viết sai (không phải lỗi hệ thống) | `/review-testcases` mode FIX — **không sửa TC trong lúc chạy** |
