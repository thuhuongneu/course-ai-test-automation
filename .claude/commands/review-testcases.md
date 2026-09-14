---
description: Review chất lượng manual test cases — chấm điểm theo rubric 6 tiêu chí, phát hiện TC mơ hồ/trùng lặp, chỉ ra coverage gaps. Hỗ trợ 2 mode — REVIEW (chỉ báo cáo) và FIX (báo cáo + sửa TC).
skills:
  - skills-testcase-reviewer
  - skills-rbt-manual-testing
---

# Workflow: Review Manual Test Cases

> **BẮT BUỘC (MANDATORY SKILL):** Bạn PHẢI nạp và đọc kỹ nội dung của skill **`skills-testcase-reviewer`** (tại `.claude/skills/skills-testcase-reviewer/SKILL.md`) trước khi bắt đầu.

> 🚨 **KHÔNG dùng workflow này để cập nhật TC theo ticket đã đổi requirements** → dùng `/update-testcases-from-impact`.
>
> Workflow này chấm **chất lượng cách viết** theo rubric 6 tiêu chí. Một TC mô tả rất tốt về hành vi **đã bị thay đổi** vẫn đạt 12/12 điểm — rubric không đối chiếu với REQ mới, nên **không bắt được TC stale**. Mode FIX lại sinh file `_improved` lạc tên index mà các workflow sau đọc theo mẫu.
>
> Thứ tự đúng: `/update-testcases-from-impact` đồng bộ nội dung **trước** → workflow này chấm chất lượng **sau**.

## ⚠️ Nguyên tắc thực thi

- **Tất cả output bằng Tiếng Việt**
- **KHÔNG tự ý sửa file TC gốc** — Mode REVIEW chỉ báo cáo; Mode FIX sinh file mới, giữ nguyên file gốc
- Mọi nhận xét phải kèm **ví dụ sửa cụ thể** — không chê chung chung
- Nếu thiếu requirements → vẫn review 5/6 tiêu chí, ghi chú rõ không đánh giá được traceability

## 2 Chế độ (Mode)

| Mode | Khi nào sử dụng | Output |
|---|---|---|
| **REVIEW** (mặc định) | User cần đánh giá chất lượng bộ TC | Báo cáo review + đề xuất sửa |
| **FIX** | User muốn agent sửa luôn các TC 🔴/🟡 | Như REVIEW + file TC phiên bản cải thiện |

> Nếu user nói "sửa luôn", "cải thiện giùm", "viết lại giúp" → tự động chuyển sang **Mode FIX**.

## Input cần thu thập

| Input | Bắt buộc? |
|---|---|
| File test cases (Markdown/Excel/CSV) | ⭐ Bắt buộc |
| Requirements/user stories liên quan | Khuyến nghị — để đánh giá traceability + coverage gaps |

## Các bước thực hiện

### Bước 1: Đọc Input
1. Đọc file TC, xác định format và số lượng
2. Đọc requirements (nếu có)

### Bước 2: Review Từng TC
1. Chấm điểm 6 tiêu chí (0-2 mỗi tiêu chí, tối đa 12) theo rubric trong skill
2. Xếp loại: 🟢 (10-12) / 🟡 (6-9) / 🔴 (0-5)
3. Với mỗi TC 🔴/🟡: trích nguyên văn chỗ chưa đạt + viết đề xuất sửa cụ thể

### Bước 3: Phân Tích Mức Bộ TC
1. Coverage gaps: happy path / negative / boundary / edge cases còn thiếu (liệt kê kịch bản cụ thể)
2. TC trùng lặp → đề xuất merge
3. Kiểm tra priority có hợp lý không

### Bước 4: Báo Cáo (CHECKPOINT)
1. Xuất `testcase_review_report.md` theo template trong skill
2. **⏸️ DỪNG LẠI** — trình bày kết quả. Mode REVIEW → KẾT THÚC. Mode FIX → hỏi user xác nhận danh sách TC sẽ sửa

### Bước 5: Sửa TC (Mode FIX — chỉ khi user xác nhận)
1. Sinh file mới `<tên_file_gốc>_improved.<ext>` với các TC đã cải thiện
2. TC 🟢 giữ nguyên; TC 🟡 sửa theo đề xuất; TC 🔴 viết lại (tuân thủ chuẩn `skills-rbt-manual-testing`)
3. Bổ sung TC mới cho coverage gaps ưu tiên High (nếu user đồng ý)

## Output

### Mode REVIEW
- File `testcase_review_report.md`: điểm từng TC, vấn đề + đề xuất sửa, coverage gaps, TC trùng lặp, khuyến nghị

### Mode FIX
- Tất cả output của Mode REVIEW, cộng thêm:
  - File TC phiên bản cải thiện (file gốc giữ nguyên)
  - Danh sách TC mới bổ sung cho gaps (nếu có)
