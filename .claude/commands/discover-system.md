---
description: Khám phá hệ thống ở cấp toàn cục — crawl navigation, lập bản đồ module, gán prefix REQ, khởi tạo danh mục. Hỗ trợ 3 modes — UI (chỉ có hệ thống chạy), HYBRID (có một phần tài liệu), DOC (chỉ có tài liệu).
skills:
  - skills-requirements-analyzer
  - skills-ui-debug-agent
---

# Workflow: Khám Phá Hệ Thống (System Discovery)

> **BẮT BUỘC (MANDATORY SKILL):** Nạp và đọc kỹ **`skills-requirements-analyzer`** (`.claude/skills/skills-requirements-analyzer/SKILL.md`) — dùng mục **2** (quy ước mã), **3.1** (UI recon), **3.1.1** (network), **3.3** (đối chiếu tài liệu ↔ UI), **5.3** (thư mục), **5.7** (danh mục), **5.8** (tầng khám phá), **7.1 + 7.2** (strict rules).
>
> Tham khảo **`skills-ui-debug-agent`** cho thao tác inspect DOM.

Workflow này chạy **trước tất cả**, khi bạn mở một hệ thống lên mà **chưa biết nó có những module nào**. Đầu ra là **bản đồ hệ thống** + **danh mục module đã gán prefix** — nền tảng để chạy `/generate-requirements-from-website` cho từng module về sau.

---

## ⚠️ Ranh giới của workflow này — đọc trước khi chạy

| Workflow này **CÓ** làm | Workflow này **KHÔNG** làm |
|---|---|
| Liệt kê toàn bộ module / trang / entity của hệ thống | ❌ Sinh `REQ-XXX-NN` — **tuyệt đối không gán mã REQ** |
| Gán **prefix** cho từng module (`LOGIN`, `CUST`…) và đăng ký chống trùng | ❌ Sinh Field Spec, validation message, ma trận phân quyền chi tiết |
| Ghi nhận độ sâu tài liệu QA cung cấp (nếu có) | ❌ Sinh test case |
| Đánh giá risk sơ bộ + đề xuất thứ tự khảo sát | ❌ Trigger validation từng field (việc của recon cấp module) |
| Khởi tạo `docs/requirements/README.md` | ❌ Thay thế `/generate-requirements-from-website` |

> **Vì sao tách bạch:** mã REQ là lớp truy vết **bất biến** (skill mục 2). Gán mã ở tầng khám phá — khi chưa mở từng form, chưa trigger validation — chắc chắn sẽ phải đánh lại số ở bước sau. Tầng khám phá **chỉ cấp prefix**, không cấp số.

---

## 3 Chế độ (Mode)

**Agent TỰ nhận diện mode ở Bước 0 — user không cần khai báo.** Mode suy ra hoàn toàn từ đầu vào:

```
Đã có docs/requirements/_discovery/system_map.md ?
├─ CHƯA → khảo sát lần đầu
│         User có đưa tài liệu?     ─ có ─→ HYBRID
│                                   └ không ─→ UI
│         Không truy cập được hệ thống (chưa có URL / chưa có account) → DOC
└─ RỒI  → User chỉ đích danh module bị sót?  ─ có ─→ ADD
                                              └ không ─→ DELTA
```

**Trigger từ lời user:**

| User nói gì | Mode |
|---|---|
| *"khám phá hệ thống này"* + đưa URL | **UI** |
| *"đây là spec tôi có"* · đính kèm file · *"tài liệu chỉ có một phần"* | **HYBRID** |
| *"chưa có account, đọc tài liệu trước"* | **DOC** |
| *"còn thiếu module X"* · *"bạn chưa khảo sát menu Báo cáo"* | **ADD** |
| *"rà lại xem có gì mới"* · *"vừa deploy tính năng mới"* | **DELTA** |

**Agent BẮT BUỘC công bố mode đã chọn** ngay câu đầu tiên, kèm lý do một dòng:
> *"Chạy mode HYBRID — có 2 file tài liệu bạn đưa, và hệ thống truy cập được."*

Sai mode thì bạn chặn ngay ở câu đó, chưa tốn công gì. **User luôn được quyền chỉ định đè** — nói *"chạy mode UI thôi, bỏ tài liệu đó đi, nó cũ rồi"* thì agent theo.

⚠️ Mode **không** được đổi giữa chừng. Đang HYBRID mà thấy tài liệu quá cũ → **không** tự hạ xuống UI; vẫn giữ HYBRID và đánh vùng đó `⚠️ Nghi lỗi thời` (Bước 2), vì lệch pha giữa tài liệu và UI là **thông tin có giá trị**, vứt tài liệu đi là mất luôn.

Ghi mode đã chạy vào mục 1 của `system_map.md`.

| Mode | Đầu vào | Đặc điểm |
|---|---|---|
| **UI** (mặc định) | Chỉ có hệ thống đang chạy | Sự thật 100% ở UI. Mọi thứ chưa quan sát được → `❔ Chưa rõ`, không suy diễn |
| **HYBRID** | Hệ thống chạy **+ một phần tài liệu** (QA cung cấp) | Phổ biến nhất. Tài liệu cho **ý định**, UI cho **thực tế**. Bắt buộc lập **Bản đồ phủ tài liệu** (Bước 2) |
| **DOC** | Chỉ có tài liệu, chưa truy cập được hệ thống | Chỉ dựng được bản đồ dự kiến. Mọi module đánh dấu `⬜ Chưa khảo sát UI` — **không** được coi là đã xác minh |

Hai chế độ chạy bổ sung khi **đã có bản đồ từ trước** (chi tiết ở cuối tài liệu):

| Chế độ | Khi nào | Phạm vi |
|---|---|---|
| **ADD** | User chỉ đích danh module bị sót | Chỉ module đó — không crawl lại |
| **DELTA** | Hệ thống đã thay đổi, hoặc muốn rà lại toàn bộ | Crawl lại, báo phần chênh |

> Tài liệu QA cung cấp thường **không phủ hết** — vài module có spec, phần còn lại trắng. Mode HYBRID xử lý đúng tình huống này: **không** vứt tài liệu đi, cũng **không** tin tài liệu thay cho UI.

---

## Các bước thực hiện

### Bước 0: Tiếp nhận đầu vào & chốt bối cảnh

Thu thập, hỏi user thứ **không suy ra được**:

| Cần có | Nguồn | Bắt buộc |
|---|---|---|
| URL hệ thống | User | ✅ (trừ mode DOC) |
| Tài khoản đăng nhập — **càng nhiều role càng tốt** | User | ✅ |
| Tài liệu sẵn có (nếu có): spec, user story, ticket, file bảng, mockup | User | Tuỳ mode |
| **Tên hệ thống viết tắt** — dùng làm tiền tố TC ID về sau (`CRM_`, `ERP_`, `HRM_`) | Agent đề xuất → **user xác nhận 1 câu** | ✅ |
| **Môi trường dùng chung không?** | **Hỏi user** — không suy ra được từ UI | ✅ |

⚠️ Môi trường dùng chung = bật ngay quy tắc: **chỉ đọc, không tạo/sửa/xoá dữ liệu** trong suốt quá trình khám phá. Khám phá không cần ghi dữ liệu — mở form xem field là đủ, **KHÔNG bấm Save**.

📌 URL và tài khoản lưu ở `.env`, **KHÔNG** ghi vào bất kỳ tài liệu nào trong `docs/`.

### Bước 1: Đọc trạng thái repo hiện có (LUÔN LÀM ĐẦU TIÊN)

Trước khi khám phá bất cứ thứ gì:

1. `docs/requirements/README.md` — có tồn tại không?
   - **Có** → đọc bảng danh mục: module nào đã có, **prefix nào đã bị chiếm**. Lần chạy này là **bổ sung**, không phải khởi tạo lại
   - **Chưa** → sẽ tạo mới ở Bước 6 (skill mục 5.7.1)
2. `docs/requirements/_discovery/system_map.md` — đã khám phá lần nào chưa? Có thì đọc để **cập nhật delta**, không viết đè
3. Glob `docs/requirements/*/requirements_*.md` — đối chiếu chéo với danh mục theo bảng dưới. Đây là **thư mục thật vs danh mục khai báo**; lệch ở đây là dấu hiệu một phiên trước đã quên cập nhật

| Điểm lệch | Xử lý ngay ở Bước 1 |
|---|---|
| Có tài liệu module nhưng **thiếu dòng** ở danh mục | Bổ sung dòng — prefix · `REQ đã dùng` · `Mã kế tiếp` lấy **từ chính tài liệu module**, không đoán. Trạng thái recon = ✅ |
| Có dòng danh mục nhưng **không có thư mục** | **KHÔNG xoá dòng** — đưa `Trạng thái recon` về ⬜, ghi lý do ở Nhật ký danh mục. Prefix đã cấp là vĩnh viễn |
| **Prefix trùng** giữa 2 module | ⛔ **DỪNG, báo user** — agent KHÔNG tự đổi. Đổi prefix là đổi mọi REQ ID của một module và mọi TC map về nó |

Sửa gì thì ghi **1 dòng vào Nhật ký danh mục**. Không lệch → nêu `Danh mục khớp thư mục thực tế` ở Bước 7.

> **Nguyên tắc phân xử:** thư mục + tài liệu module là **nguồn sự thật**, danh mục là bản phái sinh → sửa danh mục theo tài liệu, không ngược lại. Ngoại lệ duy nhất là **prefix** — lệch prefix phải hỏi user.

> ❌ **Tuyệt đối không** ghi đè `README.md` hay `system_map.md` đã có. Prefix đã cấp là **vĩnh viễn** — kể cả khi module đó đổi tên trên UI.

### Bước 2: Nạp tài liệu QA cung cấp (Mode HYBRID / DOC)

> Mode UI → bỏ qua, sang Bước 3.

1. **Đọc đúng cách theo định dạng** — theo bảng ở skill mục **3.2 Bước 0**:
   `.docx` → skill `docx` · `.xlsx/.csv` → skill `xlsx` · `.pdf` → skill `pdf` · URL Jira → `/fetch-jira-requirements`
   ⚠️ Không đọc được → **báo user và dừng**, không suy đoán từ tên file
2. **Lưu bản gốc** vào `docs/requirements/_discovery/sources/` — giữ nguyên tên file, để người review mở đối chiếu được
3. **Lập Bản đồ phủ tài liệu** — đây là sản phẩm chính của bước này:

| Vùng hệ thống | Tài liệu nào phủ | Mức phủ | Ghi chú |
|---|---|---|---|
| Đăng nhập / phân quyền | `spec_auth_v2.docx` mục 3 | 🟩 Đầy đủ | Có cả ma trận role |
| Quản lý khách hàng | `field_spec.xlsx` sheet "Customer" | 🟨 Một phần | Chỉ có field, thiếu business rule |
| Báo cáo | — | ⬜ Trắng | Phải recon UI hoàn toàn |

**Bảng mã mức phủ:**

| Ký hiệu | Nghĩa | Hệ quả cho recon cấp module |
|---|---|---|
| 🟩 **Đầy đủ** | Có AC/field spec/rule rõ ràng | Recon để **đối chiếu**, tập trung tìm lệch pha (skill 3.3) |
| 🟨 **Một phần** | Có nhưng thiếu mảng lớn (rule, message, phân quyền) | Recon **bổ khuyết** đúng phần trắng |
| ⬜ **Trắng** | Không có gì | Recon **đầy đủ** như mode UI |
| ⚠️ **Nghi lỗi thời** | Có tài liệu nhưng ngày cũ / mô tả không khớp UI khi liếc qua | Recon với giả định **UI thắng**, mọi lệch pha ghi `AMB` ở bước sau |

4. **Rút danh sách module dự kiến** từ tài liệu — đây mới là *dự kiến*, phải đối chiếu UI ở Bước 4

### Bước 3: Khám phá UI cấp hệ thống (Mode UI / HYBRID)

> Mode DOC → bỏ qua, sang Bước 4 với dữ liệu tài liệu.

Thứ tự bắt buộc (theo `.claude/rules/playwright_rules.md`):

```
browser_navigate → chờ load → browser_snapshot
```

**3.1 — Crawl điều hướng (làm cạn kiệt, không dừng ở tầng 1):**

| Việc | Cách làm | Bẫy hay gặp |
|---|---|---|
| Menu chính (sidebar / header) | `browser_snapshot` toàn trang | Menu **cấp 2, cấp 3** chỉ hiện khi hover/click — phải mở **từng** mục cha |
| Menu bị cuộn khuất | Cuộn hết sidebar | Module cuối danh sách hay bị bỏ sót |
| Link không nằm trong menu | `browser_evaluate` gom toàn bộ `a[href]` trên các trang chính, lọc trùng | Trang chỉ tới được từ nút trong bảng (Chi tiết, Sửa) |
| Route ẩn | Đọc file JS định tuyến qua network (3.2) hoặc thử URL theo mẫu quan sát được | ⚠️ Route đoán ra mà không mở được → ghi `❔ Nghi có, chưa xác minh`, **không** đưa vào danh mục module |
| Trang chỉ vào được từ hành động | Mở 1 bản ghi bất kỳ ở chế độ xem | ⚠️ Môi trường dùng chung: **chỉ mở, không sửa** |

**3.2 — Đọc tầng network** (skill mục **3.1.1**): bật `browser_network_requests` trong lúc crawl, gom danh sách endpoint. Đây là cách rẻ nhất để phát hiện:
- Module **có API nhưng chưa có UI** → tính năng đang build dở → ghi `⚪ Chưa implement`
- Ranh giới entity thật (`/api/customers`, `/api/customers/{id}/contacts` → Customer và Contact là 2 entity)
- Enum trạng thái trả về trong response → gợi ý module nào có status flow

**3.3 — Ghi nhận cho mỗi module phát hiện được:**

| Thông tin | Cách lấy |
|---|---|
| Tên hiển thị trên UI (nguyên văn) | Snapshot |
| URL / route | Thanh địa chỉ |
| Loại màn hình | Danh sách · Form · Dashboard · Wizard · Báo cáo · Cấu hình |
| Có CRUD không | Quan sát nút trên thanh công cụ |
| Có status flow không | Cột trạng thái trong bảng danh sách, badge màu |
| Số tab con | Đếm tab trong màn hình chi tiết |
| Ước lượng độ lớn | Số field form + số cột bảng + số tab → dùng cho ước số REQ |
| Screenshot 1 ảnh/module | `browser_take_screenshot(fullPage=true)` → `_discovery/evidence/` |

> Ở tầng khám phá, **1 ảnh full-page mỗi module là đủ** — chỉ để chứng minh module tồn tại. Chuẩn evidence đầy đủ (skill 7.2.1) áp dụng ở tầng recon cấp module, không phải ở đây.

**3.4 — Thăm dò phân quyền** (skill mục **3.1.2**): nếu user cấp nhiều account, đăng nhập lần lượt và so sánh menu nhìn thấy được. Chỉ có 1 account → áp quy tắc suy diễn ở 3.1.2 và **đánh dấu rõ là suy diễn**.

### Bước 4: Đối chiếu & chuẩn hoá danh sách module

**4.1 — Đối chiếu tài liệu ↔ UI (Mode HYBRID):**

| Kiểu lệch | Nghĩa | Xử lý ở tầng khám phá |
|---|---|---|
| Tài liệu có, UI không có | Chưa build / đã gỡ | Vẫn vào danh mục, trạng thái `⚪ Chưa implement`, ghi chú nguồn |
| UI có, tài liệu không nói | Tính năng ngoài tài liệu | Vào danh mục, mức phủ `⬜ Trắng` |
| Tên gọi khác nhau | Tài liệu "Đối tác" ↔ UI "Nhà cung cấp" | **Lấy tên UI làm chuẩn**, ghi tên tài liệu vào cột bí danh |
| Ranh giới module khác nhau | Tài liệu gộp, UI tách (hoặc ngược lại) | **Theo UI**, ghi 1 dòng cảnh báo trong `system_map.md` |

**4.2 — Quyết định ranh giới module** (quyết định đắt nhất của cả workflow — sai là phải đánh lại prefix):

| Nguyên tắc | Áp dụng |
|---|---|
| **1 entity nghiệp vụ có vòng đời riêng = 1 module** | `Customer` và `Contact` tách, dù nằm chung menu |
| Tab con **không** có entity riêng → **không** tách | 17 tab của Project vẫn là 1 module `PRJ` |
| Tab con **có** entity riêng + CRUD riêng → tách | Tab "Hoá đơn" trong Project → module `INV` |
| Nhóm màn hình cấu hình rời rạc → gom 1 module `SETTING` | Tránh đẻ ra 20 prefix cho 20 trang cấu hình 1 field |
| Nghi ngờ → **gộp**, đừng tách | Gộp rồi tách sau vẫn giữ được REQ ID; tách rồi gộp thì vỡ |

**4.3 — Gán prefix:**
- Ngắn, **VIẾT HOA**, không dấu, 3–6 ký tự: `LOGIN`, `CUST`, `PRJ`, `INV`
- **Đối chiếu danh sách prefix đã chiếm** ở Bước 1 — trùng là phải đổi
- Prefix suy từ **tên nghiệp vụ**, không suy từ URL (URL hay đổi)
- Ghi cả **bí danh** nếu tài liệu gọi tên khác

**4.4 — Đánh giá risk sơ bộ:**

| Mức | Dấu hiệu |
|---|---|
| 🔴 Cao | Liên quan tiền/quyền/dữ liệu khách hàng · nhiều module khác phụ thuộc · có status flow phức tạp · mức phủ tài liệu ⬜ Trắng |
| 🟡 Trung bình | CRUD thường · dùng hằng ngày · tài liệu 🟨 một phần |
| 🟢 Thấp | Màn hình tra cứu tĩnh · cấu hình ít đổi · tài liệu 🟩 đầy đủ |

### Bước 5: ⏸️ CHECKPOINT — chốt với user trước khi ghi file

**DỪNG LẠI.** Trình bày cho user:

1. **Bảng module phát hiện được** — tên, prefix đề xuất, loại màn hình, mức phủ tài liệu, risk, ước số REQ
2. **Danh sách nghi vấn** — route đoán được nhưng chưa mở được, module không chắc nên tách hay gộp
3. **Lệch pha tài liệu ↔ UI** (mode HYBRID) — liệt kê thẳng, đây là thứ user quan tâm nhất
4. **Thứ tự khảo sát đề xuất** — xếp theo *phụ thuộc trước, risk sau*, không theo alphabet

Hỏi đúng 3 câu:
- Danh sách module đã đủ chưa? Có module nào tôi **không thấy được** vì thiếu quyền, nằm sâu trong menu, hoặc chỉ vào được bằng URL trực tiếp?
- Ranh giới tách/gộp có đúng cách team hiểu không?
- Thứ tự khảo sát ưu tiên thế nào?

> Câu đầu là quan trọng nhất — user thường biết những module agent **không có cách nào thấy được**. Trả lời ở đây thì chưa file nào bị ghi; phát hiện sau đó phải chạy **Mode ADD** để bổ sung.

⚠️ **Chưa có phản hồi thì chưa ghi file.** Prefix ghi ra rồi là bất biến — sửa sau tốn hơn hỏi trước rất nhiều.

### Bước 6: Ghi file đầu ra

Sau khi user chốt, ghi đúng 3 nhóm file:

```
docs/requirements/
├── README.md                          ← DANH MỤC — tạo mới hoặc BỔ SUNG dòng
└── _discovery/
    ├── system_map.md                  ← INDEX — TÊN FILE BẤT BIẾN
    ├── modules/                        ← chỉ khi tách (ngưỡng 6.2)
    │   ├── module_01_dang_nhap_phan_quyen.md
    │   └── module_02_khach_hang.md
    ├── doc_inventory.md               ← chỉ mode HYBRID/DOC — bản đồ phủ tài liệu
    ├── sources/                       ← bản gốc tài liệu QA cung cấp
    └── evidence/<module>_overview_fullpage.png
```

**6.1 — Ngưỡng tách file bản đồ** (đếm số module **sau khi** user chốt ở Bước 5):

| Số module | Cấu trúc |
|---|---|
| **≤ 8** | 1 file `system_map.md` — đủ ngắn để đọc một lượt |
| **> 8** | Tách: `system_map.md` (index) + `modules/module_NN_<slug>.md` |

Tách ngay kể cả khi ≤ 8 module, nếu gặp **bất kỳ** dấu hiệu:
- Có module ≥ 5 tab con hoặc ≥ 30 field — mô tả riêng nó đã dài hơn cả phần còn lại
- Hệ thống chia theo **phân hệ** rõ rệt (Bán hàng · Kho · Kế toán) — tách theo phân hệ đọc dễ hơn hẳn
- Nhiều người cùng khảo sát song song — mỗi người một file, tránh đụng nhau khi commit

**6.2 — `docs/requirements/README.md`** theo schema skill mục 5.7.2, bảng danh mục **thêm 2 cột** phục vụ khám phá:

| Module | Prefix | Trạng thái recon | Mức phủ tài liệu | Tài liệu | REQ đã dùng | Mã kế tiếp | AMB treo | Cập nhật |
|---|---|---|---|---|---|---|---|---|
| Khách hàng | `CUST` | ⬜ Chưa khảo sát | 🟨 Một phần | — | — | `REQ-CUST-01` | — | 2026-08-10 |
| Đăng nhập | `LOGIN` | ✅ Đã có tài liệu | 🟩 Đầy đủ | [requirements_login.md](login/requirements_login.md) | 01 → 14 | `REQ-LOGIN-15` | AMB-02 | 2026-08-11 |

**Bảng mã trạng thái recon:**

| Ký hiệu | Nghĩa | Hành động tiếp theo |
|---|---|---|
| ⬜ | **Chưa khảo sát** — mới chỉ phát hiện tên | Chạy `/generate-requirements-from-website` |
| 🟨 | **Đang khảo sát** — recon dở dang | Tiếp tục, nêu rõ đang dở ở đâu |
| ✅ | **Đã có tài liệu** — `requirements_<module>.md` đã phát hành | Sẵn sàng sinh test case |
| ⏸️ | **Hoãn** — user chốt ngoài phạm vi đợt này | Ghi lý do, giữ prefix |
| ⚪ | **Chưa implement** — phát hiện qua tài liệu/API, UI chưa có | Không recon được; viết TC trước, đánh `skip` |

**6.3 — Phân chia nội dung giữa index và file module (quy tắc cứng)**

Nguyên tắc: **cái gì cắt ngang nhiều module thì ở index, cái gì thuộc riêng một module thì ra file module.** Nhân bản nội dung cắt ngang vào từng file là lỗi — sửa một chỗ quên chỗ kia.

| Mục | Đặt ở đâu | Lý do |
|---|---|---|
| 1. Bối cảnh khảo sát — ngày · mode · URL · role đã dùng · môi trường dùng chung · phạm vi crawl | **Index** | Áp cho cả đợt khảo sát |
| 2. Sơ đồ điều hướng toàn hệ thống — cây menu nguyên trạng kèm route | **Index** | Chỉ có nghĩa khi nhìn tổng thể |
| 3. **Bảng module tổng** — tên UI · bí danh · prefix · file khám phá · loại màn hình · risk · ước REQ | **Index** | Bản đồ điều hướng, thay cho việc mở từng file |
| 4. Bản đồ entity & phụ thuộc | **Index** | Bản chất là quan hệ **giữa** các module |
| 5. Ma trận phân quyền **sơ bộ** cấp module | **Index** | Cắt ngang mọi module |
| 6. Thứ tự khảo sát đã chốt + module `BLOCKED` | **Index** | Kế hoạch cấp đợt |
| 7. Nhật ký khám phá | **Index** | Một dòng cho mỗi lần chạy `/discover-system` |
| **Bản đồ tài liệu** (khi tách) | **Index** | Xem 6.3 — bắt buộc |
| Chi tiết từng module — route · loại màn hình · CRUD · status flow · số tab · số field ước lượng · risk kèm lý do | **File module** | Đơn vị chia việc |
| Phát hiện tầng network **của riêng module** — endpoint · field ẩn · enum trạng thái | **File module** | Gắn với module đó |
| Vùng chưa xác minh **của riêng module** | **File module** | Người nhận module đó cần biết |
| Link evidence của module | **File module** | Đi cùng mô tả |

> Phát hiện network **cấp hệ thống** (endpoint không thuộc module nào, ranh giới entity) vẫn ở **index** mục 4.

**6.4 — Hợp đồng đọc: index bất biến + Bản đồ tài liệu**

Giống nguyên tắc `requirements_<module>.md` ở skill mục 5.5 — workflow phía sau chỉ cần biết **một** đường dẫn:

```
docs/requirements/_discovery/system_map.md
```

Bất kể có tách hay không. Khi tách, index **BẮT BUỘC** có mục:

```markdown
## Bản đồ tài liệu
| File | Module bao phủ | Prefix | Trạng thái recon |
|---|---|---|---|
| [modules/module_01_dang_nhap_phan_quyen.md](modules/module_01_dang_nhap_phan_quyen.md) | Đăng nhập · Người dùng · Vai trò | `LOGIN` · `USER` · `ROLE` | ⬜ ⬜ ⬜ |
| [modules/module_02_khach_hang.md](modules/module_02_khach_hang.md) | Khách hàng | `CUST` | ✅ |
```

Không có mục này → workflow sau hiểu là bản đồ 1 file và **sẽ không bao giờ mở thư mục `modules/`**.

**6.5 — Quy tắc đặt tên & gộp module vào chung file**

**Đặt tên:** `module_NN_<slug>.md`
- `NN` — số thứ tự **theo thứ tự khảo sát đã chốt** ở Bước 5, không theo alphabet
- `<slug>` — tiếng Việt **không dấu**, gạch dưới, mô tả nghiệp vụ: `khach_hang`, `du_an_cong_viec`
- File gộp nhiều module → tên phải **nêu được cả nhóm**, không lấy tên module lớn nhất rồi giấu phần còn lại:

| ✅ Đúng | ❌ Sai | Vì sao |
|---|---|---|
| `module_01_dang_nhap_phan_quyen.md` | `module_01_login.md` | File còn chứa Người dùng + Vai trò, tên phải nói ra |
| `module_03_du_an_cong_viec.md` | `module_03_project.md` | Ưu tiên tiếng Việt, và Task bị giấu |
| `module_05_danh_muc_he_thong.md` | `module_05_misc.md` | "misc" không cho biết bên trong có gì |

**Khi nào được gộp** — thoả **ít nhất một**:

| Điều kiện | Ví dụ |
|---|---|
| Quan hệ **cha–con chặt**, con không tồn tại độc lập | `Dự án` ↔ `Công việc` — task luôn thuộc một project |
| **Luôn được khảo sát cùng nhau**, dùng chung màn hình cấu hình | `Đăng nhập` ↔ `Người dùng` ↔ `Vai trò` |
| Nhiều module **nhỏ cùng loại** (mỗi cái 1–2 màn hình tra cứu/cấu hình) | Các trang danh mục: Đơn vị tính · Khu vực · Nguồn khách |
| Cùng **một phân hệ** và số module trong phân hệ ≤ 3 | Kho: `Nhập kho` · `Xuất kho` · `Tồn kho` |

**Khi nào KHÔNG được gộp:**
- ❌ Chỉ vì "nằm cùng menu" — menu là cách trình bày, không phải quan hệ nghiệp vụ
- ❌ Hai module đều 🔴 risk cao — mỗi cái xứng đáng một file để soi kỹ
- ❌ Gộp **quá 3 module** vào một file — quá 3 thì file dài bằng bản đồ chưa tách, tách vô nghĩa
- ❌ Gộp module ⬜ chưa khảo sát với module ✅ đã có tài liệu — hai trạng thái tiến độ khác nhau, khó theo dõi

**Bất biến khi tách/gộp — kiểm trước khi bàn giao:**

- [ ] **Prefix giữ nguyên tuyệt đối** — gộp file **KHÔNG** gộp prefix. 3 module chung một file vẫn là 3 prefix `LOGIN` · `USER` · `ROLE`, và về sau vẫn sinh ra 3 file `requirements_<module>.md` riêng
- [ ] **Mỗi module thuộc đúng 1 file** — không mồ côi, không nằm ở 2 file
- [ ] **Tổng module trong các file = tổng module ở bảng index** — ghi rõ con số ở index để tự kiểm chứng
- [ ] **Cột `Trạng thái recon` chỉ có ở một nơi** — file `README.md` danh mục. File module ở `_discovery/` **tham chiếu**, không nhân bản (nhân bản là chắc chắn lệch nhau sau vài lần cập nhật)
- [ ] Mỗi file module có link ngược về index; index có link tới mọi file module
- [ ] Nội dung cắt ngang (mục 2 · 4 · 5 · 6) **không** bị chép vào file module

> **Tách/gộp lại về sau là an toàn** — bản đồ khám phá không mang mã REQ (Bước 0), nên chuyển module sang file khác không phá vỡ traceability. Đây chính là lý do tầng khám phá không được cấp số REQ.

**6.6 — `_discovery/doc_inventory.md`** (mode HYBRID/DOC): Bản đồ phủ tài liệu (Bước 2.3) + danh mục file gốc + bảng lệch pha tài liệu ↔ UI (Bước 4.1).

### Bước 7: Bàn giao

**Checklist trước khi báo xong:**

- [ ] Mọi module có prefix **duy nhất**, không trùng prefix đã chiếm từ trước
- [ ] Prefix của module **đã có tài liệu từ trước giữ nguyên tuyệt đối**
- [ ] **Không có mã `REQ-XXX-NN` nào** trong toàn bộ output của workflow này
- [ ] Mỗi module có ≥ 1 ảnh trong `_discovery/evidence/`, hoặc ghi rõ lý do không có
- [ ] Ô phân quyền **suy diễn** đều mang dấu ⚠️, không lẫn với ô đã kiểm chứng
- [ ] Vùng chưa xác minh được liệt kê thật, **không** làm tròn thành "đã khảo sát xong"
- [ ] `README.md` và `system_map.md` khớp nhau về số module và prefix
- [ ] **Danh mục khớp thư mục thực tế** — đã chạy đối chiếu ở Bước 1.3; lệch thì đã sửa và ghi Nhật ký danh mục, hoặc đã báo user (trường hợp trùng prefix)
- [ ] **Nếu đã tách file:** index vẫn tên `system_map.md` · có mục `## Bản đồ tài liệu` · đủ 6 bất biến ở mục **6.5** (prefix giữ nguyên · mỗi module đúng 1 file · tổng khớp · trạng thái recon chỉ ở `README.md` · link 2 chiều · không nhân bản mục cắt ngang)
- [ ] Tên file gộp **nêu đủ nhóm** module bên trong, không giấu module nào (mục 6.5)
- [ ] `.env` chứa URL/tài khoản — `docs/` **không** chứa credentials

**Báo cáo cho user:**
- Số module phát hiện · số đã có tài liệu · số còn trắng
- Mode đã chạy + phạm vi tài liệu đã nạp
- Danh sách vùng chưa xác minh (cần thêm quyền / thêm account / thêm dữ liệu)
- **Lệnh kế tiếp**, theo đúng thứ tự đã chốt:
  ```
  /generate-requirements-from-website <module đầu tiên>
  ```
  Nhắc: mỗi module chạy xong nhớ cập nhật cột `Trạng thái recon` trong danh mục.

---

## Bổ sung module do user chỉ ra (Mode ADD)

> Kích hoạt khi user nói: *"còn thiếu module X"* · *"menu Báo cáo bạn chưa khảo sát"* · *"vào /admin/settings xem"* — tức **user biết có module mà agent không thấy được**.
>
> Khác với Delta bên dưới: ở đây **không crawl lại toàn hệ thống**, chỉ xử lý đúng phần user chỉ.

**Vì sao agent hay sót — hỏi lại user đúng câu để biết đường đi:**

| Nguyên nhân | Dấu hiệu | Cần user cung cấp |
|---|---|---|
| **Thiếu quyền** — account đang dùng không thấy menu | Module chỉ hiện với role cao hơn | Account có quyền, hoặc xác nhận "chỉ role X thấy" |
| **Menu chỉ hiện khi hover/click nhiều tầng** | Nằm ở menu cấp 3 trở lên | Đường đi menu: `Hệ thống → Cấu hình → Phân quyền` |
| **Route không có trong menu** | Vào được bằng URL trực tiếp | URL đầy đủ |
| **Chỉ vào được từ hành động trên bảng** | Bấm nút trong dòng dữ liệu mới ra | Mô tả thao tác để tới đó |
| **Cần dữ liệu mới hiện** | Danh sách rỗng nên không có nút/tab nào | Bản ghi mẫu để mở, hoặc xác nhận môi trường chưa có data |
| **Bị ẩn bởi feature flag / chưa bật** | Có API nhưng không có UI | Xác nhận tính năng đã bật chưa |

**Các bước:**

1. **Hỏi user đủ 2 thứ**: đường đi tới module (URL hoặc menu path) + lý do agent không thấy (theo bảng trên). Thiếu đường đi thì không tự đoán route
2. **Thử truy cập thật:**

| Kết quả | Xử lý |
|---|---|
| ✅ Vào được | Khảo sát như Bước 3, ghi nhận đầy đủ, `Nguồn` = `UI thực tế` |
| ❌ Không vào được (403 / không có quyền / chưa có data) | **Vẫn thêm vào danh mục** với `Trạng thái recon` = ⬜ và ghi rõ ở mục 7 của index: *"Chưa xác minh được — thiếu quyền, chờ account role X"*. `Nguồn` = **`User cung cấp — chưa xác minh UI`** |

3. **Cấp prefix** — đối chiếu danh sách prefix đã chiếm trong `README.md`, chọn prefix chưa dùng
4. **Ghi vào đúng file** theo bảng "Module mới xuất hiện — thêm vào đâu" ở phần Delta bên dưới
5. **Cập nhật 3 nơi**: bảng module ở index · `README.md` danh mục · **Nhật ký khám phá** (ghi rõ `Nguồn: user bổ sung`)
6. **Xem lại thứ tự khảo sát** ở mục 6 của index — module mới chen vào đâu, có `BLOCKED` module nào không

⚠️ **KHÔNG được ghi module user chỉ ra như thể đã tự khảo sát.** Vào được thì mới ghi `UI thực tế`; không vào được thì `User cung cấp — chưa xác minh UI`. Trộn hai cái là bước sinh TC sau đó sẽ tin nhầm vào thứ chưa ai nhìn thấy.

💡 **Rẻ nhất là chặn ngay ở checkpoint Bước 5** — lúc đó agent hỏi thẳng *"có module nào tôi không thấy được vì thiếu quyền?"*. Trả lời ở đó thì chưa file nào được ghi, không phải sửa gì cả.

---

## Chạy lại lần sau (Delta)

> Dùng khi **hệ thống đã thay đổi** (deploy tính năng mới) hoặc muốn **rà lại toàn bộ** — agent tự crawl và so sánh. User chỉ đích danh một module thì dùng Mode ADD ở trên cho nhanh.

Hệ thống có module mới, hoặc lần trước bỏ sót:

1. Bước 1 đọc `system_map.md` cũ → biết đã có gì. Có mục `## Bản đồ tài liệu` thì đọc tiếp các file trong `modules/`
2. Crawl lại, **so sánh** với bảng module cũ
3. Chỉ báo cáo **phần chênh**: module mới · module biến mất · route đổi · menu đổi tên
4. Ghi thêm dòng vào **Nhật ký khám phá**, **không** viết đè bảng cũ
5. Module biến mất khỏi UI → **không xoá dòng**, đổi trạng thái + ghi chú ngày — cùng nguyên tắc "không xoá REQ" ở skill mục 6.2

**Module mới xuất hiện — thêm vào đâu:**

| Tình huống | Xử lý |
|---|---|
| Bản đồ **chưa tách** và tổng module vẫn ≤ 8 | Thêm dòng vào `system_map.md` |
| Bản đồ **chưa tách** nhưng thêm xong vượt 8 | Tách ngay lần này — số `NN` giữ đúng thứ tự khảo sát đã chốt trước đó |
| Bản đồ **đã tách**, module mới thuộc nhóm đã có | Thêm vào file nhóm đó (nếu file đó chưa quá 3 module), cập nhật `## Bản đồ tài liệu` |
| Bản đồ **đã tách**, module mới độc lập | File mới `module_NN_<slug>.md` với `NN` = số kế tiếp — **không** đánh lại số các file cũ |

⚠️ **Không đổi tên file module đã có** chỉ vì thứ tự ưu tiên thay đổi. Thứ tự khảo sát cập nhật ở mục 6 của index; tên file giữ nguyên để link cũ không gãy.
