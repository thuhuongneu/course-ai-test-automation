---
description: Cập nhật manual test cases đã có theo Impact Report (chế độ delta) — sửa TC stale tại chỗ trong index, đánh dấu Deprecated TC của chức năng bị gỡ, ghi Nhật ký thay đổi, xuất Delta TC List cho automation. Hỗ trợ 2 mode — PLAN (chỉ lập kế hoạch) và APPLY (sửa + cập nhật danh mục).
skills:
  - skills-rbt-manual-testing
  - skills-coverage-traceability
  - skills-testcase-reviewer
---

# Workflow: Cập Nhật Test Cases Theo Impact Report

> **BẮT BUỘC (MANDATORY SKILLS):** Nạp và đọc kỹ trước khi bắt đầu:
> - **`skills-rbt-manual-testing`** (`.claude/skills/skills-rbt-manual-testing/SKILL.md`) — **Mode DELTA** + Quy Tắc Xuất File (index bất biến, ngưỡng tách part, Bảng Đối Soát Coverage)
> - **`skills-coverage-traceability`** (`.claude/skills/skills-coverage-traceability/SKILL.md`) — Mapping Rules để nối REQ ID ↔ TC ID

Mắt xích **giữa** của chuỗi delta 3 tầng. Requirements đổi → Impact Report chỉ ra TC stale → **workflow này** sửa đúng TC đó → `/update-automation-from-impact` sửa script tương ứng.

```
/update-requirements-from-ticket   (tầng requirements — đã có)
        ↓ Impact Report
/update-testcases-from-impact      (tầng test case — WORKFLOW NÀY)
        ↓ Delta TC List
/update-automation-from-impact     (tầng automation — đã có)
```

## Workflow này khác gì `/review-testcases`?

Hai workflow trả lời **hai câu hỏi khác nhau**. Chạy sai cái là bỏ sót đúng thứ cần bắt:

| | Workflow này (delta) | `/review-testcases` (chất lượng) |
|---|---|---|
| **Câu hỏi trả lời** | "TC còn khớp requirement **mới** không?" | "TC viết có đạt chuẩn không?" |
| **Input** | Impact Report + REQ đã đổi | Bộ TC (+ requirements nếu có) |
| **Bắt được** | TC **stale** — mô tả hành vi đã bị thay | TC mơ hồ, thiếu assertion, trùng lặp, thiếu boundary |
| **Bỏ sót** | Chất lượng diễn đạt của TC | **TC stale** — TC viết rất tốt về hành vi **cũ** vẫn được 12/12 điểm rubric |
| **Ghi vào đâu** | Sửa **tại chỗ** trong index, sao lưu bản cũ vào `archive/` | Sinh file `<tên>_improved.md` mới, giữ nguyên gốc |

> 🚨 **KHÔNG dùng `/review-testcases` mode FIX để đồng bộ TC theo ticket.** Nó chấm rubric chất lượng, không đối chiếu với REQ đã đổi, và đẻ ra file `_improved` **lạc tên index** — `/update-automation-from-impact` đọc theo mẫu `docs/testcases/<module>/test_cases_<module>.md` sẽ không thấy bản mới.
>
> Chạy `/review-testcases` **sau** workflow này thì hợp lý: đồng bộ nội dung trước, chấm chất lượng sau.

## ⚠️ Nguyên tắc thực thi

- **Tất cả output bằng Tiếng Việt**
- 🚨 **CẤM sinh lại cả file/module TC.** Chỉ sửa đúng dòng của TC nằm trong delta. Sinh lại là xoá sạch công biên tập tay và các TC bổ sung đã tích luỹ
- 🚨 **CẤM đổi TC ID, CẤM đánh lại số từ `001`** — TC ID là khoá nối sang automation (`allure.label('testId', ...)`) và RTM. Đứt TC ID là vỡ truy vết cả hai chiều
- 🚨 **CẤM xoá dòng TC** — chức năng bị gỡ thì đổi trạng thái `🗑️ Deprecated` kèm mã ticket, giữ nguyên dòng (đối xứng quy tắc "KHÔNG xoá dòng REQ" của tầng requirements)
- 🚨 **CẤM sinh file `_improved` / `_v2` / `_new`** — tên index **bất biến** là `test_cases_<module>.md`. Bản trước khi sửa chuyển vào `archive/test_cases_<module>_<nền-tảng>_vN.md`
- **KHÔNG tự viết TC cho REQ mới (nhóm ➕)** — ngoài phạm vi, route sang `/generate-testcases-manual-rbt` hoặc `/generate-testcases-from-requirements`
- **KHÔNG sửa TC theo suy đoán từ tên TC** — phải đọc **nội dung REQ sau khi đổi** trong tài liệu requirements. Tên TC không chứa đủ thông tin để biết kỳ vọng mới là gì
- **KHÔNG bịa mapping REQ ↔ TC.** Không map được thì ghi vào mục "cần xác nhận", KHÔNG đoán rồi sửa nhầm TC
- 🚨 **Bám đúng độ hạt của bộ TC đang có.** Bộ TC viết ở độ hạt GỘP (có Bảng biến thể) thì TC bổ sung cũng phải gộp, và sửa một biến thể là sửa **dòng biến thể** chứ không tách nó ra thành TC mới. Bộ TC viết ở độ hạt TÁCH thì mỗi case mới là một TC. Trộn hai độ hạt trong cùng một file làm hỏng cách đếm và cách giao việc — xem mục **Độ Hạt Test Case** trong skill
- 🚨 **CẤM đổi độ hạt của cả bộ TC trong chế độ DELTA.** Đổi độ hạt là đánh lại toàn bộ TC ID, mâu thuẫn trực tiếp với quy tắc giữ nguyên TC ID ở trên. Muốn đổi thì phải là quyết định riêng, có xác nhận của user, và chỉ làm được khi chưa có script / execution report / RTM nào trỏ vào bộ TC
- 🚨 **Ticket đụng nhãn / bố cục / thứ tự field / giá trị mặc định là đụng VÒNG 1, không phải chỉ Vòng 2.** Thêm một field vào form thì sửa TC validation của field đó (V2) là **chưa xong** — bảng kiểm liệt kê thành phần màn hình ở V1 (`UI cơ bản`) cũng phải thêm dòng. Đây là chỗ DELTA hụt nhiều nhất, vì Impact Report chỉ liệt kê TC map trực tiếp với REQ
- ⚠️ **DELTA chỉ chấm lại nhánh 4 vòng mà ticket chạm tới**, giữ nguyên phần còn lại của Bảng Đối soát loại kiểm thử — KHÔNG rà lại cả module (đó là việc của `/generate-testcases-manual-rbt`)
- ⚠️ Sau khi user duyệt kế hoạch → agent tự sửa hết, KHÔNG hỏi lại giữa chừng

## 2 Chế độ (Mode)

| Mode | Khi nào sử dụng | Output |
|---|---|---|
| **PLAN** (mặc định) | Cần biết ticket này đụng tới TC nào, sửa gì | Bảng ánh xạ REQ → TC + kế hoạch sửa từng TC |
| **APPLY** | Muốn agent sửa luôn | Như PLAN + TC đã sửa + Nhật ký thay đổi + Delta TC List |

> User nói "sửa luôn", "cập nhật TC đi", "apply" → tự động **Mode APPLY**.

## Input cần thu thập

| Input | Bắt buộc? | Ghi chú |
|---|---|---|
| **Impact Report** | ⭐ Bắt buộc (hoặc thay bằng danh sách REQ dưới) | `docs/requirements/<module>/impact/impact_<TICKET-ID>.md` — do `/update-requirements-from-ticket` ghi ra, hoặc `impact_spec_<YYYY-MM-DD>.md` do `/generate-requirements-from-api` ghi ra khi spec API đổi phiên bản. Hoặc dán trực tiếp nội dung |
| **Danh sách REQ đã đổi** | Thay thế cho Impact Report | Khi user tự biết REQ nào đổi: REQ ID + đổi cái gì |
| **Tài liệu requirements hiện hành** | ⭐ Bắt buộc | `docs/requirements/<module>/requirements_<module>.md` — nguồn sự thật của kỳ vọng **mới** |
| **File test cases hiện hành** | ⭐ Bắt buộc | Index `docs/testcases/<module>/test_cases_<module>.md` → theo `## Bản đồ tài liệu` mở file nền tảng `<nền-tảng>/test_cases_<module>_<nền-tảng>.md` (+ `parts/` nếu có). REQ đổi khai áp nền tảng nào thì mở **đủ** file của các nền tảng đó |
| **Evidence của module** | ⭕ Khuyến nghị · ⭐ **Bắt buộc khi ticket chạm V1** | `docs/requirements/<module>/<nền-tảng>/evidence/*.png` — **bắt buộc mở** nếu thay đổi đụng nhãn nguyên văn, bố cục, thứ tự field, giá trị mặc định hoặc định dạng hiển thị (4 nhóm không được suy diễn). Chưa có ảnh mới sau khi đổi → gắn `@NeedsVerify`, KHÔNG sửa theo suy đoán |
| **RTM** | ⭕ Khuyến nghị | Có sẵn thì map REQ → TC nhanh và chắc hơn nhiều |

> Impact Report ghi `⚠️ chưa rà soát` ở cột TC → **dừng**, báo user chạy `/generate-traceability-matrix` trước. Không có nguồn map thì workflow này chỉ đoán mò.

## Các bước thực hiện

### Bước 1: Đọc Delta

1. Đọc Impact Report, trích bảng **Test case cần xử lý** — 3 nhóm: `⚠️ Review & sửa` / `🗑️ Archive` / `➕ Viết mới`
2. Với mỗi REQ trong nhóm 🟡 (SỬA): đọc **nội dung REQ sau khi đổi** trong `requirements_<module>.md` để biết kỳ vọng mới là gì
3. Đọc **Nhật ký thay đổi** ở cuối tài liệu requirements — nó ghi rõ đổi từ gì sang gì, đây là nguồn chính xác nhất
4. Ghi rõ **đổi cái gì**: precondition / steps / expected result / test data / message nguyên văn / phân quyền

> Thay đổi đụng bố cục, nhãn, thứ tự field, định dạng hiển thị → **BẮT BUỘC mở evidence** theo Quy Tắc Đối Chiếu Evidence của `skills-rbt-manual-testing`. Sửa TC chỉ từ chữ là đường ngắn nhất tới TC bịa.

### Bước 2: Map REQ → TC

Áp dụng Mapping Rules của `skills-coverage-traceability`, theo thứ tự tin cậy:

| Cách map | Độ tin cậy | Dấu hiệu |
|---|---|---|
| Cột `REQ ID` trong bảng TC | ✅ Chắc chắn | TC có cột truy vết về REQ |
| RTM (`traceability_matrix.md`) | ✅ Chắc chắn | Tra ngược REQ → danh sách TC |
| Bảng Đối Soát Coverage trong index | ✅ Chắc chắn | Mục "mọi REQ phải có ≥1 TC" |
| So khớp mô tả TC ↔ nội dung REQ | ⚠️ Suy luận | **Cần user xác nhận trước khi sửa** |
| Không tìm thấy TC nào | ❓ | REQ chưa có TC — chuyển sang nhóm ➕, KHÔNG sửa gì |

Mỗi TC ghi lại: **file nào** (index hay `parts/part_NN_*.md`), **dòng nào**, TC ID.

### Bước 3: Lập Kế Hoạch Sửa Từng TC

Phân loại hành động cho TC — **khác** với hành động dành cho script:

| Delta của REQ | Vòng · Nhánh | Hành động với TC | Phạm vi chạm |
|---|---|---|---|
| Đổi **nhãn nguyên văn** (nút, tiêu đề, nhãn field, thông báo) | **V1 · UI cơ bản** | Sửa nhãn trong bảng kiểm màn hình **và** mọi Expected Result trích nhãn đó | Bảng kiểm V1 + các ô Expected liên quan |
| **Thêm / bớt field** trên form | **V1 · UI cơ bản** *(+ V2 kèm theo)* | Thêm/bớt **dòng trong bảng kiểm** thành phần màn hình, kiểm lại thứ tự field và thứ tự Tab | Bảng kiểm V1 — **cộng thêm** TC Required/Validation ở V2 |
| Đổi **giá trị mặc định** (checkbox tick sẵn, option đang chọn, con trỏ vào ô nào) | **V1 · UI cơ bản** | Sửa dòng tương ứng trong bảng kiểm | 1 dòng bảng kiểm |
| Đổi **định dạng hiển thị** (tiền tệ, ngày, giờ, badge) | **V1 · Display** | Sửa Expected Result ghi đúng định dạng mới | Nhóm TC hiển thị |
| Đổi **hành vi giao diện** (nút khoá đến khi đủ điều kiện, field bật/tắt theo lựa chọn khác) | **V2 · UI Behavior** | Sửa TC hành vi + kiểm cả hai chiều (bật điều kiện → mở khoá; tắt → khoá lại) | Nhóm TC hành vi |
| Đổi **expected result** / message | V2 · theo nhánh của TC đó | Sửa cột Expected Result, ghi message **nguyên văn** | 1 ô trong bảng |
| Field từ tuỳ chọn → **bắt buộc** | **V2 · Required** | Sửa TC happy path + **thêm TC negative** cho trường hợp để trống | 1 TC sửa + 1 TC mới cùng nhóm |
| Đổi **luật validation** (độ dài, định dạng, dải giá trị) | **V2 · Validation + BVA** | Sửa TC boundary — kiểm lại cả 3 mốc: dưới ngưỡng / đúng ngưỡng / trên ngưỡng | Nhóm TC boundary của field đó |
| Đổi **steps / luồng** | **V2 · Use Case** | Sửa cột Steps, kiểm lại Precondition còn đúng không | Steps + Precondition |
| Đổi **quy tắc nghiệp vụ / trạng thái** | **V2 · Business Rule · State Transition** | Sửa bảng quyết định / bảng transition, kiểm cả ô hợp lệ lẫn ô bị chặn | Nhóm TC tương ứng |
| Đổi **phân quyền** | **V3 · Permission** | Sửa TC phân quyền, kiểm ma trận role còn khớp | Nhóm TC phân quyền |
| Đổi **breakpoint / trình duyệt cam kết** | **V4 · Responsive · Compatibility** | Sửa danh sách kích thước/trình duyệt trong TC | Nhóm TC V4 |
| Chức năng **bị gỡ** (REQ 🔴) | Nhánh của TC đó | Đổi trạng thái TC → `🗑️ Deprecated (TICKET-XXX)`, **KHÔNG xoá dòng** | Cột trạng thái |
| REQ **mới** (🟢) | — | ❌ Ngoài phạm vi → `/generate-testcases-manual-rbt` | — |

Mỗi mục ghi: `file:dòng`, TC ID, **vòng · nhánh**, sửa ô nào, có cần mở evidence không.

> 🚨 **Bốn dòng V1 đầu bảng BẮT BUỘC mở evidence trước khi sửa** — nhãn nguyên văn, thứ tự field, giá trị mặc định, định dạng hiển thị là đúng **4 nhóm TC không được suy diễn** theo Quy Tắc Đối Chiếu Evidence trong skill. Chưa có ảnh mới của màn hình sau khi đổi thì **KHÔNG sửa**, gắn `@NeedsVerify` và báo user recon bổ sung.
>
> 🚨 **Bộ TC ở độ hạt GỘP:** thêm field = thêm **một dòng vào Bảng kiểm** của TC `UI cơ bản` (Kiểu B), **KHÔNG** tạo TC mới và **KHÔNG** tách dòng đó ra. Trần 6 biến thể/TC vẫn áp dụng — vượt thì tách theo nhóm, không nhét thêm.

**Kiểm tác động lan toả** — chỗ hay bị bỏ sót nhất:

- **Ticket có đụng thành phần nhìn thấy được trên màn hình không** (thêm/bớt field, đổi nhãn, đổi vị trí, đổi giá trị mặc định)? → TC `UI cơ bản` ở **Vòng 1** phải cập nhật, **không chỉ** TC validation ở Vòng 2. Đây là câu hỏi hay bị bỏ qua nhất
- Field đổi thành bắt buộc → có TC nào khác **dùng field đó ở bước phụ** mà giờ sẽ fail không?
- TC bị Deprecated → có TC nào **lấy nó làm precondition** không?
- Thêm field mới → **bảng 15 loại field** của loại field đó đã được đối soát đủ chưa (Password 9 mục, Email 9 mục…), hay mới sinh 2–3 TC cho xong?
- Số TC sau khi thêm có **vượt ngưỡng 40** không → phải tách `parts/` theo quy tắc của skill?

### Bước 4: Báo Cáo & Xin Duyệt (CHECKPOINT)

1. Xuất `docs/testcases/<module>/impact/impact_plan_<TICKET-ID>.md`:
   - Bảng ánh xạ REQ → TC (tách riêng mapping ✅ chắc chắn / ⚠️ suy luận / ❓ chưa có TC)
   - Kế hoạch sửa từng TC **kèm cột `Vòng · Nhánh`**, đánh dấu mục cần mở evidence
   - **Danh sách nhánh 4 vòng bị ticket chạm tới** — user nhìn một dòng là biết ticket này có đụng lớp giao diện (V1) hay chỉ đụng validation (V2):
     ```
     Nhánh bị chạm: V1 · UI cơ bản (thêm field Deadline) · V2 · Required · V2 · Validation
     Nhánh KHÔNG đụng: toàn bộ V3, V4 — giữ nguyên
     ```
   - Tác động lan toả phát hiện ở Bước 3
   - Danh sách **ngoài phạm vi** kèm command tiếp theo
2. **⏸️ DỪNG LẠI**. Mode PLAN → **KẾT THÚC**. Mode APPLY → hỏi user duyệt, **đặc biệt xác nhận nhóm ⚠️ mapping suy luận, nhóm 🗑️ Deprecated, và danh sách nhánh 4 vòng bị chạm**

### Bước 5: Sửa TC (Mode APPLY — chỉ sau khi user duyệt)

1. **Sao lưu trước khi sửa:** copy bản hiện tại của **từng file nền tảng sắp sửa** sang `archive/test_cases_<module>_<nền-tảng>_v<N>.md` (`N` = số phiên bản kế tiếp). Đây là bước không được bỏ — sửa tại chỗ mà không sao lưu là mất bản đối chiếu
2. Sửa **tại chỗ** trong file nền tảng `<nền-tảng>/test_cases_<module>_<nền-tảng>.md` (hoặc `<nền-tảng>/parts/part_NN_*.md` nếu đã tách) — **chỉ chạm đúng ô đã liệt kê** ở Bước 3. Bộ TC cũ chưa có tầng nền tảng → sửa trong `test_cases_<module>.md` rồi chuyển một lần theo skill rbt (Quy Tắc Xuất File mục 2)
3. TC bị gỡ → đổi trạng thái, giữ nguyên dòng và TC ID:
   ```
   | CRM_PRJ_TC_031 | Copy Project | ... | 🗑️ Deprecated — gỡ theo TICKET-123 (2026-08-17) |
   ```
4. TC negative/boundary **mới sinh trong phạm vi REQ 🟡** → cấp TC ID **tiếp theo dải hiện có** của module, KHÔNG chèn số vào giữa
5. Cập nhật **Bảng Đối Soát Coverage** ở index — REQ 🔴 không còn TC active là **đúng**, phải ghi rõ lý do; REQ 🟡 vẫn phải có ≥1 TC active
6. Cập nhật **Bảng Đối soát loại kiểm thử (4 vòng)** ở cuối index — **chỉ những nhánh ticket chạm tới**, giữ nguyên phần còn lại:
   - Nhánh có TC bị sửa/thêm → cập nhật lại dải TC ID và **số biến thể** (độ hạt GỘP)
   - Nhánh mà ticket vừa làm phát sinh nhu cầu mới nhưng chưa có TC → chấm `🔴 Thiếu`, bổ sung trước khi kết thúc
   - Nhánh mà chức năng vừa bị gỡ khiến không còn TC active → chuyển `➖` kèm lý do *"chức năng gỡ theo TICKET-XXX"*, **không** xoá dòng khỏi bảng

### Bước 6: Quality Gate Delta

Kiểm đủ 7 mục, thiếu mục nào là chưa xong:

- [ ] **Mọi TC ID giữ nguyên** — không TC nào bị đổi số hay đánh lại
- [ ] **Tên file index không đổi** — không sinh `_improved` / `_v2` ở ngoài `archive/`
- [ ] **Không dòng TC nào bị xoá** — TC gỡ đều ở trạng thái 🗑️ Deprecated kèm mã ticket
- [ ] **Bảng Đối Soát Coverage** khớp lại: mọi REQ 🟡/🟢 active có ≥1 TC active
- [ ] **Bảng Đối soát loại kiểm thử (4 vòng)** đã cập nhật **đúng những nhánh ticket chạm tới** — không rà lại cả module, không để nhánh nào rơi vào trạng thái sai sau khi sửa. Ticket đụng thành phần màn hình mà nhánh `V1 · UI cơ bản` không đổi gì = **dấu hiệu đã bỏ sót**
- [ ] Số TC trong index **khớp** tổng của các `parts/` (nếu có tách)
- [ ] **`docs/testcases/README.md`** (danh mục) đã cập nhật: số TC, REQ bao phủ, ngày cập nhật

### Bước 7: Nhật Ký & Delta TC List

1. Ghi **Nhật ký thay đổi** vào cuối `test_cases_<module>.md`:

```markdown
## Nhật ký thay đổi

| Ngày | Ticket | TC bị ảnh hưởng | Thay đổi | Bản sao lưu |
|---|---|---|---|---|
| 2026-08-17 | TICKET-123 | CRM_PRJ_TC_002 | V1 · UI cơ bản: thêm dòng bảng kiểm cho field `Deadline`, cập nhật thứ tự field | `archive/test_cases_project_v2.md` |
| 2026-08-17 | TICKET-123 | CRM_PRJ_TC_018 | V2 · Required: Deadline tuỳ chọn → bắt buộc — sửa expected result + thêm TC_077 negative | ↑ |
| 2026-08-17 | TICKET-123 | CRM_PRJ_TC_031 | 🗑️ Deprecated — chức năng Copy Project đã gỡ | ↑ |
```

2. Xuất **Delta TC List** ra chat — đây là input trực tiếp cho `/update-automation-from-impact`:

```markdown
## Delta TC List — TICKET-123 · 2026-08-17

| TC ID | Vòng · Nhánh | Hành động đã làm | Đổi cái gì (cho automation) |
|---|---|---|---|
| CRM_PRJ_TC_002 | V1 · UI cơ bản | ✏️ Đã sửa | Thêm dòng bảng kiểm cho field `Deadline` → sửa assertion danh sách thành phần màn hình |
| CRM_PRJ_TC_018 | V2 · Required | ✏️ Đã sửa | Expected result: giờ báo lỗi khi Deadline trống → sửa assertion |
| CRM_PRJ_TC_077 | V2 · Required | ➕ Mới (trong REQ 🟡) | TC negative mới → cần viết script mới |
| CRM_PRJ_TC_031 | V2 · Save/Edit/Delete | 🗑️ Deprecated | Script tương ứng đánh dấu skip, KHÔNG xoá file |

> Cột **Vòng · Nhánh** giúp `/update-automation-from-impact` chọn đúng kiểu sửa: TC `V1 · UI cơ bản` thường là assertion trên danh sách phần tử (`Automatable: Partial`), TC `V2 · Validation` thường map sang test data-driven.

### Ngoài phạm vi
| REQ | Việc còn lại | Command |
|---|---|---|
| REQ-PRJ-79 → 81 | Chưa có TC | `/generate-testcases-manual-rbt` |
```

3. Nhắc user chuỗi tiếp theo: `/update-automation-from-impact` (script đã có) → `/generate-traceability-matrix` (khớp lại RTM)

## Output

### Mode PLAN
- `docs/testcases/<module>/impact/impact_plan_<TICKET-ID>.md`: bảng ánh xạ REQ → TC, kế hoạch sửa từng TC (kèm `Vòng · Nhánh`), **danh sách nhánh 4 vòng bị chạm**, tác động lan toả, danh sách ngoài phạm vi

> Phân biệt hai file cùng gắn với một ticket: `requirements/<module>/impact/impact_<TICKET-ID>.md` là **báo cáo** — nói *cái gì đã đổi*. `testcases/<module>/impact/impact_plan_<TICKET-ID>.md` là **kế hoạch** — nói *sẽ sửa TC nào, sửa gì*.

### Mode APPLY
- Tất cả output Mode PLAN, cộng thêm:
  - File nền tảng đã sửa **tại chỗ** (tên file không đổi) · index `test_cases_<module>.md` cập nhật Bản đồ tài liệu + Nhật ký
  - `archive/test_cases_<module>_<nền-tảng>_v<N>.md` — bản trước khi sửa
  - **Bảng Đối soát loại kiểm thử (4 vòng)** đã cập nhật đúng nhánh bị chạm
  - Nhật ký thay đổi ở cuối file TC
  - `docs/testcases/README.md` đã cập nhật
  - **Delta TC List** hiển thị trong chat
  - Trạng thái: ✅ ĐÃ ĐỒNG BỘ / ⚠️ CÒN VIỆC NGOÀI PHẠM VI / ❌ CHƯA XỬ LÝ XONG

## Command liên quan

| Tình huống | Command |
|---|---|
| Trước đó — cập nhật requirements từ ticket | `/update-requirements-from-ticket` |
| Trước đó — chưa có nguồn map REQ ↔ TC | `/generate-traceability-matrix` |
| REQ mới hoàn toàn, chưa có TC | `/generate-testcases-manual-rbt` · `/generate-testcases-from-requirements` |
| Sau đó — cập nhật automation script **đã có** | `/update-automation-from-impact` |
| Sau đó — automate TC mới hoàn toàn | `/generate-automation-from-testcases` |
| Sau đó — chấm chất lượng bộ TC vừa sửa | `/review-testcases` |
| Sau đó — chạy lại phần TC bị ảnh hưởng | `/execute-test-cases` |
| Sau đó — khớp lại ma trận truy vết | `/generate-traceability-matrix` |
