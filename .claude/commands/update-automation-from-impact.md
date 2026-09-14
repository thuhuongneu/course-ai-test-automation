---
description: Cập nhật automation script đã có theo Impact Report (chế độ delta) — map TC thay đổi sang script tương ứng, chỉ sửa đúng phần đổi, KHÔNG sinh lại cả module. Hỗ trợ 2 mode — PLAN (chỉ lập kế hoạch) và APPLY (sửa + chạy lại).
skills:
  - skills-coverage-traceability
  - skills-qa-automation-engineer
  - skills-ui-debug-agent
  - skills-smart-locator-agent
---

# Workflow: Cập Nhật Automation Theo Impact Report

> **BẮT BUỘC (MANDATORY SKILLS):** Nạp và đọc kỹ trước khi bắt đầu:
> - **`skills-coverage-traceability`** (`.claude/skills/skills-coverage-traceability/SKILL.md`) — Mapping Rules để nối TC ID ↔ script
> - **`skills-qa-automation-engineer`** (`.claude/skills/skills-qa-automation-engineer/SKILL.md`) — quy tắc automation chung
> - **`skills-ui-debug-agent`** (`.claude/skills/skills-ui-debug-agent/SKILL.md`) — inspect DOM khi thay đổi kéo theo UI mới

Mắt xích **cuối** của chuỗi delta 3 tầng: requirements đổi → TC được đồng bộ → **workflow này** chỉ ra script nào phải sửa và sửa đúng chỗ đó.

```
/update-requirements-from-ticket   → Impact Report
        ↓
/update-testcases-from-impact      → Delta TC List (kỳ vọng MỚI đã nằm trong TC)
        ↓
/update-automation-from-impact     ← WORKFLOW NÀY
```

> ⚠️ **Chạy workflow này khi TC chưa được đồng bộ là sửa script theo kỳ vọng cũ.** File test cases là nguồn sự thật của kỳ vọng mới — TC còn mô tả hành vi cũ thì script sửa xong vẫn sai. Chưa chạy `/update-testcases-from-impact` thì làm nó trước.

## Workflow này khác gì `generate-automation-from-testcases`?

| | Workflow này (delta) | `generate-automation-from-testcases` (sinh mới) |
|---|---|---|
| **Input** | Impact Report / danh sách TC đã đổi | Bộ TC đầy đủ |
| **Với script đã có** | **Sửa đúng phần đổi**, giữ nguyên phần còn lại | Sinh lại từ đầu |
| **Phạm vi chạm** | Chỉ test/POM liên quan TC đổi | Cả module |
| **Dùng khi** | TC_005 đổi expected result | Module chưa có automation |

> TC **mới hoàn toàn** (➕ Viết mới trong Impact Report) **không** thuộc workflow này — route sang `/generate-automation-from-testcases`. Trộn hai việc vào một là mất lý do tồn tại của delta mode.

## ⚠️ Nguyên tắc thực thi

- **Tất cả output bằng Tiếng Việt**
- 🚨 **CẤM sinh lại cả file/module.** Chỉ sửa đúng dòng liên quan TC đã đổi. Sinh lại là xoá sạch công sửa tay và các fix đã tích luỹ
- 🚨 **CẤM xoá file script** cho TC bị archive — theo `automation_rules.md` mục 4, đánh dấu skip + báo user, chờ xác nhận mới gỡ
- **KHÔNG bịa mapping TC ↔ script.** Không map được thì ghi vào mục "cần xác nhận", KHÔNG đoán rồi sửa nhầm file
- **KHÔNG đoán locator** khi thay đổi kéo theo field/màn hình mới — phải inspect DOM thực tế
- **Giữ nguyên TC ID** trong Allure label sau khi sửa — đứt TC ID là vỡ RTM
- ⚠️ Sau khi user duyệt kế hoạch → agent tự sửa + chạy lại, KHÔNG hỏi lại giữa chừng

## 2 Chế độ (Mode)

| Mode | Khi nào sử dụng | Output |
|---|---|---|
| **PLAN** (mặc định) | Cần biết thay đổi này đụng tới script nào, tốn bao nhiêu | Bảng ánh xạ + kế hoạch sửa từng script |
| **APPLY** | Muốn agent sửa luôn | Như PLAN + code đã sửa + kết quả chạy lại |

> User nói "sửa luôn", "cập nhật script đi", "apply" → tự động **Mode APPLY**.

## Input cần thu thập

| Input | Bắt buộc? | Ghi chú |
|---|---|---|
| **Impact Report** | ⭐ Bắt buộc (hoặc thay bằng danh sách TC dưới) | Từ `/update-requirements-from-ticket` — dán trực tiếp hoặc file path |
| **Danh sách TC đã đổi** | Thay thế cho Impact Report | Khi user tự biết TC nào đổi: TC ID + đổi cái gì |
| **File test cases hiện hành** | ⭐ Bắt buộc | Để đọc nội dung TC **sau khi đổi** — nguồn sự thật của kỳ vọng mới |
| **Thư mục automation** | ⭐ Bắt buộc | VD: `src/tests/` + `src/pages/` |
| **RTM** | ⭕ Khuyến nghị | Có sẵn thì map nhanh và chắc hơn nhiều |

> Impact Report ghi `⚠️ chưa rà soát` ở cột TC → dừng, báo user chạy `/generate-traceability-matrix` trước. Không có nguồn map thì workflow này chỉ đoán mò.

## Các bước thực hiện

### Bước 1: Đọc Delta

1. Đọc Impact Report, trích bảng **Test case cần xử lý** — 3 nhóm: `⚠️ Review & sửa` / `🗑️ Archive` / `➕ Viết mới`
2. Với mỗi TC ở nhóm `⚠️`: đọc **nội dung TC hiện hành** trong file test cases để biết kỳ vọng **mới** là gì
3. Ghi rõ **đổi cái gì**: expected result / steps / test data / precondition / bị gỡ

> Không có Impact Report mà user tự liệt kê TC → vẫn chạy được, nhưng phải hỏi rõ từng TC đổi ở điểm nào. Không tự suy từ tên TC.

### Bước 2: Map TC → Script

Áp dụng Mapping Rules của `skills-coverage-traceability`, theo thứ tự tin cậy:

| Cách map | Độ tin cậy | Dấu hiệu |
|---|---|---|
| Allure label `testId` | ✅ Chắc chắn | `allure.label('testId', 'CRM_LOGIN_TC_001')` |
| TC ID trong tên test / annotation | ✅ Chắc chắn | `test('CRM_LOGIN_TC_001 - đăng nhập...')`, `@Test(description = "...")` |
| So khớp mô tả TC ↔ tên test | ⚠️ Suy luận | Cần user xác nhận trước khi sửa |
| Không tìm thấy | ❓ | TC chưa được automate — ghi nhận, KHÔNG sửa gì |

Mỗi TC ghi lại: file test, dòng test block, và **Page Object nào bị kéo theo**.

### Bước 3: Lập Kế Hoạch Sửa Từng Script

Phân loại hành động cho automation — **khác** với hành động dành cho TC:

| Delta của TC | Hành động với script | Phạm vi chạm |
|---|---|---|
| Đổi **expected result** | Sửa assertion | Chỉ dòng assert trong test |
| Đổi / thêm **steps** | Sửa method trong Page Object | POM + có thể thêm locator |
| Đổi **test data / validation** | Sửa data generator hoặc input trong test | Utils + test |
| Đổi **precondition** | Sửa setup/fixture | `beforeEach` / `@BeforeMethod` |
| Field/màn hình **mới** | Thêm locator (**inspect DOM thật**) + method POM | POM |
| TC bị **archive** | Đánh dấu skip + ghi lý do, **KHÔNG xoá file** | Test block |
| TC **mới** | ❌ Ngoài phạm vi → `/generate-automation-from-testcases` | — |

Mỗi mục ghi: `file:line`, đổi gì, có cần mở browser inspect không.

### Bước 4: Báo Cáo & Xin Duyệt (CHECKPOINT)

1. Xuất `reports/automation_impact_plan.md`:
   - Bảng ánh xạ TC → script (tách riêng mapping chắc chắn / ⚠️ suy luận / ❓ chưa automate)
   - Kế hoạch sửa từng script, có đánh dấu mục cần inspect DOM
   - Danh sách **ngoài phạm vi** kèm command tiếp theo
2. **⏸️ DỪNG LẠI**. Mode PLAN → **KẾT THÚC**. Mode APPLY → hỏi user duyệt, **đặc biệt xác nhận nhóm ⚠️ mapping suy luận và nhóm 🗑️ archive**

### Bước 5: Sửa (Mode APPLY — chỉ sau khi user duyệt)

1. Sửa theo kế hoạch, **chỉ chạm đúng phần đã liệt kê**
2. Cần locator mới → mở browser headed (viewport theo `--viewport-size` lúc launch, KHÔNG resize), đưa về đúng trạng thái, inspect DOM, verify locator match đúng 1 element
3. TC archive → thêm annotation skip kèm lý do và mã ticket:
   ```
   test.skip('CRM_PRJ_TC_031 - Copy Project', ...)   // Gỡ theo TICKET-123, chờ user xác nhận xoá
   ```
4. Sau mỗi thay đổi, kiểm lại metadata Allure còn đủ và **TC ID không đổi**

### Bước 6: Chạy Lại & Xác Nhận

1. Chạy các test vừa sửa — phải PASS **2 lần liên tiếp**
2. Chạy thêm test **cùng Page Object** nhưng không nằm trong delta — để chắc sửa POM không làm gãy test khác. Đây là bước hay bị bỏ nhất
3. Còn đỏ:

| Nguyên nhân | Hành động |
|---|---|
| Sửa thiếu / sai kỳ vọng mới | Quay lại Bước 3 — **tối đa 3 vòng** |
| App **chưa** implement thay đổi | DỪNG — đây là bug/chưa xong việc, không phải lỗi test. Báo user, gợi ý `/create-bug-report` |
| Locator gãy diện rộng | → `/heal-locators` |

### Bước 7: Báo Cáo Cuối

Cập nhật `reports/automation_impact_plan.md`:
- Danh sách file đã sửa (`file:line`, code cũ → mới)
- Kết quả chạy: test trong delta + test hồi quy cùng POM
- Việc còn lại: TC mới cần automate, TC archive chờ xác nhận xoá, mapping ❓ chưa giải quyết
- Nhắc chạy `/generate-traceability-matrix` để RTM khớp lại

## Output

### Mode PLAN
- `reports/automation_impact_plan.md`: bảng ánh xạ TC → script, kế hoạch sửa từng file, danh sách ngoài phạm vi

### Mode APPLY
- Tất cả output Mode PLAN, cộng thêm:
  - Code đã sửa (`file:line`, code cũ → mới)
  - Kết quả chạy lại (delta + hồi quy cùng POM)
  - Trạng thái: ✅ ĐÃ ĐỒNG BỘ / ⚠️ CÒN VIỆC NGOÀI PHẠM VI / ❌ CHƯA XỬ LÝ XONG

## Command liên quan

| Tình huống | Command |
|---|---|
| Trước đó — cập nhật requirements từ ticket | `/update-requirements-from-ticket` |
| Trước đó — **đồng bộ TC theo Impact Report** (nguồn kỳ vọng mới) | `/update-testcases-from-impact` ⭐ |
| Trước đó — chưa có nguồn map TC ↔ script | `/generate-traceability-matrix` |
| TC mới hoàn toàn, chưa có script | `/generate-automation-from-testcases` |
| Sau khi sửa — locator gãy diện rộng | `/heal-locators` |
| Sau khi sửa — muốn chạy cả suite kiểm hồi quy | `/run-and-fix-tests` |
| App chưa implement đúng thay đổi | `/create-bug-report` |
