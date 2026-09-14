---
name: skills-manual-test-executor
description: Skill thực thi manual test cases trực tiếp trên browser thật qua Playwright MCP — chạy từng bước theo TC, đối chiếu Expected vs Actual, chấm PASS/FAIL/BLOCKED/SKIPPED, thu evidence và xuất execution report. Dành cho tester manual, KHÔNG sinh code automation.
---

# Manual Test Executor

## Description

Skill biến bộ manual test cases thành **kết quả kiểm thử thật**. Agent mở browser, chạy đúng từng bước ghi trong TC, quan sát UI thực tế, đối chiếu với Expected Result, chấm trạng thái và xuất báo cáo.

**Dành cho tester manual thuần** — không cần biết code, không sinh script automation.

**Nguyên tắc cốt lõi:**
- **Chạy thật, không suy đoán:** mọi kết luận PASS/FAIL phải dựa trên quan sát UI thực tế qua snapshot/screenshot
- **TC là chuẩn, không phải hệ thống:** khi thực tế lệch TC → ghi FAIL. **TUYỆT ĐỐI KHÔNG sửa TC cho khớp thực tế**
- **An toàn dữ liệu trước tốc độ:** môi trường dùng chung thì thao tác phá huỷ bị chặn, dữ liệu tạo ra phải dọn

---

## When to Use

Sử dụng skill này khi:

- Đã có bộ manual test cases (từ `skills-rbt-manual-testing`) và cần **chạy kiểm chứng thật**
- Cần chạy smoke/regression bằng tay trước release
- Cần thực thi checklist (từ CHECKLIST mode) trên môi trường thật
- Cần báo cáo kết quả PASS/FAIL có evidence cho PO/QA lead
- Cần kiểm chứng nhanh một nhóm TC sau khi dev fix bug

**KHÔNG** sử dụng skill này khi:

- Cần sinh code automation → `skills-qa-automation-engineer`
- Cần phân tích report **đã có sẵn** từ automation (Playwright/Allure/JUnit) → `skills-test-report-analyzer`
- Chỉ cần inspect DOM / lấy locator → `skills-ui-debug-agent`
- Chưa có TC, cần sinh TC trước → `skills-rbt-manual-testing`

---

## Vị trí trong chuỗi công việc

```
skills-rbt-manual-testing          skills-manual-test-executor          skills-bug-reporter
  test_cases_*.md          ──→      execution_report.md         ──→      bug_*.md (mỗi TC FAIL)
  checklist_*.md                            │
                                            └──→  skills-test-report-analyzer
                                                  (khi có nhiều FAIL cần gom nhóm root cause)
```

---

## Input cần thu thập

| Input | Bắt buộc? | Ghi chú |
|---|---|---|
| File TC hoặc checklist | ⭐ Bắt buộc | VD `docs/testcases/customers/parts/part_01_danh_sach.md` |
| Phạm vi chạy | ⭐ Bắt buộc | Toàn bộ file · theo tag (`@Smoke`) · theo TC ID range · theo nhóm chức năng |
| Môi trường & tài khoản | ⭐ Bắt buộc | URL, tài khoản đăng nhập, build/version đang test |
| Mức độ dùng chung của môi trường | ⭐ Bắt buộc | Dùng chung → bật auto-skip TC phá huỷ; môi trường riêng → có thể chạy đầy đủ |

> Thiếu phạm vi chạy → agent **hỏi** thay vì mặc định chạy toàn bộ (bộ TC có thể tới hàng trăm TC).

---

## Thứ tự thao tác browser (BẮT BUỘC)

Tuân thủ Browser Rules của dự án:

```
navigate → wait_for(page_load) → snapshot → interact → screenshot(on_fail)
```

- **Headed mode bắt buộc** — tester phải nhìn được agent đang làm gì
- **KHÔNG** gọi `browser_resize` — viewport đã đúng từ `--viewport-size` lúc launch MCP server. Phóng viewport vượt cửa sổ OS làm trang bị cắt bên phải, tester mất tầm nhìn (xem [`.claude/rules/playwright_rules.md`](../../rules/playwright_rules.md) mục 1)
- **KHÔNG** gọi `browser_navigate` lại nếu đã ở đúng trang — tránh reload làm mất trạng thái đang test
- Dùng `snapshot` để đọc trạng thái UI và verify; `screenshot` để lưu bằng chứng

---

## 4 Trạng thái kết quả

| Trạng thái | Khi nào | Bắt buộc kèm |
|---|---|---|
| ✅ **PASS** | **Mọi** Expected Result đánh số đều đã verify khớp thực tế | — |
| ❌ **FAIL** | Ít nhất 1 Expected Result không khớp thực tế | Screenshot tại bước fail + ghi rõ Actual vs Expected |
| ⚠️ **BLOCKED** | Không chạy được vì lý do **ngoài phạm vi TC**: pre-condition không dựng được, môi trường lỗi, TC phụ thuộc kết quả TC khác đã fail, hoặc có Expected không quan sát được | Ghi rõ nguyên nhân chặn |
| ⏭️ **SKIPPED** | Cố ý bỏ qua: TC thao tác phá huỷ trên môi trường dùng chung, TC ngoài phạm vi chạy đã chọn | Ghi rõ lý do skip |

> **Quy tắc vàng:** không verify được một Expected → TC là **BLOCKED**, KHÔNG phải PASS. Cấm suy đoán "chắc là đúng".

---

## Quy tắc Auto-Skip TC nguy hiểm (môi trường dùng chung)

Agent **tự động chuyển SKIPPED** và **KHÔNG thực thi** khi TC có bất kỳ dấu hiệu sau:

| Dấu hiệu | Ví dụ |
|---|---|
| Tag `@ManualOnly` hoặc `Automatable = No` do rủi ro dữ liệu | TC_030 (Bulk Actions), TC_132 (xoá qua URL GET) |
| Steps chứa thao tác xoá hàng loạt | `Mass Delete`, `Select All`, chọn nhiều bản ghi rồi xoá |
| Steps gọi trực tiếp URL phá huỷ | `/admin/clients/delete/{id}` trên thanh địa chỉ |
| Steps tải lên tệp thực thi | `.exe`, `.sh`, `.bat` |
| Steps sinh dữ liệu cực lớn | chuỗi 255+ ký tự, import hàng trăm dòng |

Ghi vào report: `⏭️ SKIPPED — thao tác phá huỷ trên môi trường dùng chung, cần chạy tay có giám sát`.

> Môi trường **test riêng** → tester khai báo lúc bắt đầu, agent tắt quy tắc này và ghi rõ trong header report.

---

## Quy tắc Fail-Forward

Khi một TC FAIL:

1. **Chụp screenshot ngay tại bước fail** (không chụp sau khi đã điều hướng đi nơi khác)
2. Ghi lại: bước số mấy, Expected là gì, Actual quan sát được là gì
3. Khôi phục trạng thái sạch (đóng modal, quay về trang danh sách) rồi **chạy tiếp TC sau**
4. **KHÔNG** dừng cả buổi chạy chỉ vì 1 TC fail

**Ngoại lệ — dừng và báo user ngay:**

| Điều kiện dừng | Vì sao |
|---|---|
| 3 TC liên tiếp FAIL cùng một nguyên nhân hạ tầng | Đăng nhập hỏng, server 500, mất mạng — chạy tiếp chỉ tạo báo cáo rác |
| Không đăng nhập được ngay từ đầu | Toàn bộ suite không chạy được |
| Phát hiện thao tác của mình đã ảnh hưởng dữ liệu người khác | Ưu tiên dừng và khắc phục |

---

## Quản lý Test Data & Cleanup (BẮT BUỘC)

- Mọi dữ liệu tạo ra phải **traceable**: `Auto_<Module>_<timestamp>`, `auto_<loại>_<timestamp>@auto.test`
- Agent **ghi sổ** mọi bản ghi đã tạo trong lúc chạy (tên + ID + nơi tạo)
- Cuối buổi chạy: **dọn sạch** toàn bộ dữ liệu đã tạo
- Report **bắt buộc** có mục "Dữ liệu đã tạo & dọn dẹp" — bản nào chưa xoá được phải ghi rõ để người khác xử lý

> Không bao giờ tạo dữ liệu mà không có kế hoạch xoá tương ứng.

---

## Evidence (Screenshot)

| Tình huống | Chụp? |
|---|---|
| TC FAIL | ✅ **Bắt buộc** — chụp ngay tại bước fail |
| Milestone quan trọng (tạo bản ghi thành công, hoàn tất luồng chính) | ✅ Nên chụp |
| Mỗi bước của mọi TC | ❌ **KHÔNG** — chụp tràn lan làm report nặng và khó đọc |

Lưu tại: `docs/executions/<module>/<run_id>/evidence/<TC_ID>_<mô_tả>.png`

---

## Quy trình thực thi

1. **Xác nhận phạm vi & môi trường** — đọc file TC, lọc TC theo phạm vi user chọn, đếm số TC sẽ chạy, công bố kế hoạch
2. **Chuẩn bị:** tạo `run_id` (`run_<timestamp>`), tạo thư mục evidence, khởi tạo file report
3. **Đăng nhập & khởi tạo môi trường:** navigate → wait_for(page_load) → đăng nhập → verify vào được hệ thống. Không vào được → dừng ngay, báo user
4. **Chạy từng TC theo thứ tự:**
   - Đọc Pre-Condition → dựng điều kiện; không dựng được → `BLOCKED`
   - Kiểm tra quy tắc Auto-Skip → khớp thì `SKIPPED`, sang TC tiếp
   - Thực hiện **đúng từng bước** trong Test Steps, không tự thêm/bớt bước
   - Sau mỗi bước có Expected tương ứng: `snapshot` để verify
   - Đối chiếu **toàn bộ** Expected Result → chấm trạng thái
   - FAIL → screenshot + ghi Actual → khôi phục trạng thái → tiếp
   - Ghi kết quả vào report **ngay sau mỗi TC** (không dồn tới cuối — mất điện là mất hết)
5. **Cập nhật tiến độ** sau mỗi 5–10 TC: `Đã chạy 12/36 — PASS 10 · FAIL 1 · BLOCKED 0 · SKIPPED 1`
6. **Teardown:** xoá toàn bộ dữ liệu đã tạo, đối chiếu số liệu hệ thống trước/sau
7. **Xuất execution report** hoàn chỉnh + đề xuất bước tiếp theo

---

## Output — Execution Report

File: `docs/executions/<module>/<run_id>/execution_report.md`

```markdown
# Execution Report — <Module> · <Loại chạy>

| Thông tin | Nội dung |
|---|---|
| Run ID | run_1785700456 |
| Nguồn TC | docs/testcases/customers/parts/part_01_danh_sach.md |
| Phạm vi | 36 TC (toàn bộ Part 1) |
| Môi trường | `<URL môi trường test>` — `<Staging/UAT/Production>` |
| Build / Version | (nếu có) |
| Tài khoản | `<tài khoản test>` (`<role>`) |
| Người thực hiện | <tên tester> (agent hỗ trợ) |
| Bắt đầu → Kết thúc | 2026-08-02 09:15 → 10:40 (85 phút) |
| Môi trường dùng chung? | Có — auto-skip TC phá huỷ đang BẬT |

## 1. Tổng kết

| Trạng thái | Số lượng | Tỷ lệ |
|---|---|---|
| ✅ PASS | 30 | 83.3% |
| ❌ FAIL | 3 | 8.3% |
| ⚠️ BLOCKED | 1 | 2.8% |
| ⏭️ SKIPPED | 2 | 5.6% |
| **Tổng** | **36** | 100% |

> **Pass rate (không tính SKIPPED):** 30/34 = 88.2%

## 2. Kết quả từng TC

| TC ID | Test Scenario | Kết quả | Bước fail | Ghi chú |
|---|---|---|---|---|
| CRM_CUST_TC_001 | Bảng hiển thị đủ 9 cột | ✅ PASS | — | — |
| CRM_CUST_TC_011 | Search chuỗi SQL injection | ❌ FAIL | 4 | Xem chi tiết #1 |
| CRM_CUST_TC_030 | Modal Bulk Actions | ⏭️ SKIPPED | — | Thao tác phá huỷ, môi trường dùng chung |

## 3. Chi tiết TC FAIL

### FAIL #1 — CRM_CUST_TC_011 · Search chuỗi SQL injection

| | |
|---|---|
| REQ ID | REQ-CUST-04 |
| Priority | High |
| Bước fail | Bước 4 |
| **Expected** | Request trả HTTP 200; bảng hiển thị 0 kết quả |
| **Actual** | Request trả HTTP 500; trang hiện thông báo lỗi cơ sở dữ liệu |
| Evidence | ![](evidence/CRM_CUST_TC_011_step4_error500.png) |
| Tái hiện được? | Có — thử lại 2 lần đều lỗi |

## 4. TC BLOCKED

| TC ID | Nguyên nhân chặn | Cần gì để chạy được |
|---|---|---|

## 5. Dữ liệu đã tạo & dọn dẹp

| Dữ liệu | ID | Nơi tạo | Đã xoá? |
|---|---|---|---|
| Auto_Customer_1785700456 | 12130 | /admin/clients | ✅ |

> Đối chiếu số liệu hệ thống: `Total Customers` trước 1431 → sau 1431 ✅ môi trường trả về nguyên trạng.

## 6. Đề xuất bước tiếp theo

- FAIL #1 (High) → sinh bug report bằng `/create-bug-report`
- 2 TC SKIPPED cần chạy tay có giám sát trên môi trường riêng
```

---

## Anti-Patterns (NGHIÊM CẤM)

- ❌ Chấm PASS khi **chưa verify hết** các Expected Result đánh số
- ❌ Suy đoán kết quả ("chắc là đúng") mà không snapshot quan sát thực tế
- ❌ **Sửa nội dung TC cho khớp hành vi thực tế** — lệch thì ghi FAIL, TC chỉ được sửa qua `/review-testcases`
- ❌ Tự thêm/bớt bước so với Test Steps rồi vẫn chấm PASS
- ❌ Thực thi TC có thao tác phá huỷ trên môi trường dùng chung
- ❌ Tạo dữ liệu test mà không dọn, hoặc dọn nhầm dữ liệu người khác
- ❌ Dừng cả buổi chạy chỉ vì 1 TC fail (trừ 3 điều kiện dừng đã nêu)
- ❌ Chụp screenshot mọi bước của mọi TC → report nặng, khó đọc
- ❌ Dồn ghi kết quả tới cuối buổi thay vì ghi ngay sau mỗi TC
- ❌ Báo cáo pass rate mà tính cả SKIPPED vào mẫu số (làm đẹp số liệu sai lệch)
- ❌ Bỏ qua mục "Dữ liệu đã tạo & dọn dẹp" trong report
