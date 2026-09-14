---
description: Tổng hợp kết quả kiểm thử toàn dự án tại một mốc (release/sprint/UAT) thành báo cáo gửi PM/khách — gộp nhiều execution report, bug đang mở, độ phủ RTM, kèm khuyến nghị go/no-go có căn cứ.
skills:
  - skills-test-summary-reporter
  - skills-test-report-analyzer
  - skills-bug-reporter
  - skills-coverage-traceability
---

# Workflow: Báo Cáo Tổng Hợp Kiểm Thử

> **BẮT BUỘC (MANDATORY SKILL):** Bạn PHẢI nạp và đọc kỹ **`skills-test-summary-reporter`** (tại `.claude/skills/skills-test-summary-reporter/SKILL.md`) — đặc biệt mục **Ranh giới nghề nghiệp** và **Tiêu chí Exit** — trước khi bắt đầu.

Workflow này trả lời đúng ba câu hỏi mà PM/khách hàng hỏi trước mỗi lần release: *đã test những gì · còn lỗi gì · có release được không.*

## ⚠️ Nguyên tắc thực thi

- **Tất cả output bằng Tiếng Việt**, viết cho người **không đọc code** — tránh thuật ngữ automation
- **QA khuyến nghị, PM quyết định** — không tự tuyên bố cấm/cho release
- **Chốt tiêu chí exit TRƯỚC khi xem kết quả** — nhìn số rồi mới đặt ngưỡng là hợp thức hoá kết quả
- **KHÔNG bịa số.** Thiếu dữ liệu thì ghi "không có dữ liệu", không ước lượng
- **KHÔNG đưa credentials, dữ liệu khách hàng thật** vào báo cáo — báo cáo này thường được gửi ra ngoài

---

## Bước 0: Chốt phạm vi & tiêu chí (CHECKPOINT — hỏi user)

| Cần chốt | Ghi chú |
|---|---|
| **Mốc báo cáo** | Release v2.4 · Sprint 12 · UAT đợt 2 — dùng làm tiêu đề |
| **Phạm vi module** | Module nào nằm trong đợt release này. ⚠️ Module **ngoài** phạm vi vẫn phải nêu ở mục 2 nếu chưa test, nhưng không tính vào tiêu chí #7 |
| **Build / Version** | Bắt buộc — báo cáo không gắn build là báo cáo không dùng được |
| **Tiêu chí exit của dự án** | Có thì dùng. **Không có → dùng bộ mặc định trong skill và GHI RÕ là mặc định của agent, cần PM xác nhận** |
| **Người nhận báo cáo** | PM nội bộ hay khách hàng — quyết định mức chi tiết kỹ thuật |

⏸️ **Chốt xong mới sang Bước 1.** Đây là bước duy nhất bắt buộc hỏi user; các bước sau agent tự chạy.

---

## Bước 1: Thu thập dữ liệu

Glob theo bảng Input của skill, **ghi lại đường dẫn từng file** để làm cột nguồn trong báo cáo:

1. `docs/executions/*/run_*/execution_report.md` — kết quả manual từng module
2. `docs/executions/*/retest_*/retest_report.md` — bug đã verify + regression phát sinh
3. `docs/bugs/README.md` (danh mục) rồi `docs/bugs/<module>/BUG_*.md` — đọc `Severity`, `Lịch sử retest` để biết bug nào còn mở
4. `docs/requirements/README.md` — **bắt buộc**, lấy module còn ⬜/🟨/⏸️
5. `traceability_matrix.md` — nếu có
6. `reports/` — kết quả automation, nếu có

**Xử lý dữ liệu thiếu:**

| Tình huống | Xử lý |
|---|---|
| Module trong phạm vi nhưng **không có execution report** | Ghi vào mục 2 "chưa được kiểm thử" — **KHÔNG** bỏ qua im lặng |
| Không có `docs/bugs/` (đội dùng Jira) | Lấy qua `skills-jira-integration`. Không lấy được → ghi rõ "không đối chiếu được tình hình bug", và **hạ mức tin cậy của khuyến nghị** |
| Không có `docs/requirements/README.md` | ⚠️ Không xác định được vùng mù → ghi rõ: *"chưa khảo sát cấp hệ thống, không kết luận được đã phủ hết hệ thống hay chưa"* |
| Nhiều run cùng một module | Lấy **run mới nhất** làm số chính; nêu trend ở phần ghi chú nếu có khác biệt đáng kể |

⚠️ **Số liệu mâu thuẫn giữa các nguồn → nêu ra cả hai**, không tự chọn số đẹp hơn.

---

## Bước 2: Đối chiếu tiêu chí Exit

Chấm **từng** tiêu chí đã chốt ở Bước 0: `✅ Đạt` / `❌ Không đạt` / `🟡 Ngoại lệ chờ duyệt` / `➖ Không áp dụng`, kèm **số thực tế**.

⛔ Không có mức "gần đạt". 94.8% với ngưỡng 95% là **không đạt** — có thể chấp nhận, nhưng phải ghi là ngoại lệ và nêu ai duyệt.

---

## Bước 3: Kết luận khuyến nghị

| Khuyến nghị | Khi nào | Bắt buộc kèm |
|---|---|---|
| 🟢 **GO** | Đạt toàn bộ tiêu chí | **Vẫn phải** liệt kê rủi ro tồn đọng (nợ kiểm thử, vùng chưa phủ) |
| 🟡 **GO có điều kiện** | Đạt phần lớn; phần thiếu có workaround hoặc nằm ngoài phạm vi người dùng | Danh sách điều kiện đánh số + rủi ro nếu vẫn release |
| 🔴 **NO-GO** | Còn bug Critical mở, hoặc luồng nghiệp vụ chính chưa chạy được | Nêu **đích danh** bug/vùng chặn và việc cần làm để gỡ |

**Câu chốt bắt buộc có trong mọi báo cáo:**

> ⚠️ Quyết định release thuộc về PM/PO. Báo cáo này cung cấp căn cứ, không thay thế quyết định đó.

---

## Bước 4: Xuất báo cáo

File: `docs/executions/test_summary_<mốc>_<timestamp>.md` — đặt ở **gốc `docs/executions/`** vì báo cáo này cắt ngang mọi module, không thuộc module nào.

Theo **Report Template** trong skill, đủ 9 mục theo đúng thứ tự:

| Mục | Vì sao ở vị trí đó |
|---|---|
| 1. Khuyến nghị | Người đọc bận — kết luận phải nằm ở dòng đầu |
| **2. Vùng chưa kiểm thử** | **Trước mọi tỷ lệ** — nếu không, con số đẹp che mất vùng trắng |
| 3. Đối chiếu tiêu chí exit | Cho thấy khuyến nghị dựa trên chuẩn, không phải cảm tính |
| 4. Kết quả theo module | Manual và automation **hai bảng riêng** |
| 5. Tình hình lỗi | Bug đang mở + regression phát sinh trong kỳ |
| 6. Độ phủ yêu cầu | Từ RTM, nếu có |
| 7. Đề xuất hành động | Việc cụ thể, ai làm, command nào |
| 8. Yếu tố cản trở & Bài học | Sau kết quả, trước phụ lục — đọc xong mới hiểu vì sao số ra như vậy |
| 9. Phụ lục — Ánh xạ chuẩn | Cuối cùng, dành cho người nghiệm thu/audit chứ không phải người đọc thường |

> **Mục 8 lấy dữ liệu từ đâu:** BLOCKED trong `execution_report.md` (lý do bị chặn), `retest_report.md` (bản fix nào gây regression), và mục Rủi ro của Master Test Plan (rủi ro nào đã thành hiện thực). **Không hỏi user** những gì có trong file — chỉ hỏi phần thời lượng nếu file không ghi.

> **Chuẩn tham chiếu:** báo cáo bám cấu trúc **ISO/IEC/IEEE 29119-3 — Test Completion Report**. Xem mục *Chuẩn tham chiếu* trong skill trước khi viết mục 9. Ghi **tên mục** chuẩn, **không** ghi số điều khoản; **không** tuyên bố "tuân thủ".

---

## Bàn giao

**Checklist trước khi báo xong:** dùng nguyên **Quality Checklist** của skill (13 mục). Bốn mục hay sai nhất:

- [ ] Vùng chưa test đặt **trước** tỷ lệ
- [ ] Khuyến nghị GO vẫn liệt kê rủi ro
- [ ] Manual và automation **không cộng gộp**
- [ ] Mục 8.1 có **thời lượng thực tế**, mục 8.2 mỗi bài học chỉ đích danh nơi áp dụng

**Báo cáo cho user:** khuyến nghị (GO / GO có điều kiện / NO-GO) · số tiêu chí đạt/tổng · bug Critical & Major đang mở · module chưa test · đường dẫn file báo cáo.

Nhắc user: file này gửi ra ngoài được — kiểm lại lần cuối không có URL nội bộ, tài khoản, hay dữ liệu khách hàng thật trong ảnh đính kèm.

---

## Mối quan hệ với workflows khác

```
/execute-test-cases ──┐
/retest-fixed-bugs  ──┤
/create-bug-report  ──┼──→  /generate-test-summary-report  ──→  PM / PO / khách hàng
/generate-traceability-matrix ──┘                │
                                                 └──→ mở scripts/execution-viewer/bundle.html để xem trực quan
```

| Nhầm lẫn thường gặp | Workflow đúng |
|---|---|
| *"Phân tích giúp lần chạy vừa rồi fail vì sao"* | `/analyze-test-report` — một phạm vi chạy |
| *"Tổng hợp cho tôi báo cáo gửi khách"* | **Workflow này** — toàn dự án tại một mốc |
| *"Requirement nào chưa có test"* | `/generate-traceability-matrix` |
