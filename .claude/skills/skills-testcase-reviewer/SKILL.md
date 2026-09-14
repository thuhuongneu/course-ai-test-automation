---
name: skills-testcase-reviewer
description: Skill review chất lượng manual test cases có sẵn — phát hiện TC mơ hồ, thiếu assertion, trùng lặp, thiếu negative/boundary case, chấm điểm theo rubric và đề xuất cải thiện cụ thể.
---

# Test Case Reviewer

Purpose: Đánh giá chất lượng bộ manual test cases (do người khác hoặc AI viết) và đề xuất cải thiện cụ thể — KHÔNG viết lại toàn bộ từ đầu.

---

## When to Use

Sử dụng skill này khi:

- User đưa file test cases (Excel/Markdown/CSV) và yêu cầu "review", "đánh giá", "check chất lượng"
- Trước khi convert manual TC sang automation (đảm bảo TC đủ tốt để automate)
- Sau khi sinh TC bằng `skills-rbt-manual-testing` và cần một vòng kiểm tra độc lập
- Onboard bộ TC cũ từ dự án khác

**KHÔNG** sử dụng skill này khi:

- **Requirements vừa đổi theo ticket, cần đồng bộ TC** → dùng `skills-rbt-manual-testing` **Mode DELTA** qua command `/update-testcases-from-impact`

> 🚨 **Phân biệt rõ:** skill này chấm **chất lượng cách viết** theo rubric — một TC mô tả rất tốt về hành vi **đã bị thay đổi** vẫn đạt 12/12 điểm, vì rubric không đối chiếu với REQ mới. Nó **không bắt được TC stale**.
>
> Ngoài ra Mode FIX của skill này sinh file `<tên>_improved.md` — **lạc tên index** `test_cases_<module>.md` mà các workflow phía sau đọc theo mẫu. Dùng nó để cập nhật theo ticket là vừa bỏ sót TC stale, vừa để lại hai bộ TC không ai biết bộ nào đang dùng.
>
> Thứ tự đúng: `/update-testcases-from-impact` đồng bộ nội dung **trước** → skill này chấm chất lượng **sau**.

---

## Review Rubric (6 tiêu chí)

Mỗi test case chấm theo 6 tiêu chí, thang điểm 0-2 (0 = không đạt, 1 = một phần, 2 = đạt):

| # | Tiêu chí | Câu hỏi kiểm tra |
|---|---|---|
| 1 | **Rõ ràng (Clarity)** | Người chưa biết feature đọc có thực hiện được không? Steps có đánh số, mỗi step 1 hành động? |
| 2 | **Expected Result đo được** | Expected có cụ thể, verify được không? (❌ "hoạt động đúng" / ✅ "hiển thị message X, redirect về /dashboard") |
| 3 | **Độc lập (Independence)** | TC có tự chuẩn bị precondition không, hay phụ thuộc TC khác chạy trước? |
| 4 | **Test data cụ thể** | Data có được chỉ định rõ không? Field unique có ghi chú cần random không? |
| 5 | **Truy vết được (Traceability)** | TC có link về requirement/user story không? |
| 6 | **Đúng trọng tâm (Focus)** | 1 TC verify 1 mục tiêu, không gộp nhiều kịch bản vào 1 TC |

**Xếp loại theo tổng điểm (tối đa 12):**
- 🟢 **10-12:** Tốt — dùng được ngay
- 🟡 **6-9:** Cần sửa — có đề xuất cụ thể
- 🔴 **0-5:** Viết lại — chỉ ra hướng viết lại

---

## Coverage Analysis (mức bộ TC)

Ngoài review từng TC, đánh giá độ phủ của cả bộ:

| Khía cạnh | Kiểm tra |
|---|---|
| **Happy path** | Các luồng chính đã có TC chưa? |
| **Negative cases** | Input sai, quyền không đủ, hành động không hợp lệ |
| **Boundary values** | Min/max length, 0, giá trị biên, ký tự đặc biệt, Unicode |
| **State/Edge cases** | Data rỗng, danh sách dài, session hết hạn, double-click/double-submit |
| **Trùng lặp** | Các TC verify cùng 1 thứ → đề xuất merge |
| **Ưu tiên** | TC có gán priority (High/Medium/Low) chưa? Có hợp lý với risk không? |

---

## Review Workflow

1. **Đọc input** — file TC + requirement liên quan (nếu có). Nếu thiếu requirement → vẫn review được 5/6 tiêu chí, ghi chú không đánh giá được traceability
2. **Review từng TC** — chấm điểm 6 tiêu chí, ghi vấn đề cụ thể (trích nguyên văn chỗ chưa đạt)
3. **Phân tích coverage** — đối chiếu bộ TC với requirement, liệt kê gap
4. **Phát hiện trùng lặp** — nhóm TC giống nhau
5. **Report** — xuất báo cáo theo template, kèm đề xuất sửa cụ thể cho từng TC 🔴/🟡

> **Nguyên tắc:** Mọi nhận xét phải kèm **ví dụ sửa cụ thể**, không chê chung chung. VD: thay vì "Expected mơ hồ" → viết "Expected hiện tại: 'hệ thống xử lý đúng' → Đề xuất: 'Toast hiển thị "Lưu thành công", record xuất hiện đầu danh sách với tên vừa nhập'".

---

## Report Template

```markdown
# Báo Cáo Review Test Cases

## Tổng quan
- **Nguồn:** <file path>
- **Số TC review:** N
- **Kết quả:** 🟢 x tốt | 🟡 y cần sửa | 🔴 z nên viết lại
- **Điểm trung bình:** x.x/12

## Chi tiết từng TC
| TC ID | Điểm | Xếp loại | Vấn đề chính | Đề xuất sửa |
|---|---|---|---|---|
| TC_01 | 11/12 | 🟢 | Thiếu ghi chú data random | Thêm "email = random unique" |
| TC_02 | 7/12 | 🟡 | Expected mơ hồ; gộp 2 kịch bản | Tách thành TC_02a/02b; Expected: "..." |

## Coverage Gaps (TC còn thiếu)
| # | Kịch bản thiếu | Loại | Priority đề xuất |
|---|---|---|---|
| 1 | Login với account bị khóa | Negative | High |

## TC trùng lặp — đề xuất merge
- TC_05 ≈ TC_12 (cùng verify validation email) → giữ TC_05, bỏ TC_12

## Kết luận & Khuyến nghị
- <Tóm tắt 3-5 hành động ưu tiên>
```

---

## Quality Checklist

- [ ] Mỗi TC 🔴/🟡 đều có đề xuất sửa cụ thể (không chê chung chung)
- [ ] Coverage gap liệt kê kịch bản cụ thể, không nói "thiếu negative case" suông
- [ ] Không tự ý sửa file TC gốc — chỉ báo cáo, trừ khi user yêu cầu sửa
- [ ] Nếu user yêu cầu sửa → sinh phiên bản mới, giữ nguyên file gốc

---

## Rules References

- `.claude/skills/skills-rbt-manual-testing/SKILL.md` — Chuẩn viết TC (để đối chiếu khi review)
- `.claude/skills/skills-coverage-traceability/SKILL.md` — Truy vết TC ↔ requirements sâu hơn
