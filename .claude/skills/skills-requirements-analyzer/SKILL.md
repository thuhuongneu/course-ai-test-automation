---
name: skills-requirements-analyzer
description: Kỹ năng phân tích trang web/module/tài liệu và sinh ra tài liệu Yêu cầu (Requirements Document/User Stories) chuẩn mực — có gán mã REQ ID truy vết được, phát hiện Ambiguity/Risk, kèm ma trận phân quyền và trạng thái.
---

# Kỹ năng Phân tích Yêu cầu (Requirements Analyzer)

Kỹ năng này cung cấp các hướng dẫn chi tiết để AI (Claude Code) có thể chuyển đổi giao diện UI, cấu trúc DOM/HTML, hoặc tài liệu (Jira ticket, .doc, user story) thành tài liệu Yêu cầu rõ ràng, chi tiết, **truy vết được**, phục vụ trực tiếp cho QA, Tester và Developer.

## 1. Mục tiêu cốt lõi
- Xây dựng tài liệu yêu cầu bám sát thực tế hệ thống đang chạy hoặc tài liệu gốc.
- **Mọi yêu cầu đều có mã REQ ID** — để test cases, automation và RTM truy vết ngược được.
- Đảm bảo tính bao quát: Happy Path, Edge Cases, phân quyền, trạng thái, thông báo lỗi.
- Phát hiện và ghi nhận có hệ thống các điểm mơ hồ (Ambiguity) và rủi ro (Risk).

---

## 2. Quy Ước Đánh Mã (BẮT BUỘC — dùng xuyên suốt toàn chuỗi)

| Loại | Format | Ví dụ | Dùng ở đâu |
|---|---|---|---|
| **Requirement** | `REQ-<MODULE>-<SỐ>` | `REQ-LOGIN-01` | Từng yêu cầu chức năng, business rule, validation rule |
| **Story** | `STORY-<MODULE>-<SỐ>` | `STORY-PRJ-03` | Nhóm REQ thành đơn vị công việc (backlog view) — xem mục 5 |
| **Ambiguity** | `AMB-<SỐ>` | `AMB-03` | Điểm mơ hồ cần clarify với PO/BA |
| **Risk** | `RISK-<SỐ>` | `RISK-02` | Rủi ro kiểm thử |

**Quy tắc:**
- Mỗi yêu cầu **đủ nhỏ để test được độc lập** — nếu 1 câu chứa nhiều rule, tách thành nhiều REQ
- REQ ID **không đổi** sau khi đã phát hành tài liệu (chỉ thêm mới, không đánh lại số)
- Test cases sinh ra ở bước sau **PHẢI ghi REQ ID** vào cột `REQ ID` — đây là mắt xích để kiểm chứng "đủ case" và chạy `/generate-traceability-matrix`

**Phân vai REQ vs STORY (quan trọng):**
- `REQ` là **lớp truy vết duy nhất** — bất biến, mọi test case / automation / RTM đều neo vào đây
- `STORY` chỉ là **lớp nhóm việc** — được phép đổi tên, gộp, tách lại theo cách team vận hành mà **không** ảnh hưởng tới REQ ID hay test case đã viết
- ❌ **TUYỆT ĐỐI KHÔNG** đánh lại số REQ khi tách file hay gom Story. REQ ID gắn với *hành vi*, không gắn với *vị trí trong tài liệu*

### 2.1. Nối tiếp mã REQ giữa các lần chạy (BẮT BUỘC — chống đụng mã)

Cùng một module có thể được phân tích **nhiều lần bằng nhiều workflow khác nhau** (`/generate-requirements-from-website` từ UI, `/analyze-requirement-document` từ Jira ticket, `/fetch-jira-requirements`…). Nếu mỗi lần đều đánh lại từ `01`, các dải mã sẽ đụng nhau và **RTM map sai hoàn toàn**.

**Quy trình bắt buộc TRƯỚC KHI gán REQ ID đầu tiên:**

1. **Đọc danh mục toàn hệ thống — LUÔN LÀM ĐẦU TIÊN:**
   ```
   docs/requirements/README.md
   ```
   Bảng danh mục cho biết: module nào đã có tài liệu · **prefix nào đã bị chiếm** · mã kế tiếp của từng module. Module mới **phải chọn prefix chưa có trong danh sách**.
   > ⚠️ **File không tồn tại (dự án mới)** → **tạo ngay** theo mục 5.7.1 trước khi đi tiếp. KHÔNG bỏ qua bước này rồi ghi thẳng tài liệu module.
2. **Kiểm tra tài liệu module:**
   ```
   docs/requirements/<module>/requirements_<module>.md
   ```
3. **Nếu CHƯA có** → đánh số từ `01`.
4. **Nếu ĐÃ có** → tìm số REQ lớn nhất đang dùng và **đánh tiếp từ số kế tiếp**. Ví dụ module đã dùng tới `REQ-PRJ-65` → yêu cầu mới bắt đầu từ `REQ-PRJ-66`.
5. **Ghi rõ nguồn** vào cột `Nguồn` của mỗi REQ mới: `Ticket ABC-123` / `UI thực tế` / `AC#2` — để biết mã đến từ đợt phân tích nào.
6. **Cập nhật ngược 2 nơi** (thiếu một trong hai là lần chạy sau đánh sai số):
   - Bảng metadata của tài liệu module:
     ```markdown
     | Dải mã đã dùng | REQ-PRJ-01 → REQ-PRJ-78 (đợt 1: UI recon 01→65 · đợt 2: ticket ABC-123 66→78) |
     | Mã kế tiếp     | REQ-PRJ-79 — KHÔNG đánh lại từ 01 |
     ```
   - **Bảng danh mục** trong `docs/requirements/README.md`: cột `REQ đã dùng`, `Mã kế tiếp`, `AMB treo`, `Cập nhật`

**Ràng buộc bổ sung:**
- ❌ Không "tái sử dụng" mã của REQ đã bị xoá — mã đã cấp là **vĩnh viễn chết**, kể cả khi yêu cầu đó không còn
- ✅ `<MODULE>` phải **giống hệt** giữa các đợt (`PRJ` thì mãi là `PRJ`, không lúc `PRJ` lúc `PROJECT`)
- ✅ Cùng nguyên tắc này áp cho `AMB-XX` và `RISK-XX` — đánh tiếp, không đánh lại
- ⚠️ Nếu yêu cầu mới **thay đổi hành vi của một REQ cũ** (không phải thêm mới): **giữ nguyên mã cũ**, sửa nội dung, và ghi chú `Cập nhật bởi ticket ABC-123` — không cấp mã mới cho cùng một hành vi

---

## 3. Quy trình trích xuất thông tin

Skill có **2 nhánh trích xuất** tuỳ theo nguồn đầu vào. Xác định nhánh **trước khi bắt đầu**:

| Nguồn đầu vào | Nhánh | Đặc điểm |
|---|---|---|
| Website/ứng dụng đang chạy | **3.1 — UI Recon** | Sự thật nằm ở hệ thống; xác minh bằng tương tác thật |
| Jira ticket, .docx, .pdf, .xlsx/.csv, .xml, user story | **3.2 — Document Analysis** | Sự thật nằm ở văn bản; xác minh bằng trích dẫn nguyên văn |
| Có cả hai, tài liệu phủ **đầy đủ** module | Chạy **3.2 trước** (nắm ý định) → **3.1 sau** (đối chiếu thực tế) | Mọi lệch pha giữa 2 nguồn là ambiguity, xem 3.3 |
| Có cả hai nhưng tài liệu **chỉ phủ một phần** | **3.3.1 — Tài liệu bán phần** | Tình huống phổ biến nhất ở hệ thống thiếu tài liệu. Phải chia vùng trước khi recon |

> ⚠️ **Chưa biết hệ thống có những module nào** → đây chưa phải việc của skill này. Chạy `/discover-system` trước để có bản đồ hệ thống + prefix, rồi mới recon từng module (xem mục **5.8**).

### 3.1. Nhánh UI Recon — phân tích website/ứng dụng thực tế

1. **Phân tích Khung giao diện (Layout):** Header, Footer, Sidebar, Main Content, Breadcrumb
2. **Thu thập Form & Inputs:** tất cả `input`, `select`, `textarea` — ghi nhận `type`, `required`, `maxlength`, `minlength`, `pattern`, giá trị mặc định
3. **Thu thập Actions:** chức năng từng nút (Save, Submit, Cancel, Delete, Edit), alerts/toasts/validation messages khi tương tác lỗi
4. **Trích xuất Workflows:** sự phụ thuộc giữa các thành phần (VD: nút Submit chỉ enable khi tích Checkbox)
5. **Thu thập Phân quyền (nếu có nhiều role):** đăng nhập từng role (nếu được cung cấp account) — ghi nhận role nào thấy gì / làm được gì
6. **Thu thập Trạng thái (nếu entity có status):** các trạng thái quan sát được và hành động cho phép ở từng trạng thái
7. **Thu thập Thông báo lỗi:** trigger từng validation để ghi nhận nguyên văn error message
8. **Đọc tầng network** (mục **3.1.1**) — bắt buộc khi hệ thống không có tài liệu
9. **Thăm dò phân quyền khi thiếu account** (mục **3.1.2**) — chỉ khi không đủ account cho mọi role

### 3.1.1. Thu thập từ tầng Network (BẮT BUỘC khi hệ thống không có tài liệu)

DOM chỉ cho biết **hệ thống đang hiển thị gì**. Tầng network cho biết **hệ thống thật sự làm gì** — đây là nguồn quý nhất khi không có một dòng tài liệu nào, và là thứ duy nhất tiết lộ được phần backend mà UI giấu đi.

Bật `browser_network_requests` trong suốt quá trình recon (không chỉ lúc load trang — **phải bật khi submit form** mới bắt được response lỗi).

| Loại request | Dữ kiện rút ra được | Ghi vào mục nào |
|---|---|---|
| `GET /api/<entity>` — danh sách | Tên trường thật của entity · trường **có trong data nhưng không hiển thị** trên bảng · cấu trúc phân trang (`page`/`size`/`total`) | 6.3 Field Spec · 6.2 REQ về phân trang |
| `GET /api/<entity>/{id}` — chi tiết | Toàn bộ schema entity · quan hệ tới entity khác (`customer_id`, `owner`) | 6.3 · Bản đồ phụ thuộc |
| `POST`/`PUT` — payload submit | Tên field thật ↔ nhãn hiển thị · field ẩn được gửi kèm · **kiểu dữ liệu thật** (số vs chuỗi) | 6.3 (cột ánh xạ nhãn ↔ field) |
| Response **4xx khi submit sai** | ⭐ **Validation server-side** — rule mà UI không chặn · **nguyên văn message** trong body (thường khác message hiện trên UI) | 6.4 Validation Messages |
| Response chứa enum | Danh sách trạng thái đầy đủ — kể cả trạng thái **chưa từng thấy trên UI** | 6.6 Ma trận Trạng thái |
| Endpoint gọi khi mở dropdown | Nguồn dữ liệu của dropdown (bảng master nào) · **tổng số option thật** so với số đang hiển thị | 6.3 · phát hiện dropdown bị giới hạn |
| Endpoint **có nhưng không UI nào gọi** | Tính năng chưa build hoặc đã gỡ giao diện | 6.2 với trạng thái ⚪ + `AMB-XX` |
| Response `403` khi đổi role | Ranh giới phân quyền thật ở tầng server | 6.5 Ma trận Phân quyền |

**Luật ghi nhận — bắt buộc:**

| Tình huống | Xử lý |
|---|---|
| Rule chỉ có ở server, UI **không** chặn | Vẫn gán REQ, cột `Nguồn` ghi `API response · POST /api/x · 422` + mở `AMB-XX` hỏi: chặn ở UI hay cố ý để server chặn? |
| Message API **khác** message hiện trên UI | Ghi **cả hai nguyên văn** vào 6.4 + `AMB-XX` 🔴 — đây là bug tiềm ẩn, không phải ambiguity thường |
| Field có trong payload nhưng **không** có trên form | Gán REQ trạng thái ⚪ hoặc mở `AMB-XX` — **không** im lặng bỏ qua |
| Endpoint không có UI | Ghi nhận, **KHÔNG** gọi thử để "kiểm chứng" |

🚫 **Cấm tuyệt đối:** gọi API trực tiếp (curl/fetch/`browser_evaluate` gọi endpoint) để dò hành vi. Recon chỉ được **quan sát thụ động** request do UI tự phát sinh. Gọi thẳng API là hành vi kiểm thử xâm nhập, càng nguy hiểm trên môi trường dùng chung — và request tự tay tạo ra **không** chứng minh được điều gì về hành vi của UI.

📌 Cột `Nguồn` cho dữ kiện lấy từ network ghi rõ: `API · POST /api/customers · 422` — đủ để người review mở lại tab Network mà đối chiếu.

### 3.1.2. Phân quyền khi không đủ account cho mọi role

Thực tế hay gặp: hệ thống có 5 role nhưng QA chỉ đưa 1 account admin. Không được vì thế mà **bỏ trống** mục 6.5, cũng không được **bịa** ma trận.

**Bước 1 — Xác định hệ thống có những role nào** (không cần account vẫn làm được):

| Nguồn | Cách lấy |
|---|---|
| Màn hình quản lý người dùng / phân quyền | Đọc dropdown chọn role, bảng danh sách role — thường lộ đủ tên role |
| Màn hình cấu hình quyền | Ma trận quyền của chính hệ thống — **nguồn tốt nhất**, chép nguyên trạng |
| Response API chứa `role` / `permissions` | Mục 3.1.1 |
| Tài liệu QA cung cấp (nếu có) | Mục 3.2 |

**Bước 2 — Suy diễn có kiểm soát, theo 3 mức bằng chứng:**

| Mức | Cách có được | Ghi vào ma trận |
|---|---|---|
| ✅ **Đã kiểm chứng** | Đăng nhập đúng role đó và thử thật | `✅` / `❌` bình thường |
| ⚠️ **Suy từ cấu hình** | Đọc từ màn hình phân quyền của hệ thống, chưa đăng nhập thử | `⚠️✅` / `⚠️❌` — kèm chú thích nguồn |
| ❔ **Chưa có căn cứ** | Không có account, không có màn hình cấu hình | `❔` + **bắt buộc** mở `AMB-XX` 🔴 |

**Bước 3 — Với account đang có, vẫn khai thác được:**
- Menu/nút **bị ẩn** vs **hiện nhưng disabled** — hai cái này nghĩa khác nhau, phải đọc DOM để phân biệt (ảnh không cho biết)
- Truy cập thẳng URL của chức năng bị ẩn → quan sát phản ứng (chuyển hướng · trang 403 · vẫn vào được). ⚠️ Chỉ thử với **URL chỉ đọc**, tuyệt đối không thử URL xoá/sửa
- So sánh response `permissions` trả về lúc đăng nhập với danh sách quyền đầy đủ ở màn hình cấu hình

**Luật cứng:**
- ❌ **Không** suy quyền của role B từ hành vi của role A — "admin làm được nên manager chắc cũng làm được" là bịa
- ❌ Ô `❔` **không được** làm tròn thành `❌`. "Chưa kiểm chứng" ≠ "không có quyền"
- ✅ Cuối ma trận 6.5 **bắt buộc** có dòng tổng: `Đã kiểm chứng: N ô · Suy diễn: M ô · Chưa rõ: K ô` — người đọc biết ngay tin được bao nhiêu phần
- ✅ Mỗi role **chưa có account** = 1 `AMB-XX` 🔴 riêng, nội dung: *"Xin account role X để kiểm chứng N ô đang suy diễn"*

### 3.2. Nhánh Document Analysis — phân tích tài liệu có sẵn

#### Bước 0 — Đọc được file đã, đừng đoán

Mỗi định dạng có cách đọc riêng. **KHÔNG** dùng `Read` thẳng cho định dạng nhị phân:

| Định dạng | Cách xử lý bắt buộc |
|---|---|
| `.md`, `.txt`, `.html`, `.xml`, `.json` | `Read` trực tiếp. File `.doc` export từ Jira thực chất là HTML → `Read` rồi bóc tag |
| `.docx`, `.dotx` | **Ủy quyền cho skill `docx`** — là ZIP/OOXML, `Read` thẳng ra rác |
| `.xlsx`, `.xlsm`, `.csv`, `.tsv` | **Ủy quyền cho skill `xlsx`** |
| `.pdf` | **Ủy quyền cho skill `pdf`** |
| `.pptx` | **Ủy quyền cho skill `pptx`** |
| Ảnh (mockup, screenshot) | `Read` — công cụ hiển thị ảnh trực tiếp |
| URL Jira/Confluence | **KHÔNG tự fetch.** Route sang `/fetch-jira-requirements` + `skills-jira-integration`. Nếu MCP chưa authorize → **dừng và báo user**, tuyệt đối không bịa nội dung ticket |

> ⚠️ Nếu không đọc được file bằng bất kỳ cách nào → **báo user và dừng**. Không suy đoán nội dung từ tên file.

#### Bước 1 — Trích xuất theo thứ tự

1. **Metadata:** Ticket ID, Type, Priority, Status, Reporter, Assignee, Fix Version, Sprint, Labels, Epic cha
2. **User Story:** dạng "As a… I want… So that…" — trích **nguyên văn**, không diễn đạt lại
3. **Scope:** module/page/component bị ảnh hưởng. Ghi rõ cả phần **ngoài phạm vi** nếu tài liệu có nêu
4. **Acceptance Criteria:** gán `REQ-<MODULE>-<SỐ>` cho **từng rule đủ nhỏ để test độc lập** (theo mục 2 + 2.1)
5. **Dependencies:** ticket/feature được reference trong AC hoặc comment — đọc và tóm tắt
6. **Comments:** thường chứa quyết định nghiệp vụ mới nhất, **đè lên** phần mô tả gốc. KHÔNG được bỏ qua
7. **Attachments:** mockup, file CSV/XLSX đính kèm — xem bước 2
8. **Lịch sử thay đổi** (nếu có): AC nào được sửa gần đây → vùng rủi ro cao

#### Bước 2 — Khai thác file bảng (CSV/XLSX) — nguồn Field Spec tốt nhất

File bảng đính kèm thường **giá trị hơn cả phần mô tả**. Tìm và khai thác:

| Loại bảng | Dùng để sinh |
|---|---|
| Danh sách field/cột kèm kiểu dữ liệu, bắt buộc, độ dài | **Field Specifications** (mục 6.3) — gần như map 1:1 |
| Bảng thông báo lỗi / message key | **Validation Messages** (mục 6.4) |
| Ma trận role × chức năng | **Ma trận Phân quyền** (mục 6.5) |
| Bảng trạng thái × hành động | **Ma trận Trạng thái** (mục 6.6) |
| Data mẫu / test data | Đầu vào cho `/generate-test-data` — ghi chú lại, không đưa vào requirements |

Khi trích từ file bảng, **ghi rõ vị trí nguồn**: `<tên file> · sheet <tên> · dòng <n>`.

#### Bước 3 — Đối chiếu chéo các nguồn (BẮT BUỘC khi có ≥ 2 nguồn)

Lập bảng đối chiếu trước khi viết requirements:

| Hạng mục | Ticket mô tả | Comment | Mockup | File đính kèm | Kết luận |
|---|---|---|---|---|---|
| VD: độ dài tối đa của Tên | không nói | 255 | — | 200 | ⚠️ Xung đột → AMB |

**Thứ tự ưu tiên khi các nguồn mâu thuẫn** (dùng làm mặc định, luôn ghi rõ đã chọn nguồn nào và vì sao):

```
1. Comment/quyết định mới nhất có ghi ngày   ← mới nhất thắng
2. AC trong phần mô tả ticket                 ← cam kết chính thức
3. File đặc tả đính kèm (CSV/XLSX/DOCX)
4. Mockup/wireframe                           ← dễ lỗi thời nhất
```

❗ **Xung đột KHÔNG được tự giải quyết im lặng** — mọi mâu thuẫn đều phải thành một `AMB-XX` kèm Assumption tạm, kể cả khi đã chọn được nguồn ưu tiên.

#### Bước 4 — Xử lý tài liệu thiếu (rất hay gặp)

| Tình huống | Cách xử lý |
|---|---|
| Ticket **không có AC nào**, chỉ 1–2 dòng mô tả | ❌ KHÔNG tự viết AC thay PO. Ghi nhận đúng những gì có, rồi liệt kê **danh sách câu hỏi cần clarify** dạng `AMB-XX` mức 🔴 High. Nêu rõ trong Overview: *"Ticket chưa có AC — tài liệu này chưa đủ để sinh test case"* |
| AC viết dạng mơ hồ ("hoạt động đúng", "như module cũ") | Gán REQ ID bình thường **nhưng** kèm ngay 1 `AMB-XX` hỏi tiêu chí cụ thể |
| Tham chiếu "giống module X" | Nếu module X đã có `requirements_<X>.md` → trích REQ tương ứng và **link chéo**. Nếu chưa có → `AMB-XX`, không tự suy diễn |
| Thiếu hoàn toàn thông tin phân quyền/trạng thái | Ghi "Không đề cập trong tài liệu" (khác với "Không áp dụng") + `AMB-XX` |

### 3.3. Khi có cả tài liệu và UI thực tế

Chạy 3.2 trước để nắm **ý định**, rồi 3.1 để đối chiếu **thực tế**. Mọi lệch pha đều phải ghi nhận, phân loại rõ:

| Kiểu lệch | Nghĩa là | Xử lý |
|---|---|---|
| Tài liệu có, UI chưa có | Tính năng chưa build hoặc build thiếu | REQ vẫn giữ, ghi trạng thái `Chưa implement` |
| UI có, tài liệu không nói | Tính năng ngoài tài liệu (scope creep / di sản) | Vẫn gán REQ, nguồn ghi `UI thực tế — ngoài tài liệu` + `AMB-XX` |
| Cả hai có nhưng khác nhau | Xung đột thật | `AMB-XX` 🔴 High, ghi cả hai giá trị nguyên văn |

### 3.3.1. Tài liệu bán phần — chia vùng trước khi recon

Tình huống thực tế phổ biến nhất: QA đưa được **một phần** tài liệu (spec của 2/8 màn hình, file Excel chỉ có danh sách field, mockup cũ 6 tháng trước), phần còn lại phải tự khám phá.

Sai lầm hay gặp là xử lý cả module theo **một** cách: hoặc tin tài liệu rồi recon qua loa, hoặc bỏ tài liệu rồi recon lại từ đầu. Cả hai đều tốn công và đều sai.

**Bước 1 — Lập Bản đồ phủ tài liệu cho module, TRƯỚC khi mở browser:**

| Vùng chức năng | Tài liệu phủ | Mức phủ | Chiến lược recon |
|---|---|---|---|
| Form tạo mới | `field_spec.xlsx` sheet "Customer" dòng 3–28 | 🟩 Đầy đủ | **Đối chiếu** — mở form kiểm từng dòng spec, tìm lệch |
| Danh sách & lọc | Mockup `list_v1.png` (2026-02) | ⚠️ Nghi lỗi thời | **UI thắng** — recon đầy đủ, mọi lệch ghi `AMB` |
| Phân quyền | — | ⬜ Trắng | **Recon đầy đủ** như mode UI thuần |
| Import Excel | Ticket ABC-12 mô tả 3 dòng | 🟨 Một phần | **Bổ khuyết** — tài liệu cho luồng chính, recon lấy validation + error |

**Bước 2 — Recon theo đúng chiến lược từng vùng:**

| Mức phủ | Việc phải làm | Việc được rút gọn |
|---|---|---|
| 🟩 **Đầy đủ** | Mở màn hình, kiểm **từng** rule tài liệu nêu có đúng trên UI không. Lệch → `AMB` | Không cần dò lại từ đầu những rule đã ghi rõ và đã khớp |
| 🟨 **Một phần** | Recon **đầy đủ** phần tài liệu không nói (thường là: validation message, edge case, phân quyền) | Phần tài liệu đã nêu rõ và khớp UI → trích dẫn, không mô tả lại |
| ⬜ **Trắng** | Recon đầy đủ 9 bước mục 3.1 | Không rút gọn gì |
| ⚠️ **Nghi lỗi thời** | Recon đầy đủ như ⬜. Tài liệu chỉ dùng làm **danh sách câu hỏi**: "chỗ này tài liệu nói X, thực tế thế nào?" | Không rút gọn gì |

**Bước 3 — Ghi nguồn phân biệt được 4 mức** (cột `Nguồn` của mỗi REQ):

| Giá trị cột `Nguồn` | Nghĩa |
|---|---|
| `Tài liệu · <file> · <vị trí>` | Chỉ có trong tài liệu, **chưa** mở UI kiểm |
| `Tài liệu + kiểm chứng thực tế` | Tài liệu nêu, đã mở UI xác nhận khớp — **mức tin cậy cao nhất** |
| `UI thực tế` / `Kiểm chứng thực tế` | Chỉ có ở UI, tài liệu không nói |
| `API · <method> <endpoint>` | Lấy từ tầng network (mục 3.1.1) |

**Luật cứng của nhánh này:**
- ❌ **Không** dùng tài liệu bán phần để suy ra vùng nó không phủ. Spec form tạo mới **không** nói gì về form sửa — dù hai form nhìn giống nhau
- ❌ Vùng ⬜ Trắng **không** được rút gọn recon với lý do "module này chắc giống module kia"
- ✅ **Bản đồ phủ tài liệu phải nằm trong tài liệu đầu ra** (một mục ngay sau Tổng quan) — người đọc cần biết phần nào tin được từ spec, phần nào do agent quan sát
- ✅ Tài liệu ⚠️ nghi lỗi thời mà **khớp** UI → nâng lên `Tài liệu + kiểm chứng thực tế`; **lệch** → `AMB-XX` kèm nguyên văn cả hai bên
- ✅ Khi `/discover-system` đã chạy: đọc `docs/requirements/_discovery/doc_inventory.md` — Bản đồ phủ tài liệu cấp hệ thống đã có sẵn ở đó, chỉ cần chi tiết hoá xuống cấp vùng chức năng

---

## 4. Framework Phát Hiện Ambiguity & Risk

### 4.1. Ambiguity (AMB-XX)

Với mỗi ambiguity, ghi: **Mã · Câu hỏi · Nguy cơ nếu không giải quyết · Mức độ (🔴 High / 🟡 Medium / 🟢 Low) · Assumption tạm** (nếu không được trả lời thì test theo giả định nào).

Các hướng phát hiện:
- Từ khóa mơ hồ: "where applicable", "as needed", "similar to", "hợp lý", "v.v."
- Validation rules thiếu: min/max, format, required/optional, giá trị mặc định
- Hành vi edge case chưa quy định: lỗi mạng, concurrent access, data rỗng, timeout
- Inconsistency giữa document và mockup/UI thực tế (tên cột, format, layout)
- Threshold/config chưa xác định (VD: bao nhiêu ngày = "sắp đến hạn"?)
- Phân quyền chưa rõ: role nào được thực hiện hành động này?
- Trạng thái chưa rõ: từ trạng thái X có được chuyển sang Y không?

### 4.2. Risk (RISK-XX)

Với mỗi risk, ghi: **Mã · Tên rủi ro · Mô tả · Mitigation** (cách giảm thiểu khi test).

---

## 4.3. Cổng Tự Soát REQ (BẮT BUỘC — chạy trước khi ghi file)

> **Vì sao có mục này.** Ambiguity (4.1) bắt cái **tài liệu/hệ thống không nói rõ**. Mục này bắt cái **chính agent vừa viết sai** — loại lỗi nguy hiểm hơn vì nó *trông như* đã kiểm chứng, có số liệu DOM hẳn hoi, và không workflow nào phía sau phát hiện được. Test case sinh ra từ một AC sai sẽ đỏ mà không ai biết lỗi nằm ở tài liệu.
>
> Rà **từng REQ** qua 9 câu hỏi dưới. Câu nào trả lời "không" → sửa REQ, không phải mở AMB.

### 4.3.1. Tên REQ và AC phải là **cùng một mệnh đề**

Tên REQ là thứ người viết test case đọc trước tiên — và nhiều người chỉ đọc tên. Tên hứa một đằng, AC đo một nẻo là lỗi nặng nhất của tài liệu requirements.

| ❌ Sai | Vì sao | ✅ Sửa |
|---|---|---|
| Tên: *"Không giữ lại email sau khi đăng nhập lỗi"* · AC: *"HTML trả về không có thuộc tính `value`"* | Người dùng **vẫn thấy** email vì trình duyệt tự điền. Tên mô tả hành vi người dùng, AC đo tầng HTML — hai thứ khác nhau | Đổi tên thành *"Máy chủ không trả lại giá trị email…"* và ghi rõ **cấm** assert ô rỗng trên màn hình |
| Tên: *"Hai lối đăng xuất"* · AC: *"tồn tại 2 phần tử `li.header-logout`"* | Đếm node DOM ≠ người dùng dùng được 2 lối | Kiểm khả dụng thật (4.3.5) rồi đặt tên theo kết quả |

**Luật:** đọc tên REQ, tự hỏi *"AC này có chứng minh đúng câu vừa đọc không?"* Không → sửa **tên** hoặc sửa **AC**, không để lệch.

### 4.3.2. Khẳng định phủ định phải kiểm bằng **trạng thái sạch**

Mệnh đề dạng *"không sinh X"* · *"không gửi Y"* · *"không lưu Z"* **không** chứng minh được bằng cách quan sát "giá trị không đổi".

```
❌ SAI:  Đăng nhập lần 2 → giá trị cookie không đổi → kết luận "không cấp cookie mới"
         (không phân biệt được: không cấp · cấp lại trùng giá trị · không đụng tới cái cũ)

✅ ĐÚNG: (1) xoá sạch tác tạo → (2) xác nhận đã sạch → (3) thực hiện hành động
         → (4) kiểm tác tạo có xuất hiện không
```

AC phải ghi **đủ 4 bước theo thứ tự**, để người khác chạy lại được. Thiếu bước (2) là phép thử vô hiệu — trạng thái sót từ lần chạy trước sẽ làm nhiễu.

### 4.3.3. AC không được chứa giá trị **biến thiên theo phiên**

Token · session id · CSRF token · id bản ghi vừa tạo · timestamp · số thứ tự tự tăng — chép giá trị cụ thể vào AC gây **hai** hậu quả:

1. Test case sinh ra hardcode giá trị đó → hỏng ngay lần chạy sau
2. Nếu là giá trị bí mật → **phát tán credential** (xem 7.1)

**Luật:** ghi **hình thái**, không ghi giá trị. `<16 ký tự hex>` · `a:2:{s:7:"user_id";s:1:"<id>";…}` · `độ dài = 32`. Cần khẳng định tính duy nhất thì mô tả **quan hệ** (*"khác giá trị trước đó"*), không chép cả hai giá trị.

### 4.3.4. Một REQ = **một** rule kiểm được độc lập

Dấu hiệu nhận biết REQ đang gộp: tên chứa **"và"**, **"hoặc"**, dấu phẩy nối hai mệnh đề; hoặc AC dùng **một** input để kiểm **nhiều** rule cùng lúc.

```
❌ "Email không phân biệt hoa thường VÀ bỏ qua khoảng trắng"
   AC: nhập "  ADMIN@Example.COM  " → đăng nhập được
   → fail thì không biết rule nào hỏng

✅ REQ-a: không phân biệt hoa thường  → input "ADMIN@Example.COM"
   REQ-b: bỏ qua khoảng trắng đầu/cuối → input "  admin@example.com  "
```

Tách ra thì **giữ nguyên mã cũ** cho rule ở lại, cấp mã mới cho phần tách (mục 2.1) — không đánh lại cả cụm.

### 4.3.5. Tồn tại trong DOM ≠ **người dùng dùng được**

Đây là cái bẫy lớn nhất khi recon bằng `browser_evaluate`. `querySelectorAll(...).length === 2` **không** có nghĩa người dùng có 2 lựa chọn: phần tử có thể bị ẩn theo responsive, bị `display:none` ở tổ tiên, nằm ngoài luồng bố cục, hoặc bị phần tử khác che.

**Trước khi khai một phần tử là "người dùng thao tác được", BẮT BUỘC đọc thêm:**

| Kiểm | Cách đọc | Ý nghĩa khi sai |
|---|---|---|
| Có được render không | `el.offsetParent === null` | `null` → **không** render (hoặc `position:fixed`) |
| Kích thước thật | `getBoundingClientRect()` | `0×0` → không bấm được |
| Tổ tiên nào ẩn | Duyệt ngược `parentElement`, đọc `getComputedStyle().display` | Chỉ ra **lý do** ẩn: responsive · quyền · trạng thái |
| Viewport đang đo | `innerWidth × innerHeight` | Kết luận chỉ đúng với **viewport đó** — phải ghi vào AC |

Phần tử chỉ hiện ở viewport khác → **không** kết luận thay cho viewport đó. Mở một `AMB-XX` yêu cầu recon riêng, đừng suy diễn.

**Kèm theo — bẫy locator do cấu trúc lồng:** trước khi ghi *"mục cuối cùng của X"*, đếm phần tử **lồng bên trong**. `X li:last-child` bắt cả `<li>` lồng ở submenu; `X > li:last-child` mới là con trực tiếp. Ghi rõ dấu `>` vào AC, kèm số phần tử lồng đếm được.

### 4.3.6. Chuỗi do **trình duyệt / hệ điều hành** sinh không phải hành vi ứng dụng

Thông báo validation mặc định của HTML5, tiêu đề trang lỗi mặc định, hộp thoại `alert/confirm` gốc, định dạng ngày theo locale — tất cả **đổi theo trình duyệt, phiên bản và ngôn ngữ hệ điều hành**.

**Luật:** được phép ghi lại nguyên văn để tham khảo, nhưng **phải** kèm tên + phiên bản trình duyệt đã quan sát, và **ghi rõ cấm dùng làm assertion**. AC chỉ được assert phần ứng dụng kiểm soát (`checkValidity() === false`, request có được gửi đi không).

Bảng metadata của tài liệu (6.1) **phải** có dòng **Trình duyệt khảo sát** ghi rõ tên + viewport.

### 4.3.7. Không assert khớp tuyệt đối lên giá trị có **phần động**

`document.title` mang tiền tố đếm thông báo · `body.className` mang token theo trình duyệt hoặc theo cấu hình · id sinh động · text có chèn số đếm.

**Luật:** đọc **giá trị đầy đủ** rồi tự hỏi *"phần nào có thể đổi giữa hai lần chạy hoặc hai môi trường?"* AC ghi `**chứa** <phần bất biến>` kèm ghi chú phần nào là động và vì sao. Ghi `= "<giá trị hôm nay>"` là bẫy cho lượt sinh TC.

### 4.3.8. Tính năng sinh ra **tác tạo** phải kiểm chứng tác tạo **có tác dụng**

Cookie ghi nhớ · email gửi đi · tệp xuất ra · bản ghi lịch sử · thông báo đẩy — recon rất dễ dừng ở *"đã được tạo ra"* rồi coi là xong. Nhưng **giá trị nghiệp vụ nằm ở chỗ nó dùng được**, và đó mới là thứ có thể hỏng âm thầm.

**Luật:** mỗi tác tạo cần **tối thiểu 2 REQ** — một cho *được tạo đúng hình thái*, một cho *dùng được đúng mục đích*. Không kiểm chứng được vế thứ hai thì vẫn **phải** cấp REQ ở trạng thái ⚪ kèm `AMB-XX`, tuyệt đối không im lặng bỏ qua. Im lặng bỏ qua khiến cả tính năng không có test case nào mà bảng độ phủ vẫn báo xanh.

### 4.3.9. Mã trạng thái / hành vi kỹ thuật bất thường phải **nghi ngờ nhất quán**

Mã HTTP lệch quy ước (chuyển hướng xác thực trả `307` thay vì `302/303`; body ghi `419` nhưng header trả `403`), phản hồi rỗng, `500` ở luồng người dùng chạm tới — đều là dấu hiệu.

**Luật:** đã mở AMB cho một bất thường thì **mọi** bất thường cùng loại trong tài liệu cũng phải được đối xử như vậy. Nghi ngờ chỗ này mà chép nguyên chỗ kia là mâu thuẫn nội tại. Chưa loại trừ được khả năng do **công cụ đo** ghi nhận sai thì AC không được assert giá trị đó — assert cái quan sát được chắc chắn (điểm dừng, nội dung hiển thị).

### 4.3.10. Bảng kiểm nhanh trước khi ghi file

- [ ] Mỗi REQ: tên và AC **cùng một mệnh đề** (4.3.1)
- [ ] Mọi khẳng định phủ định có **phép thử trạng thái sạch đủ 4 bước** (4.3.2)
- [ ] Không AC nào chứa token / id / timestamp **cụ thể** (4.3.3)
- [ ] Không REQ nào có "và"/"hoặc" nối hai rule trong tên (4.3.4)
- [ ] Mọi phần tử khai là "dùng được" đã đọc `offsetParent` + kích thước + viewport (4.3.5)
- [ ] Mọi chuỗi của trình duyệt đã ghi tên trình duyệt + đánh dấu cấm assert (4.3.6)
- [ ] Mọi giá trị có phần động dùng `chứa`, không dùng `=` (4.3.7)
- [ ] Mọi tác tạo có đủ REQ *tạo ra* **và** REQ *dùng được* (4.3.8)
- [ ] Bất thường kỹ thuật được nghi ngờ nhất quán trong toàn tài liệu (4.3.9)

---

## 5. Quy Mô Tài Liệu & Quy Tắc Tách File (Scaling Rule)

Module lớn sinh ra tài liệu dài, khó review và khó chia việc. Ngược lại, tách quá sớm sẽ **làm vỡ các hạng mục cắt ngang** (ma trận phân quyền, ma trận trạng thái, AMB/RISK liên module). Quy tắc dưới đây quyết định dựa trên **số REQ**, không dựa trên dung lượng file.

### 5.1. Bảng ngưỡng (BẮT BUỘC áp dụng)

Đếm tổng số REQ **sau khi hoàn tất recon**, trước khi ghi file:

| Số REQ | Cấu trúc đầu ra | Mục Phân rã Epic/Story |
|---|---|---|
| **< 25** | 1 file `requirements_<module>.md` | ❌ Không cần |
| **25 – 80** | 1 file `requirements_<module>.md` | ✅ **BẮT BUỘC** (mục 6.8) |
| **> 80** hoặc thoả điều kiện tại 5.2 | Tách nhiều file theo cấu trúc 5.3 | ✅ **BẮT BUỘC** ở file index |

### 5.2. Điều kiện tách bắt buộc (kể cả khi < 80 REQ)

Tách ngay nếu gặp **bất kỳ** dấu hiệu nào sau:

- Module chứa **≥ 2 entity nghiệp vụ độc lập** có vòng đời riêng (VD: `Customer` và `Contact`, `Project` và `Task`)
- Phạm vi khảo sát mở rộng sang **sub-module/tab con** có nghiệp vụ riêng (VD: phân tích cả 17 tab của Project)
- Có **≥ 2 ma trận trạng thái** khác nhau trong cùng tài liệu
- Người dùng nêu rõ team làm việc theo Jira và cần chia việc theo Story

### 5.3. Cấu trúc thư mục (áp dụng cho MỌI module, tách hay không tách)

**Mỗi module một thư mục riêng.** Layout chuẩn của repo:

```
docs/requirements/
├── README.md                              ← DANH MỤC toàn hệ thống (mục 5.7)
├── <module>/
│   ├── requirements_<module>.md           ← INDEX — TÊN FILE BẤT BIẾN
│   ├── evidence/                          ← ảnh chụp màn hình làm bằng chứng
│   │   └── *.png
│   ├── stories/                           ← CHỈ khi tài liệu bị tách (ngưỡng mục 5.1)
│   │   ├── story_01_<slug>.md
│   │   └── story_02_<slug>.md
│   ├── analysis/                          ← phân tích ticket của RIÊNG module này
│   │   └── analysis_<TICKET-ID>.md
│   └── impact/                            ← Impact Report của /update-requirements-from-ticket
│       └── impact_<TICKET-ID>.md          ← input BẮT BUỘC cho /update-testcases-from-impact
```

**Quy tắc thư mục — bất biến:**

| Quy tắc | Lý do |
|---|---|
| Tên file index **luôn** `requirements_<module>.md` | Mọi workflow phía sau đọc theo `docs/requirements/<module>/requirements_<module>.md`. Đổi tên là vỡ chuỗi (mục 5.5) |
| Tên thư mục = tên module, chữ thường, không dấu | Tra cứu bằng glob `docs/requirements/*/requirements_*.md` |
| Evidence nằm **trong** thư mục module | Tài liệu và bằng chứng đi cùng nhau |
| Phân tích ticket nằm ở `<module>/analysis/` | Giữ liên kết ticket ↔ module. **KHÔNG** đặt ở thư mục toàn cục |
| Thêm module = thêm thư mục + 1 dòng ở danh mục | Không đụng gì khác trong repo |

**Phân chia nội dung khi tách — quy tắc cứng:**

| Nội dung | Đặt ở đâu | Lý do |
|---|---|---|
| Bảng metadata, Tổng quan, Phạm vi | **Index** | Điểm vào duy nhất |
| Bảng phân rã Epic/Story + ánh xạ REQ | **Index** | Bản đồ điều hướng toàn module |
| Ma trận phân quyền | **Index** | Cắt ngang mọi Story |
| Ma trận trạng thái | **Index** | Cắt ngang mọi Story |
| Bảng Ambiguity & Risk | **Index** | Đánh số theo **toàn module**, không đánh lại theo từng file |
| Yêu cầu phi chức năng | **Index** | Áp cho toàn module |
| Bảng REQ chi tiết của từng Story | **File story** | Đơn vị chia việc |
| Đặc tả trường dữ liệu (Field Spec) | **File story** tương ứng | Đi kèm form/màn hình của Story đó |
| Validation messages | **File story** tương ứng | Gắn trực tiếp với REQ trong Story |
| User Flow | **File story** tương ứng | Mỗi flow thuộc đúng 1 Story |

### 5.4. Bất biến khi tách (Invariants — kiểm tra trước khi bàn giao)

- [ ] **REQ ID giữ nguyên tuyệt đối** — không đánh lại số, không đổi prefix
- [ ] **Mỗi REQ thuộc đúng 1 Story** — không REQ mồ côi, không REQ nằm ở 2 Story
- [ ] **Tổng REQ trong các Story = tổng REQ đã sinh** — nêu rõ con số trong index để tự kiểm chứng
- [ ] **AMB/RISK đánh số toàn module**, chỉ đặt ở index; file story chỉ *tham chiếu*, không nhân bản nội dung
- [ ] **Hạng mục cắt ngang không bị nhân bản** vào file story
- [ ] Mỗi file story có link ngược về index; index có link tới mọi file story

### 5.5. Hợp đồng đọc cho các workflow phía sau (Consumer Contract)

Tách file **không được** làm vỡ chuỗi `/generate-testcases-manual-rbt` → `/generate-traceability-matrix` → `/generate-automation-from-testcases`. Vì vậy:

- **Đường dẫn index LUÔN là:**
  ```
  docs/requirements/<module>/requirements_<module>.md
  ```
  Bất kể tài liệu có tách hay không. Mọi workflow phía sau chỉ cần biết đúng một mẫu đường dẫn này — tra được bằng glob `docs/requirements/*/requirements_*.md`.
- **Điểm vào cấp hệ thống là `docs/requirements/README.md`** — khi không biết module nào tồn tại hoặc prefix nào đã dùng, đọc file này trước (mục 5.7).
- Index **BẮT BUỘC** có mục `## Bản đồ tài liệu` liệt kê đầy đủ file con kèm dải REQ mà file đó chứa:
  ```markdown
  ## Bản đồ tài liệu
  | File | Story | REQ bao phủ |
  |---|---|---|
  | [stories/story_01_danh_sach.md](stories/story_01_danh_sach.md) | STORY-PRJ-01 | REQ-PRJ-01 → REQ-PRJ-13 |
  ```
- Workflow phía sau đọc index trước, thấy có bản đồ thì đọc tiếp các file con; không có bản đồ thì hiểu là tài liệu 1 file.

### 5.6. Ưu tiên convention sẵn có

Trước khi quyết định tách, **đọc `docs/requirements/README.md` và xem các module đã có**. Nếu repo đang theo một convention khác với mục 5.3, **giữ nguyên convention đó** và chỉ bổ sung mục Phân rã Epic/Story — trừ khi người dùng yêu cầu đổi. Nhất quán trong repo quan trọng hơn ngưỡng lý thuyết.

### 5.7. Danh mục toàn hệ thống — `docs/requirements/README.md`

#### 5.7.1. Khởi tạo khi chưa tồn tại (BẮT BUỘC — dự án mới)

> ⚠️ **Đây là bước dễ bị bỏ sót nhất khi bắt đầu một dự án mới.** Không có file danh mục thì bước 1 của mục 2.1 (đọc prefix đã chiếm + mã kế tiếp) **im lặng không chạy** — cơ chế chống trùng mã và nối tiếp REQ mất tác dụng mà không có cảnh báo nào.

**Trước khi ghi tài liệu module ĐẦU TIÊN của một dự án:**

1. Kiểm tra `docs/requirements/README.md` có tồn tại không
2. **Nếu CHƯA có** → tạo ngay từ template ở mục 5.7.2, với bảng danh mục **rỗng** (chỉ có dòng tiêu đề)
3. Ghi tài liệu module
4. Bổ sung dòng đầu tiên vào bảng danh mục

Áp dụng y hệt cho `docs/testcases/README.md` ở nhánh test case (xem skill `skills-rbt-manual-testing`).

#### 5.7.2. Nội dung bắt buộc

File danh mục là **điểm vào cấp hệ thống**, phải được cập nhật mỗi khi có module mới hoặc REQ mới. Nội dung bắt buộc:

| Mục | Nội dung |
|---|---|
| **1. Bảng danh mục module** | `Module · Prefix · Trạng thái recon · Mức phủ tài liệu · Tài liệu · REQ đã dùng · Mã kế tiếp · AMB treo · Story · Cập nhật` |
| **Trạng thái recon** | `⬜ Chưa khảo sát` · `🟨 Đang khảo sát` · `✅ Đã có tài liệu` · `⏸️ Hoãn` · `⚪ Chưa implement` — module do `/discover-system` phát hiện vào danh mục ngay ở mức ⬜, chưa cần có tài liệu |
| **Danh sách prefix đã chiếm** | Module mới **phải** chọn prefix chưa có trong danh sách — chống trùng mã giữa các module |
| **2. Trạng thái REQ toàn hệ thống** | Tổng hợp 🟢/🟡/🔴/⚪ theo từng module |
| **3. Ambiguity 🔴 High còn treo** | Danh sách chặn tiến độ, gom từ mọi module — để đưa PO một lần thay vì hỏi lẻ tẻ |
| **4. Cấu trúc thư mục chuẩn** | Copy từ mục 5.3 |
| **5. Quy trình sử dụng** | Bảng: tình huống → workflow → ghi vào đâu |
| **6. Nhật ký danh mục** | Thay đổi cấp cấu trúc (thêm module, đổi layout) |

**Ba việc file này làm được mà thư mục không làm được:**
1. Đọc **một file** là nắm toàn cảnh, không phải quét thư mục rồi mở từng tài liệu
2. **Chống trùng prefix** giữa các module
3. Gom ambiguity 🔴 High của mọi module về một chỗ — thấy ngay vấn đề lặp lại (VD: cả 3 module đều thiếu tài khoản role thấp)

### 5.8. Tầng khám phá — `docs/requirements/_discovery/`

Skill này làm việc ở **cấp module**: đã biết module tên gì, ở đâu, thì recon và sinh REQ. Nhưng với hệ thống **không có tài liệu**, câu hỏi đầu tiên lại là *"hệ thống này có những module nào?"* — đó là việc của tầng khám phá, do workflow `/discover-system` đảm nhiệm.

```
docs/requirements/
├── README.md              ← danh mục (5.7) — do /discover-system khởi tạo
├── _discovery/            ← TẦNG KHÁM PHÁ — cấp hệ thống
│   ├── system_map.md      ← INDEX BẤT BIẾN — bản đồ module · entity · phụ thuộc · risk
│   ├── modules/           ← chỉ khi > 8 module (xem 5.8.1)
│   │   └── module_NN_<slug>.md
│   ├── doc_inventory.md   ← bản đồ phủ tài liệu (khi QA có đưa tài liệu)
│   ├── sources/           ← bản gốc tài liệu QA cung cấp
│   └── evidence/          ← 1 ảnh tổng quan mỗi module
└── <module>/              ← TẦNG MODULE — skill này làm việc ở đây
```

**Phân vai — không được lấn:**

| | Tầng khám phá (`/discover-system`) | Tầng module (skill này) |
|---|---|---|
| Đơn vị làm việc | Cả hệ thống | Một module |
| Cấp mã | **Prefix** (`CUST`) | **Số REQ** (`REQ-CUST-01`) |
| Độ sâu | Module tồn tại · loại màn hình · có CRUD không | Từng field · từng rule · từng message |
| Evidence | 1 ảnh/module, chứng minh module có thật | Chuẩn đầy đủ mục 7.2.1 |
| Đầu ra | `_discovery/` + `README.md` | `<module>/requirements_<module>.md` |

> ❌ Tầng khám phá **không được** sinh mã `REQ-XXX-NN`. Mã REQ là bất biến (mục 2); cấp mã khi chưa mở form, chưa trigger validation thì chắc chắn phải đánh lại số — đúng thứ mục 2.1 cấm.

**Cách skill này dùng tầng khám phá — làm ở đầu mỗi lần recon module:**

1. Có `_discovery/system_map.md` → đọc phần của module sắp recon: route · loại màn hình · số tab · risk · vùng chưa xác minh. Tiết kiệm được toàn bộ bước dò đường
2. Có `_discovery/doc_inventory.md` → lấy Bản đồ phủ tài liệu của module, chi tiết hoá xuống cấp vùng chức năng theo mục **3.3.1**
3. Prefix **lấy từ danh mục**, không tự đặt lại — kể cả khi thấy prefix cũ đặt chưa hay
4. Recon xong → cập nhật cột `Trạng thái recon` trong `README.md` sang ✅, và ghi 1 dòng vào Nhật ký khám phá của `system_map.md` nếu phát hiện lệch so với bản đồ

**Không có `_discovery/` thì sao?** Vẫn recon module bình thường — tầng khám phá là bước tăng tốc, không phải điều kiện bắt buộc. Nhưng nếu user hỏi *"hệ thống có gì"* thay vì *"module X có gì"* → **route sang `/discover-system`**, đừng tự crawl rồi sinh REQ lẫn lộn hai tầng.

#### 5.8.1. Bản đồ khám phá bị tách file — cách đọc

Hệ thống > 8 module thì `/discover-system` tách bản đồ thành nhiều file. Quy tắc đọc **giống hệt** hợp đồng ở mục 5.5:

- **Điểm vào luôn là `docs/requirements/_discovery/system_map.md`** — tên bất biến, dù tách hay không
- Index có mục `## Bản đồ tài liệu` → đọc tiếp file trong `modules/` để lấy chi tiết module cần recon. Không có mục đó → hiểu là bản đồ 1 file
- **Một file `modules/module_NN_<slug>.md` có thể chứa nhiều module** khi chúng quan hệ chặt (VD `module_01_dang_nhap_phan_quyen.md` chứa cả `LOGIN` · `USER` · `ROLE`)

⚠️ **Gộp file KHÔNG gộp prefix, cũng KHÔNG gộp tài liệu requirements.** Ba module nằm chung một file khám phá vẫn sinh ra **ba** thư mục và **ba** file `requirements_<module>.md` riêng ở tầng module. Bản đồ khám phá gộp là để **đọc cho gọn**; ranh giới truy vết vẫn là **prefix**, không phải file.

| | Tầng khám phá | Tầng module |
|---|---|---|
| `LOGIN` · `USER` · `ROLE` | 1 file `module_01_dang_nhap_phan_quyen.md` | 3 file `login/requirements_login.md` · `user/…` · `role/…` |

Bản đồ khám phá **không mang mã REQ** (mục 5.8), nên tách/gộp lại về sau là thao tác an toàn — khác hẳn tài liệu requirements, nơi REQ ID bất biến ràng buộc mọi thứ.

---

## 6. Cấu trúc Tài liệu Yêu cầu Đầu ra (Output Format)

> ### ⚠️ Phạm vi áp dụng — đọc trước khi dùng mục này
>
> Skill có **3 template đầu ra khác nhau** tuỳ workflow. Chọn đúng một cái, không trộn:
>
> | Workflow | Template dùng | Ghi chú |
> |---|---|---|
> | `/generate-requirements-from-website` | **Mục 6 này** — tài liệu đặc tả module | Đầu ra là `requirements_<module>.md` |
> | `/analyze-requirement-document` | **Template 10 mục của chính command đó** | Đầu ra là `analysis_<TICKET-ID>.md` — tài liệu phân tích ticket, KHÁC tài liệu đặc tả module |
> | `/discover-system` | **Template của chính command đó** (Bước 6) | Đầu ra là `_discovery/system_map.md` + `README.md` — cấp hệ thống, **KHÔNG có mã REQ**. Xem mục **5.8** |
>
> Nhánh `/analyze-requirement-document` chỉ **mượn** từ skill các mục: **2 + 2.1** (đánh mã), **3.2** (quy trình trích xuất), **4 + 4.3** (AMB/RISK + Cổng Tự Soát REQ), **5** (scaling), **7.1 + 7.3** (strict rules) — **KHÔNG** dùng cấu trúc mục 6.
>
> ⚠️ **Mục 4.3 áp cho MỌI nhánh sinh REQ, không có ngoại lệ.** REQ sinh từ tài liệu cũng lệch tên/AC, cũng gộp nhiều rule vào một mã, cũng chép giá trị biến thiên — y hệt REQ sinh từ UI.
>
> Nhánh `/discover-system` mượn: **2** (quy ước prefix), **3.1 + 3.1.1** (crawl UI + network), **3.1.2** (phân quyền sơ bộ), **5.3 + 5.7 + 5.8** (thư mục, danh mục, tầng khám phá), **7.1 + 7.2 + 7.2.1** — **KHÔNG** dùng mục 6 và **KHÔNG** cấp số REQ. Không sinh REQ nên không chạy 4.3, nhưng **vẫn phải** theo luật evidence (7.2.1) và luật cấm ghi giá trị bí mật (7.1).
>
> Riêng 2 dòng metadata `Dải mã đã dùng` / `Mã kế tiếp` ở mục 6.1 là **bắt buộc với cả hai template**, vì quy tắc nối tiếp mã (2.1) cần chúng để hoạt động.

Tài liệu format Markdown, lưu artifact (`requirements_<module>.md`). **Nội dung bắt buộc:**

### 6.1. Bảng metadata + Tổng quan (Overview)

Mở đầu tài liệu bằng bảng metadata, trong đó **BẮT BUỘC** có 2 dòng phục vụ quy tắc nối tiếp mã (mục 2.1):

```markdown
| **Dải mã đã dùng** | `REQ-PRJ-01` → `REQ-PRJ-65` · `AMB-01` → `AMB-12` · `RISK-01` → `RISK-12` |
| **Mã kế tiếp**     | Đợt phân tích sau bắt đầu từ `REQ-PRJ-66` · `AMB-13` · `RISK-13` — **KHÔNG đánh lại từ 01** |
```

Với **nhánh UI Recon**, bảng metadata còn **BẮT BUỘC** thêm một dòng nữa:

```markdown
| **Trình duyệt khảo sát** | Google Chrome (Playwright MCP), viewport `1600×750`. Mọi AC dựa trên thông báo mặc định của trình duyệt **chỉ đúng với trình duyệt này** |
```

Thiếu dòng này thì không ai biết AC nào phụ thuộc trình duyệt, và mọi kết luận về hiển thị/responsive mất ngữ cảnh viewport — xem mục **4.3.6** và **4.3.5**.

Sau bảng metadata: mô tả tóm tắt tính năng, mục đích, phạm vi module (trong phạm vi / ngoài phạm vi).

### 6.2. Yêu cầu Chức năng (Functional Requirements) — CÓ MÃ REQ
Chia thành User Stories / Use Cases, **mỗi yêu cầu 1 dòng trong bảng có mã**:

| REQ ID | Tên yêu cầu | Mô tả | Acceptance Criteria | Trạng thái | Cập nhật lần cuối | Nguồn |
|---|---|---|---|---|---|---|
| REQ-LOGIN-01 | Đăng nhập bằng email | Là người dùng, tôi muốn... | Nhập đúng email+password → vào Dashboard | 🟢 | — | UI thực tế / AC#1 |
| REQ-LOGIN-02 | Khóa account sau 5 lần sai | ... | ... | 🟡 | 2026-08-10 · ABC-123 | AC#3 |

**Bảng mã trạng thái REQ:**

| Ký hiệu | Nghĩa | Hệ quả với test case |
|---|---|---|
| 🟢 | **Active** — đang hiệu lực, chưa từng sửa | TC giữ nguyên |
| 🟡 | **Changed** — đã bị ticket sau sửa nội dung | ⚠️ TC map vào REQ này **phải review lại** |
| 🔴 | **Deprecated** — tính năng đã bị gỡ bỏ | TC map vào REQ này **phải xoá/archive**. KHÔNG xoá dòng REQ, chỉ đổi trạng thái |
| ⚪ | **Chưa implement** — tài liệu có, hệ thống chưa build | TC viết trước, đánh dấu `skip` cho tới khi build xong |

**Quy tắc:**
- Mặc định mọi REQ mới là 🟢, cột `Cập nhật lần cuối` để `—`
- Trạng thái ≠ 🟢 thì **BẮT BUỘC** có dòng tương ứng trong Nhật ký thay đổi (mục 6.9) — hai nơi này phải khớp nhau
- **KHÔNG BAO GIỜ xoá dòng REQ** khỏi tài liệu, kể cả khi tính năng bị gỡ. Xoá dòng là mất dấu vết, và mã REQ đó cũng không được tái sử dụng (mục 2.1)
- Với tài liệu **đã phát hành trước khi có schema này**: không cần sửa lại toàn bộ dòng. Ghi một dòng quy ước dưới bảng — *"REQ không ghi trạng thái = 🟢 Active"* — rồi chỉ đánh dấu những REQ thực sự thay đổi về sau

### 6.3. Đặc tả Trường Dữ liệu (Field Specifications)
Bảng chi tiết từng field — phần cốt lõi cho Tester:

| Field (Label) | Loại UI | Required | Ràng buộc (min/max/format/default) | REQ liên quan | Ghi chú |
|---|---|---|---|---|---|

### 6.4. Business Rules & Validation Messages
| REQ ID | Rule / Trigger | Thông báo lỗi mong đợi (nguyên văn) |
|---|---|---|

### 6.5. Ma trận Phân quyền (nếu có nhiều role)

| Hành động | Admin | Manager | Staff | Guest |
|---|---|---|---|---|
| Xem danh sách | ✅ | ✅ | ⚠️✅ | ❌ |
| Xóa bản ghi | ✅ | ⚠️❌ | ❔ | ❌ |

**Ký hiệu mức bằng chứng** (mục 3.1.2) — bắt buộc dùng khi không đủ account cho mọi role:

| Ký hiệu | Nghĩa |
|---|---|
| `✅` / `❌` | **Đã kiểm chứng** — đăng nhập đúng role và thử thật |
| `⚠️✅` / `⚠️❌` | **Suy từ cấu hình** — đọc từ màn hình phân quyền của hệ thống, chưa đăng nhập thử |
| `❔` | **Chưa có căn cứ** — bắt buộc kèm `AMB-XX` 🔴 |

**Dòng tổng bắt buộc đặt ngay dưới bảng:**
```
Tổng 21 ô = Đã kiểm chứng 13 · Suy diễn 0 · Chưa rõ 7 · Không áp dụng 1
Ô "không áp dụng" là [<hành động> × <role>] — <lý do>. Role Staff/Guest chưa có account (AMB-07)
```

Không có dòng tổng thì người đọc không biết ma trận này tin được bao nhiêu phần. Ô `❔` **không được** làm tròn thành `❌`.

⚠️ **Các nhóm phải cộng lại đúng bằng `số hàng × số cột`.** Ô đánh `—` (không áp dụng) cũng là một nhóm phải đếm và **phải giải thích lý do** — bỏ nó ra ngoài khiến tổng không khớp, và người đọc không phân biệt được "đã cân nhắc, không liên quan" với "quên mất".

### 6.5.1. Bản đồ phủ tài liệu — bắt buộc khi có tài liệu bán phần (mục 3.3.1)

Đặt ngay sau Tổng quan (6.1). Người đọc phải biết phần nào tin được từ spec, phần nào do agent quan sát:

| Vùng chức năng | Tài liệu phủ | Mức phủ | REQ liên quan |
|---|---|---|---|
| Form tạo mới | `field_spec.xlsx` sheet "Customer" dòng 3–28 | 🟩 Đầy đủ | REQ-CUST-01 → 14 |
| Phân quyền | — | ⬜ Trắng | REQ-CUST-30 → 33 |

Module không có tài liệu nào → ghi một dòng: *"Không có tài liệu — toàn bộ REQ sinh từ khảo sát UI thực tế"*. **KHÔNG** bỏ hẳn mục.

### 6.6. Ma trận Trạng thái (nếu entity có status flow)
| Trạng thái hiện tại | Hành động cho phép | Trạng thái kế tiếp | Ai được thực hiện |
|---|---|---|---|

### 6.7. Điểm Mơ Hồ & Rủi Ro

**Bảng Ambiguities** — có vòng đời, không phải danh sách tĩnh:

| Mã | Câu hỏi | Nguy cơ | Mức độ | Assumption tạm | Trạng thái | Kết luận |
|---|---|---|---|---|---|---|
| AMB-04 | Visible Tabs vs quyền khách hàng, cái nào ưu tiên? | ... | 🔴 | Visible Tabs thắng | ❓ Chờ trả lời | — |
| AMB-02 | Deadline có được sớm hơn Start Date? | ... | 🔴 | Không validate | ✅ Đã trả lời 2026-08-10 | PO xác nhận là lỗi → sinh REQ-PRJ-79 |

**Bảng mã trạng thái AMB:**

| Ký hiệu | Nghĩa | Hành động tiếp theo |
|---|---|---|
| ❓ | **Chờ trả lời** — đã gửi PO/BA, chưa có phản hồi | Test theo Assumption tạm, gắn nhãn `assumption-based` |
| ✅ | **Đã trả lời** — ghi ngày + kết luận | Nếu kết luận sinh yêu cầu mới → cấp REQ mới và ghi Nhật ký. Nếu khác Assumption tạm → **TC đang dựa trên giả định phải sửa** |
| ⏭️ | **Bỏ qua** — PO xác nhận không cần làm rõ ở giai đoạn này | Ghi rõ lý do; test theo Assumption tạm và chấp nhận rủi ro |

> AMB đã ✅ hoặc ⏭️ **không được xoá khỏi bảng** — giữ lại để biết quyết định đến từ đâu.

**Bảng Risks:** `RISK-XX | Rủi ro | Mô tả | Mitigation`

### 6.8. Phân rã Epic / Story (Backlog View) — bắt buộc khi ≥ 25 REQ

Chiếu toàn bộ REQ sang cấu trúc backlog để chia việc, **không thay thế** các mục trên:

| Story ID | Tên Story | REQ bao phủ | Số REQ | AMB / RISK liên quan | Ghi chú phạm vi |
|---|---|---|---|---|---|
| STORY-PRJ-01 | Danh sách & lọc | REQ-PRJ-01 → REQ-PRJ-13 | 13 | RISK-05 | ... |

Kèm theo bảng trên, **BẮT BUỘC** có đủ 4 phần:
1. **Dòng tổng kiểm chứng:** `Tổng: N Story / M REQ — mọi REQ thuộc đúng một Story, không mồ côi, không trùng`, kèm phép cộng hiện rõ (`7+5+7+6+6+9 = 40 ✔`)
2. **Bảng đối chiếu AMB/RISK:** **mọi** mã AMB và RISK phải xuất hiện ở đúng một trong hai chỗ — gắn vào một Story, hoặc nằm trong Hạng mục cấp Epic. Trình bày thành bảng để tự kiểm:

   | Nhóm | Mã | Nằm ở đâu |
   |---|---|---|
   | AMB thuộc Story | AMB-02…17 | phân bổ ở bảng Story |
   | AMB cấp Epic | AMB-01 | Ma trận Phân quyền |
   | RISK thuộc Story | RISK-01…05, 07 | phân bổ ở bảng Story |
   | RISK cấp Epic | RISK-06 | rủi ro mức module |

   Mã không xuất hiện ở đâu cả = **không ai chịu trách nhiệm xử lý**. Đây là lỗi hay bị bỏ sót vì bảng Story chỉ được kiểm tổng REQ, không kiểm tổng AMB/RISK.
3. **Hạng mục cấp Epic:** liệt kê những phần cắt ngang cố ý không gán vào Story nào (ma trận phân quyền, ma trận trạng thái, NFR, RISK mức module…) **kèm lý do**
4. **Thứ tự triển khai đề xuất:** xếp theo phụ thuộc và rủi ro, **không** theo thứ tự đánh số. Đánh dấu rõ Story nào đang `BLOCKED` bởi AMB nào

### 6.9. Nhật ký Thay đổi (Changelog) — BẮT BUỘC ở mọi tài liệu module

Đây là **bộ nhớ liền mạch** của tài liệu. Module đang phát triển thì ticket sẽ liên tục sửa/bổ sung yêu cầu; mục này ghi lại toàn bộ đường đi để bất kỳ ai (hoặc AI ở phiên làm việc mới) đọc một lần là nắm đủ bối cảnh.

Đặt ở **cuối tài liệu**, thứ tự **mới nhất lên trên**:

| Ngày | Nguồn | REQ ảnh hưởng | Loại | Tóm tắt thay đổi | TC cần xử lý |
|---|---|---|---|---|---|
| 2026-08-15 | ABC-140 | REQ-PRJ-58 → 62 | 🔴 Bỏ | Gỡ chức năng Copy Project khỏi phạm vi | TC-PRJ-31 → archive |
| 2026-08-10 | ABC-123 | REQ-PRJ-42 | 🟡 Sửa | Deadline chuyển thành bắt buộc phải sau Start Date | TC-PRJ-18, TC-PRJ-19 → review |
| 2026-08-10 | ABC-123 | REQ-PRJ-79 | 🟢 Thêm | Bổ sung field Priority cho dự án | — (viết TC mới) |
| 2026-08-02 | UI recon | REQ-PRJ-01 → 65 | 🟢 Thêm | Khởi tạo tài liệu từ khảo sát UI thực tế | — |

**Quy tắc ghi Nhật ký:**
- **Mọi** thay đổi đều phải có dòng — kể cả khi chỉ sửa câu chữ mà không đổi hành vi (ghi loại `✏️ Biên tập`)
- Cột `Nguồn` ghi ticket ID hoặc `UI recon` — phải truy được về đợt phân tích nào
- Cột `TC cần xử lý` là **mắt xích cảnh báo** cho tester: `review` / `archive` / `viết mới`. Không biết TC nào bị ảnh hưởng thì ghi `⚠️ chưa rà soát`
- Dòng đầu tiên luôn là dòng khởi tạo tài liệu
- Nhật ký **chỉ nằm ở file index** khi tài liệu bị tách nhiều file (mục 5.3)

**Đồng bộ 3 nơi — kiểm tra trước khi bàn giao:**

```
Nhật ký (6.9)  ←→  Trạng thái REQ (6.2)  ←→  Dải mã metadata (6.1)
```
- REQ nào có trạng thái 🟡/🔴 thì phải có dòng Nhật ký tương ứng, và ngược lại
- REQ mới thêm phải nằm trong dải `Mã kế tiếp` đã công bố, và dòng `Dải mã đã dùng` phải được cập nhật

> Mục 6.5 / 6.6 ghi "Không áp dụng" nếu module không có phân quyền/trạng thái — KHÔNG được bỏ hẳn mục, để người đọc biết đã cân nhắc.

---

## 7. Bắt buộc (Strict Rules)

### 7.1. Luật chung — áp cho MỌI nhánh

- Luôn viết bằng **Tiếng Việt**.
- **Mọi yêu cầu chức năng, business rule, validation rule đều phải có mã REQ ID** — tài liệu không có mã bị coi là chưa đạt.
- **Không tự suy diễn nghiệp vụ** nếu không có căn cứ từ UI/tài liệu → đưa vào Ambiguities kèm Assumption tạm.
- **Mọi khẳng định phải truy được về nguồn.** Cột `Nguồn` của mỗi REQ là bắt buộc, không được để trống hay ghi chung chung.
- **Luôn kiểm tra `docs/requirements/<module>/requirements_<module>.md` trước khi gán mã REQ đầu tiên** và đánh tiếp từ số cuối cùng (mục 2.1) — áp cho MỌI workflow sinh REQ, không riêng nhánh nào.
- **Dự án mới, chưa có `docs/requirements/README.md` → PHẢI tạo file danh mục trước** khi ghi tài liệu module đầu tiên (mục 5.7.1). Thiếu file này, cơ chế chống trùng prefix và nối tiếp mã REQ im lặng không hoạt động.
- **Không bao giờ đánh lại số REQ ID** khi tách file, gom Story hay tái cấu trúc tài liệu.
- **Đếm số REQ trước khi ghi file** và áp đúng bảng ngưỡng tại mục 5.1 — không tự ý gộp hay tách ngoài quy tắc.
- Dù tách bao nhiêu file, **điểm vào luôn là `requirements_<module>.md`** kèm mục `## Bản đồ tài liệu` (mục 5.5) — để các workflow phía sau không vỡ.
- **KHÔNG BAO GIỜ xoá dòng REQ** khỏi tài liệu — tính năng bị gỡ thì đổi trạng thái sang 🔴 Deprecated (mục 6.2). Xoá dòng là mất dấu vết và làm vỡ RTM.
- **Mọi thay đổi tài liệu đều phải ghi Nhật ký thay đổi** (mục 6.9), kèm cột `TC cần xử lý` — đây là mắt xích duy nhất báo cho tester biết test case nào đã stale.
- **Ba nơi phải luôn khớp nhau:** Nhật ký (6.9) ↔ Trạng thái REQ (6.2) ↔ Dải mã metadata (6.1).
- **Chạy Cổng Tự Soát REQ (mục 4.3) TRƯỚC KHI ghi file** — 9 câu hỏi bắt lỗi do chính agent viết sai. Bỏ qua bước này thì lỗi chỉ lộ ra ở tầng test case, khi đã quá muộn để truy nguồn.
- 🔒 **CẤM ghi giá trị bí mật thật vào bất kỳ tệp nào trong `docs/`** — mật khẩu, token, cookie xác thực, session id, API key, chuỗi ký của liên kết đặt lại mật khẩu. Ghi **hình thái** thay cho giá trị (mục 4.3.3). Lý do: `docs/` được commit, và lịch sử git **không xoá được bằng cách sửa file**. Credentials sống ở `.env` (đã `.gitignore`), tài liệu chỉ mô tả *hình dạng* của chúng.
- 🔒 **Trước khi ghi tài liệu đầu tiên của dự án, kiểm `docs/` có bị `.gitignore` chặn không.** Không chặn (mặc định nên vậy — tài liệu cần được commit) thì **mọi** thứ đưa vào `docs/` là công khai với người có quyền đọc repo: cân nhắc lại từng ảnh evidence và từng giá trị chép vào AC.
- **AMB và RISK cũng không được mồ côi.** Mục 6.8 đã buộc mọi REQ thuộc đúng một Story; áp cùng nguyên tắc cho AMB/RISK — mỗi mã hoặc gắn vào một Story, hoặc nằm trong bảng **Hạng mục cấp Epic** kèm lý do. Mã không xuất hiện ở đâu cả nghĩa là không ai chịu trách nhiệm xử lý nó.

### 7.2. Chỉ áp cho nhánh UI Recon (3.1)

- Nếu có Playwright MCP: mở browser thật (`navigate → wait_for(page_load) → snapshot`) để capture giao diện — **KHÔNG đoán field/validation**. KHÔNG gọi `browser_resize`; viewport đã đúng từ `--viewport-size` lúc launch.
- Error messages ghi **nguyên văn từ UI thực tế**, không diễn đạt lại.
- **Trigger từng validation** để lấy message thật — không chép từ code cũ, không suy ra từ tên field.
- Cột `Nguồn` ghi `UI thực tế` (chỉ quan sát) hoặc `Kiểm chứng thực tế` (đã tương tác và xác nhận) — **phân biệt rõ hai mức này**. Có tài liệu kèm thì dùng thang 4 mức ở **3.3.1 Bước 3**.
- Lưu evidence screenshot vào `docs/requirements/<module>/evidence/` — **trong** thư mục module, không tách ra ngoài (mục 5.3).
- **Bật `browser_network_requests` trong suốt recon** (mục 3.1.1) — kể cả lúc submit form. Hệ thống không có tài liệu mà bỏ tầng network là bỏ mất validation server-side và trạng thái ẩn.
- 🚫 **CẤM gọi API trực tiếp** (curl / fetch / `browser_evaluate` gọi endpoint) để dò hành vi. Chỉ **quan sát thụ động** request do UI tự phát sinh — gọi thẳng API là kiểm thử xâm nhập, và không chứng minh được gì về hành vi UI.
- **Không đủ account cho mọi role → KHÔNG bỏ trống mục 6.5, cũng KHÔNG bịa.** Áp thang 3 mức bằng chứng ở mục 3.1.2, kèm dòng tổng `Đã kiểm chứng / Suy diễn / Chưa rõ`, và mỗi role thiếu account = 1 `AMB-XX` 🔴.
- **Ô `❔` không được làm tròn thành `❌`** — "chưa kiểm chứng" khác hẳn "không có quyền".
- **Có tài liệu bán phần → lập Bản đồ phủ tài liệu TRƯỚC khi mở browser** (mục 3.3.1) và đưa vào tài liệu đầu ra ở mục 6.5.1. Không được recon cả module theo một chiến lược duy nhất.
- **Không dùng tài liệu của vùng này để suy ra vùng khác** — spec form tạo mới không nói gì về form sửa, dù hai form trông giống nhau.
- **Đọc `docs/requirements/_discovery/` trước khi recon nếu đã chạy `/discover-system`** (mục 5.8) — lấy route, prefix, bản đồ phủ tài liệu; **không tự đặt lại prefix**.
- **User hỏi "hệ thống có gì" (chưa biết module nào) → route sang `/discover-system`**, không tự crawl rồi sinh REQ lẫn hai tầng.

#### 7.2.1. Chuẩn Evidence (BẮT BUỘC) — evidence thiếu thì bước sinh TC sẽ bịa

Evidence là **nguồn sự thật** cho mọi workflow phía sau (`/generate_testcases_*`, `/generate_automation_*`). Ảnh cắt cụt khiến agent sinh TC phải suy diễn phần không nhìn thấy — và **không ai phát hiện được chỗ nào là suy diễn**.

| Quy tắc | Chi tiết |
|---|---|
| **Ảnh phải chứa trọn đối tượng cần chứng minh** | Mặc định `browser_take_screenshot(fullPage=true)`, vì form dài chụp viewport luôn bị cắt mất nút Save, checkbox cuối, editor. **Mục đích của luật là không cắt cụt đối tượng — không phải chụp càng nhiều càng tốt.** Đối tượng là một widget nằm trọn trong viewport (thanh đầu trang, dropdown, hộp thoại) thì chụp viewport/element là **đủ và đúng phạm vi** — xem luật vệ sinh dữ liệu bên dưới |
| **Chụp theo trạng thái, không chỉ trạng thái mặc định** | Mỗi trạng thái động = 1 ảnh riêng: dropdown **đang mở** (thấy đủ options), field sau khi **đổi giá trị điều khiển** (Billing Type → Total Rate hiện), checkbox **disabled vs enabled**, form **sau khi submit lỗi** (thấy message inline) |
| **🔍 BẮT BUỘC mở lại ảnh xác nhận trước khi ghi danh mục** | Chụp xong phải **`Read` lại chính tệp đó** và xác nhận nó thật sự thể hiện trạng thái sắp khai. Rất hay gặp: bấm mở dropdown nhưng ảnh chụp trước khi menu kịp render, hoặc thao tác mở thất bại mà không ai biết → danh mục ghi *"menu đang mở"* trong khi ảnh là menu đóng. Dòng danh mục sai **nguy hiểm hơn không có ảnh**, vì nó tạo cảm giác REQ đã có bằng chứng |
| **Chụp đủ mọi tab** | Form/trang nhiều tab → mỗi tab 1 ảnh full-page. Tab chưa active thì nội dung bên trong **không render đúng** |
| **Bảng danh sách chụp cả phần điều khiển** | Phải thấy: toàn bộ hàng tiêu đề, dropdown page size, mọi nút thanh công cụ (kể cả nút icon không nhãn), ô tìm kiếm, khu vực phân trang |
| **Đặt tên nêu rõ trạng thái VÀ phạm vi chụp** | `<màn_hình>_<trạng_thái>_<fullpage\|viewport>.png` — VD `project_new_form_default_fullpage.png`, `dashboard_logout_menu_open_viewport.png`. Hậu tố phải khớp cách chụp thật; đặt `_fullpage` cho ảnh viewport là khai sai |
| **🔒 Vệ sinh dữ liệu — evidence là tệp sẽ được commit** | Ảnh chụp hệ thống thật thường kéo theo **dữ liệu không liên quan tới REQ**: tên khách hàng, số tiền, email, nhật ký hoạt động, nội dung công việc. Luật: **chụp đúng phạm vi đối tượng cần chứng minh, không chụp thừa.** Đối tượng nằm trong viewport mà ảnh full-page kéo theo cả trang dữ liệu nghiệp vụ → **chụp viewport**, và ghi một dòng lý do trong Danh mục Evidence. Buộc phải full-page mà không tránh được dữ liệu nhạy cảm → báo user trước khi commit |
| **Ghi bảng Danh mục Evidence vào tài liệu** | Mỗi ảnh 1 dòng: tên tệp · màn hình · trạng thái · REQ mà nó làm bằng chứng. REQ không có ảnh nào chống lưng thì cột `Nguồn` **không được** ghi `Kiểm chứng thực tế` |
| **Ảnh KHÔNG thay được việc đọc DOM** | Ảnh không cho biết `disabled` hay chỉ là `không tick`, không đếm được option nào `selected`, không đọc được `value` / `data-*`. Những dữ kiện này **bắt buộc đọc bằng `browser_evaluate`** và **chép nguyên số liệu vào Acceptance Criteria** — vì ảnh lưu lại cũng không kiểm chứng lại được |
| **DOM cũng KHÔNG thay được ảnh** | Chiều ngược lại cũng đúng: đếm được phần tử trong DOM **không** chứng minh người dùng nhìn thấy nó. Hai nguồn bổ sung cho nhau, không thay thế nhau — xem mục **4.3.5** |

**Dữ kiện bắt buộc đọc từ DOM (ảnh không thể hiện được):**

| Dữ kiện | Vì sao ảnh không đủ |
|---|---|
| Số `<option>` thật của mỗi `<select>` | Option rỗng đầu danh sách không hiển thị trên UI nhưng vẫn đếm khi automation |
| Option nào đang `selected` (nhất là multi-select) | Multi-select hiển thị dạng chuỗi bị cắt `...` — không đếm được |
| `disabled` vs `checked` của từng checkbox | Xám mờ trên ảnh có thể là disabled, cũng có thể chỉ là màu nhạt |
| `value` của option (ánh xạ nhãn ↔ giá trị) | Không bao giờ hiện trên UI, nhưng automation phải chọn theo `value` |
| Thứ tự focus thật của form | Bố cục nhiều cột làm thứ tự Tab khác thứ tự đọc từ trên xuống |
| Phần tử ẩn bằng class nhưng vẫn trong DOM | Ảnh chỉ cho biết "không thấy", không cho biết "không tồn tại" |
| `offsetParent` + `getBoundingClientRect()` của **mọi** phần tử khai là "dùng được" | Đếm được node **không** nghĩa là người dùng bấm được — node có thể bị tổ tiên `display:none` (responsive, quyền, trạng thái). Bắt buộc theo mục **4.3.5** |
| Số phần tử **lồng bên trong** khi khai vị trí ("mục cuối cùng", "dòng đầu") | `X li:last-child` bắt cả `<li>` ở submenu; phải dùng `X > li:last-child`. Sai chỗ này thì locator trong TC trỏ nhầm phần tử |
| Giá trị **đầy đủ** của `document.title` và `body.className` | Thường có phần động (số đếm thông báo, token theo trình duyệt/cấu hình) — chép thiếu là AC assert khớp tuyệt đối sẽ đỏ. Xem mục **4.3.7** |
| Viewport đang đo (`innerWidth × innerHeight`) | Mọi kết luận về hiển thị **chỉ đúng với viewport đó** — phải ghi vào AC, và phần tử responsive cần recon riêng ở viewport khác |

**Checklist trước khi đóng recon:**
- Mọi REQ có cột `Nguồn` = `UI thực tế` / `Kiểm chứng thực tế` đều phải truy được về ít nhất 1 ảnh trong Danh mục Evidence **hoặc 1 lần đọc DOM có ghi số liệu trong Acceptance Criteria**. Không truy được → hạ về `Chưa kiểm chứng` + mở `AMB-XX`.
- **Mọi ảnh trong Danh mục Evidence đã được `Read` lại** và xác nhận đúng trạng thái đang khai.
- **Đã chạy Cổng Tự Soát REQ (mục 4.3)** trên toàn bộ REQ vừa sinh.

### 7.3. Chỉ áp cho nhánh Document Analysis (3.2)

- **KHÔNG đọc file nhị phân bằng `Read`** — ủy quyền đúng skill theo bảng ở mục 3.2 Bước 0. Không đọc được thì **dừng và báo user**, không suy đoán từ tên file.
- **KHÔNG tự viết AC thay PO/BA.** Tài liệu thiếu AC → ghi nhận đúng thực trạng + `AMB-XX` 🔴 High (mục 3.2 Bước 4). Đây là luật tương đương với "không đoán locator" của nhánh UI.
- **Trích dẫn nguyên văn** mọi rule, message, giá trị ngưỡng — cấm diễn đạt lại rồi gán REQ.
- **Ghi vị trí nguồn cụ thể** trong cột `Nguồn`, đủ để người review mở đúng chỗ mà đối chiếu:
  ```
  ✅ Ticket ABC-123 · AC#4          ✅ field_spec.xlsx · sheet "Fields" · dòng 12
  ✅ Comment của PO ngày 2026-07-15  ❌ "theo tài liệu"   ❌ "trong ticket"
  ```
- **KHÔNG bỏ qua comments** — comment thường là quyết định mới nhất và **đè lên** phần mô tả gốc.
- **Xung đột giữa các nguồn KHÔNG được tự giải quyết im lặng** — luôn thành `AMB-XX`, kể cả khi đã áp thứ tự ưu tiên ở mục 3.2 Bước 3.
- Phân biệt **"Không đề cập trong tài liệu"** (tài liệu thiếu → cần hỏi) với **"Không áp dụng"** (đã cân nhắc và xác định không liên quan). Hai câu này nghĩa khác hẳn nhau.
- **KHÔNG tự fetch URL Jira/Confluence** — route sang `/fetch-jira-requirements`. MCP chưa authorize thì báo user, tuyệt đối không bịa nội dung ticket.
