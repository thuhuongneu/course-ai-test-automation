---
description: Sinh Master Test Plan cấp quản lý — phạm vi, chiến lược, tiêu chí vào/ra, môi trường, nhân lực, lịch trình, rủi ro dự án. Tài liệu để gửi PM/khách hàng, KHÔNG phải danh sách test scenario.
skills:
  - skills-test-summary-reporter
  - skills-requirements-analyzer
  - skills-rbt-manual-testing
---

# Workflow: Master Test Plan

> **BẮT BUỘC (MANDATORY SKILL):** Bạn PHẢI nạp **`skills-test-summary-reporter`** — mục **Tiêu chí Exit** là **nguồn duy nhất** định nghĩa tiêu chí ra. Plan này công bố tiêu chí đó **trước**, `/generate-test-summary-report` chấm lại **sau**. Hai bên phải khớp, nếu không thì lúc release sẽ cãi nhau về chuẩn.

Tài liệu quản lý một đợt kiểm thử: **ai làm gì, trong phạm vi nào, khi nào bắt đầu được, khi nào coi là xong.**

## ⚠️ Chọn đúng workflow — 3 thứ hay bị nhầm

| User cần | Workflow đúng | Đầu ra |
|---|---|---|
| *"Test cái gì trước, có những kịch bản nào"* | `/generate-application-test-plan` | Scenario + priority, **không** quản lý |
| *"Hệ thống có module nào"* | `/discover-system` | Bản đồ module + prefix |
| *"Tài liệu kế hoạch để gửi PM/khách ký"* | **Workflow này** | Master Test Plan — phạm vi · tiêu chí · lịch · nhân lực · rủi ro |

> Workflow này **không sinh test scenario, không sinh TC**. Nó nói *cách tổ chức việc kiểm thử*, không nói *kiểm thử cái gì cụ thể*.

## ⚠️ Nguyên tắc thực thi

- **Tất cả output bằng Tiếng Việt**, viết cho PM/khách hàng đọc
- **KHÔNG BỊA** lịch trình, nhân lực, ngày mốc, tên người — agent không có cách nào biết. Thiếu thì để `❓ Chờ <ai> cung cấp`, **không** điền số cho đủ chỗ
- **Phần nào suy ra được từ repo thì tự điền** (danh sách module, độ phủ hiện tại, rủi ro sản phẩm đã phát hiện) — đừng hỏi lại thứ đã có trong `docs/`
- Tiêu chí exit lấy **nguyên** từ `skills-test-summary-reporter`, không tự chế bộ khác
- **KHÔNG bịa số điều khoản chuẩn** — xem mục Chuẩn tham chiếu ngay dưới

---

## Chuẩn tham chiếu — vì sao cần và ghi thế nào

Template này bám **ISO/IEC/IEEE 29119-3 — Test Plan**, chuẩn quốc tế hiện hành về tài liệu kiểm thử. Nó **thay thế IEEE 829** (đã withdrawn), nhưng nhiều khách hàng Nhật/Mỹ và công ty outsourcing vẫn quen mặt template 829 — nên tài liệu xuất ra ánh xạ sang **cả hai**.

**Mục đích của việc ánh xạ:** khi đi nghiệm thu hợp đồng hoặc audit, người duyệt hỏi *"plan này theo chuẩn nào"*. Có bảng ánh xạ thì trả lời được bằng một trang; không có thì phải ngồi giải trình từng mục.

| Mục trong template này | ISO/IEC/IEEE 29119-3 — Test Plan | IEEE 829-2008 |
|---|---|---|
| 1. Mục tiêu | Context of the testing — Project/test sub-process | 2. Introduction |
| 2.1 Trong phạm vi | Context of the testing — Test scope · Test item(s) | 3. Test items · 4. Features to be tested |
| 2.2 Ngoài phạm vi | Context of the testing — Test scope (exclusions) | **5. Features NOT to be tested** |
| 3. Chiến lược kiểm thử | Test strategy — Test sub-processes · Test design techniques · Retesting & regression | 6. Approach |
| 4.1 Tiêu chí vào | Test strategy — Entry criteria | — (829 không có mục riêng) |
| 4.2 Tiêu chí ra | Test strategy — Test completion criteria | 7. Item pass/fail criteria |
| 4.3 Tạm dừng & tiếp tục | Test strategy — Suspension & resumption criteria | 8. Suspension criteria and resumption requirements |
| 5. Môi trường & Dữ liệu test | Test strategy — Test environment requirements · Test data requirements | 10. Environmental needs |
| 6. Nhân lực & Phân công | Staffing — Roles, responsibilities, training needs | 11. Responsibilities · 12. Staffing and training needs |
| 7. Lịch trình & Mốc | Testing activities and estimates · Schedule | 9. Testing tasks · 13. Schedule |
| 8. Rủi ro dự án | **Risk register — Project risks** | 14. Risks and contingencies |
| 9. Sản phẩm bàn giao | Test strategy — Test deliverables | — (nằm rải trong 829) |
| 10. Phê duyệt | — (29119 để tổ chức tự quy định) | 15. Approvals |

### Hai mục chuẩn có, template này CHƯA có — nêu khi phù hợp

| Mục chuẩn | Khi nào bắt buộc thêm vào plan |
|---|---|
| **Risk register — Product risks** | 29119 gộp rủi ro sản phẩm và rủi ro dự án vào một register. Repo này **tách đôi**: rủi ro sản phẩm do RBT đánh giá, nằm ở `docs/testcases/<module>/`. Plan chỉ cần **trỏ tới** nơi đó ở mục 8, không chép lại |
| **Deviations from Organizational Test Strategy** | Khi công ty có chiến lược kiểm thử cấp tổ chức và đợt này làm khác đi (bỏ một cấp test, dùng tool khác, hạ ngưỡng exit). Đây là mục **auditor đọc đầu tiên** — làm khác mà không giải trình là điểm trừ nặng |
| **Testing communication** | Dự án có khách hàng bên ngoài hoặc nhiều đội: ai báo cho ai, tần suất, kênh nào |

> ⚠️ **Quy tắc ghi chuẩn:** ghi **tên mục** của chuẩn (`Test strategy — Suspension & resumption criteria`), **KHÔNG ghi số điều khoản** (`clause 7.3.2`). Số điều khoản đổi giữa các bản phát hành, và agent không có bản chuẩn trong tay để tra — ghi số là bịa. Tên mục thì ổn định và người duyệt tra ra ngay.
>
> ⚠️ **KHÔNG tuyên bố "tuân thủ ISO/IEC/IEEE 29119".** Tuân thủ là kết luận của đánh giá viên, không phải của người viết tài liệu. Câu đúng: *"Tài liệu này biên soạn **theo cấu trúc** ISO/IEC/IEEE 29119-3 — Test Plan"*.

---

## Bước 1: Tự đọc repo trước khi hỏi user

Thu thập những gì **đã có**, để buổi hỏi user ngắn nhất có thể:

| Đọc gì | Lấy ra |
|---|---|
| `docs/requirements/README.md` | Danh sách module · trạng thái recon · ambiguity 🔴 còn treo |
| `docs/requirements/_discovery/system_map.md` | Phụ thuộc giữa module · risk đã ghi nhận |
| `docs/testcases/README.md` | Module nào đã có TC, bao nhiêu TC |
| `traceability_matrix.md` (nếu có) | Độ phủ hiện tại |
| `docs/bugs/` | Bug đang mở — ảnh hưởng tiêu chí vào. Đọc `docs/bugs/README.md` trước để nắm danh mục |
| **Bảng rà soát đặc tính chất lượng** cuối `test_cases_<module>.md` | ⭐ Mọi ô `➖ Ngoài phạm vi` → chép thẳng vào **mục 2.2**. Đây là nguồn đáng tin nhất cho mục ngoài phạm vi: nó do người viết TC ghi lại lúc đã nhìn kỹ module, không phải nhớ lại lúc lập plan |

> Chưa chạy `/discover-system` → plan sẽ thiếu phần phạm vi đáng tin. Đề nghị user chạy khám phá trước, hoặc chấp nhận plan có phạm vi tạm và ghi rõ điều đó.

---

## Bước 2: Hỏi user những thứ KHÔNG suy ra được (CHECKPOINT)

Hỏi **một lượt**, không hỏi lắt nhắt:

| Cần chốt | Vì sao agent không tự biết |
|---|---|
| **Mốc & phạm vi đợt** | Release nào, module nào trong đợt này |
| **Ngoài phạm vi (out of scope)** | ⭐ Quan trọng nhất — xem cảnh báo bên dưới |
| **Lịch & mốc chính** | Ngày bắt đầu test, ngày freeze code, ngày release |
| **Nhân lực** | Mấy người, ai làm module nào, có ai kiêm nhiệm không |
| **Môi trường** | Có môi trường test riêng hay dùng chung, ai dựng, khi nào sẵn sàng |
| **Tiêu chí exit của dự án** | Có bộ riêng không, hay dùng bộ mặc định của skill |
| **Loại test trong phạm vi** | Chức năng · regression · UAT · hiệu năng · bảo mật — cái nào đội này làm, cái nào đội khác |
| **Công cụ quản lý** | Jira/Xray hay file markdown trong repo |
| **Chuẩn tài liệu khách hàng yêu cầu** | Mặc định dùng cấu trúc 29119-3 của template này. Khách có template riêng (hay bắt theo IEEE 829 16 mục) → **hỏi xin file mẫu**, ánh xạ nội dung sang đó thay vì bắt khách đọc cấu trúc lạ |
| **Có Test Strategy cấp tổ chức không** | Có → phải điền mục 11.2 *Điểm làm khác chiến lược chung*. Không có → ghi `Không áp dụng` |

> ### ⚠️ Mục "Ngoài phạm vi" là mục có giá trị pháp lý cao nhất trong plan
>
> Khi có sự cố production, câu hỏi đầu tiên luôn là *"sao QA không test cái này?"*. Plan ghi rõ *"hiệu năng và bảo mật không thuộc phạm vi đợt này"* thì đó là **quyết định đã được thống nhất từ trước**, không phải QA bỏ sót. Mục này trống là rủi ro cho chính đội QA — hỏi user cho bằng được, đừng để trống cho gọn.

**User chưa có thông tin** (hay gặp với lịch và nhân lực) → điền `❓ Chờ PM xác nhận` và **liệt kê những ô còn treo ở đầu tài liệu**, để người duyệt thấy ngay plan chưa hoàn chỉnh ở đâu.

---

## Bước 3: Xuất Master Test Plan

File: `docs/test_plan_<mốc>.md`

```markdown
# Master Test Plan — <Dự án> · <Mốc>

| | |
|---|---|
| Phiên bản tài liệu | v1.0 |
| Ngày lập | 2026-08-12 |
| Người lập | <QA Lead> |
| Người duyệt | ❓ Chờ PM xác nhận |
| Trạng thái | 🟨 Draft — còn 3 ô chờ thông tin (mục 6, 7) |
| Cấu trúc tài liệu | Biên soạn **theo cấu trúc** ISO/IEC/IEEE 29119-3 — Test Plan · ánh xạ ở mục 11 |

> **Ô còn treo:** lịch mốc (mục 7) · nhân lực module Báo cáo (mục 6) · ngày môi trường UAT sẵn sàng (mục 5)

---

## 1. Mục tiêu

Kiểm thử đợt <mốc> nhằm xác nhận <mục tiêu nghiệp vụ>, đảm bảo <phạm vi> hoạt động đúng đặc tả trước khi mở cho người dùng.

## 2. Phạm vi

### 2.1 Trong phạm vi

| Module | Prefix | Số REQ | Số TC hiện có | Ghi chú |
|---|---|---|---|---|
| Khách hàng | CUST | 24 | 36 | Đã có tài liệu |
| Báo cáo | RPT | 0 | 0 | ⚠️ Chưa khảo sát — cần làm trước |

### 2.2 NGOÀI phạm vi (out of scope)

| Không kiểm thử | Lý do | Ai chịu trách nhiệm |
|---|---|---|
| Hiệu năng / tải | Không có công cụ và môi trường trong đợt này | Đội Hạ tầng, đợt sau |
| Bảo mật chuyên sâu (pentest) | Thuê ngoài | Nhà cung cấp X |
| Module Kế toán | Không thay đổi trong release này | — |

> Mục này đã được thống nhất với PM ngày <ngày>. Thay đổi phạm vi phải cập nhật tài liệu và thông báo lại.

## 3. Chiến lược kiểm thử

| Loại test | Có làm? | Cách làm | Ghi chú |
|---|---|---|---|
| Kiểm thử chức năng | ✅ | Manual theo TC — `/execute-test-cases` | |
| Regression | ✅ | Checklist + automation suite | `/generate-checklist-test` |
| Retest bug | ✅ | `/retest-fixed-bugs` | Mọi bug Critical/Major chạy mode FULL |
| Kiểm thử tích hợp cross-module | ✅ | `/generate-cross-module-test-plan` | |
| UAT | ✅ | Khách hàng thực hiện, QA hỗ trợ | |
| Hiệu năng · Bảo mật | ❌ | Ngoài phạm vi — mục 2.2 | |

**Tỷ trọng manual/automation:** <mô tả>. **Thứ tự ưu tiên:** theo mức rủi ro sản phẩm đã đánh giá tại `docs/testcases/<module>/`.

## 4. Tiêu chí Vào / Ra

### 4.1 Tiêu chí VÀO (Entry) — chưa đủ thì CHƯA bắt đầu test

| # | Điều kiện | Trạng thái |
|---|---|---|
| 1 | Build đã deploy lên môi trường test và truy cập được | ❓ |
| 2 | Smoke test của dev đã pass | ❓ |
| 3 | Tài liệu requirements của module trong phạm vi đã có | 🟨 4/5 module |
| 4 | Test case đã viết và đã review | 🟨 |
| 5 | Tài khoản test đủ mọi role trong phạm vi | ❓ |
| 6 | Môi trường test sẵn sàng, có dữ liệu nền | ❓ |

> ⚠️ Bắt đầu test khi chưa đạt tiêu chí vào là nguyên nhân số một khiến kết quả kiểm thử không dùng được — BLOCKED tràn lan, phải chạy lại từ đầu. Thiếu điều kiện nào thì báo PM, đừng bắt đầu rồi chữa sau.

### 4.2 Tiêu chí RA (Exit)

> Lấy nguyên từ `skills-test-summary-reporter`. `/generate-test-summary-report` sẽ chấm lại đúng bộ này.

| # | Tiêu chí | Ngưỡng |
|---|---|---|
| 1 | Bug Critical đang mở | 0 |
| 2 | Bug Major đang mở | 0 hoặc có workaround được PM duyệt |
| 3 | Pass rate TC Priority High | ≥ 95% |
| 4 | Pass rate toàn bộ TC đã chạy | ≥ 90% |
| 5 | Tỷ lệ BLOCKED | ≤ 5% |
| 6 | REQ mức Critical có ≥ 1 TC PASS | 100% |
| 7 | Module trong phạm vi đã có TC và đã chạy | 100% |

☐ Bộ mặc định của agent — **chờ PM xác nhận**  ☐ Bộ tiêu chí riêng của dự án

### 4.3 Tiêu chí TẠM DỪNG (Suspension) & tiếp tục

**Tạm dừng kiểm thử khi:** môi trường sập > 4 giờ · build lỗi không đăng nhập được · > 30% TC BLOCKED cùng một nguyên nhân · phát hiện bug Critical chặn luồng chính.

**Tiếp tục khi:** nguyên nhân đã xử lý, có build mới, và đã chạy lại smoke.

## 5. Môi trường & Dữ liệu test

| | |
|---|---|
| Môi trường | Staging — URL lưu ở `.env`, **không** ghi vào tài liệu này |
| Dùng chung với đội khác? | Có → bật quy tắc auto-skip TC phá huỷ, bắt buộc dọn data sau mỗi lần chạy |
| Trình duyệt / thiết bị | Chrome 1920×1080 (chính) · <bổ sung nếu có> |
| Dữ liệu nền | <mô tả> |
| Quy tắc test data | Random + traceable theo `CLAUDE.md` mục 7 |
| Người dựng môi trường | ❓ |

## 6. Nhân lực & Phân công

| Vai trò | Người | Module phụ trách | Ghi chú |
|---|---|---|---|
| QA Lead | ❓ | | Duyệt TC, lập báo cáo tổng hợp |
| Tester | ❓ | | |

## 7. Lịch trình & Mốc

| Mốc | Ngày | Điều kiện hoàn thành |
|---|---|---|
| Hoàn tất viết TC | ❓ | TC đã review qua `/review-testcases` |
| Bắt đầu thực thi | ❓ | Đạt toàn bộ tiêu chí vào (mục 4.1) |
| Code freeze | ❓ | |
| Báo cáo tổng hợp | ❓ | `/generate-test-summary-report` |

## 8. Rủi ro DỰ ÁN & biện pháp

> Đây là rủi ro **của việc kiểm thử**, khác với rủi ro **sản phẩm** (do RBT đánh giá, nằm ở tài liệu test case).

| Rủi ro | Khả năng | Ảnh hưởng | Biện pháp |
|---|---|---|---|
| Môi trường dùng chung, data bị đội khác sửa | Cao | Kết quả sai lệch, phải chạy lại | Chạy vào khung giờ ít người, dọn data ngay, ghi ID bản ghi đã tạo |
| Chưa có tài khoản role Manager | Cao | Không kiểm được phân quyền — 1 vùng trắng | Xin tài khoản trước ngày <mốc>; không có thì ghi vùng chưa xác minh vào báo cáo |
| Requirements module Báo cáo đến muộn | Trung bình | TC viết vội, chất lượng thấp | Khảo sát UI song song, không chờ tài liệu |
| Thiếu người khi trùng đợt release khác | Trung bình | Trễ mốc | Ưu tiên TC Priority High trước |

## 9. Sản phẩm bàn giao

| Sản phẩm | Nơi lưu | Workflow sinh ra |
|---|---|---|
| Tài liệu requirements | `docs/requirements/<module>/` | `/generate-requirements-from-website` |
| Test cases | `docs/testcases/<module>/` | `/generate-testcases-manual-rbt` |
| Execution report | `docs/executions/<module>/run_*/` | `/execute-test-cases` |
| Bug report | `docs/bugs/<module>/` | `/create-bug-report` |
| Ma trận truy vết | `traceability_matrix.md` | `/generate-traceability-matrix` |
| **Báo cáo tổng hợp** | `docs/executions/test_summary_*.md` | `/generate-test-summary-report` |

## 10. Phê duyệt

| Vai trò | Tên | Ngày | Ý kiến |
|---|---|---|---|
| QA Lead | | | |
| PM / PO | | | |

## 11. Ánh xạ chuẩn tài liệu

### 11.1 Đối chiếu mục

> Tài liệu này biên soạn **theo cấu trúc** ISO/IEC/IEEE 29119-3 — Test Plan (chuẩn hiện hành, thay thế IEEE 829 đã withdrawn). Bảng dưới để người duyệt đối chiếu nhanh; **không** phải tuyên bố đã được đánh giá tuân thủ.

| Mục tài liệu này | ISO/IEC/IEEE 29119-3 — Test Plan | IEEE 829-2008 |
|---|---|---|
| 1 | Context of the testing — Project/test sub-process | 2 |
| 2.1 | Context of the testing — Test scope · Test item(s) | 3, 4 |
| 2.2 | Context of the testing — Test scope (exclusions) | 5 |
| 3 | Test strategy — Test sub-processes · Test design techniques | 6 |
| 4.1 | Test strategy — Entry criteria | — |
| 4.2 | Test strategy — Test completion criteria | 7 |
| 4.3 | Test strategy — Suspension & resumption criteria | 8 |
| 5 | Test strategy — Test environment · Test data requirements | 10 |
| 6 | Staffing — Roles, responsibilities, training needs | 11, 12 |
| 7 | Testing activities and estimates · Schedule | 9, 13 |
| 8 | Risk register — Project risks | 14 |
| 9 | Test strategy — Test deliverables | — |
| 10 | (Tổ chức tự quy định) | 15 |

**Rủi ro sản phẩm** (Risk register — Product risks): không chép vào plan này. Đánh giá theo quy trình RBT, lưu tại `docs/testcases/<module>/` — xem mục 8.

### 11.2 Điểm làm khác chiến lược chung (Deviations)

| Làm khác ở đâu | Chiến lược chung quy định | Đợt này làm | Lý do | Ai duyệt |
|---|---|---|---|---|
| | | | | |

> Không có Test Strategy cấp tổ chức → ghi `Không áp dụng`. **Đừng để trống** — trống nghĩa là "chưa ai xem xét", khác hẳn "đã xem và không có gì khác biệt".
```

---

## Bàn giao

**Checklist:**

- [ ] Mục **2.2 Ngoài phạm vi** đã điền — không để trống
- [ ] Tiêu chí exit **khớp** `skills-test-summary-reporter`, không tự chế bộ khác
- [ ] Mọi ô không suy ra được đều là `❓`, **không có số bịa**
- [ ] Danh sách ô còn treo nằm ở **đầu** tài liệu
- [ ] Rủi ro dự án tách bạch với rủi ro sản phẩm
- [ ] URL, tài khoản nằm ở `.env` — **không** trong tài liệu
- [ ] Phạm vi module lấy từ `docs/requirements/README.md`, khớp trạng thái recon thực tế
- [ ] Mục **11.1** có đủ dòng cho mọi mục 1–10 — thêm mục mới vào plan thì thêm dòng ánh xạ tương ứng
- [ ] Mục **11.2 Deviations** đã điền hoặc ghi rõ `Không áp dụng` — không để bảng rỗng
- [ ] **Không** có số điều khoản chuẩn (`clause 7.x`) ở bất kỳ đâu — chỉ tên mục
- [ ] **Không** có câu tuyên bố "tuân thủ ISO/IEC/IEEE 29119" — chỉ "biên soạn theo cấu trúc"

**Báo cáo cho user:** số module trong phạm vi · số ô còn chờ thông tin (liệt kê rõ) · rủi ro dự án đáng chú ý nhất · nhắc gửi PM duyệt trước khi bắt đầu thực thi.

---

## Cập nhật plan giữa đợt

Phạm vi/lịch đổi → **không viết lại từ đầu**: tăng phiên bản tài liệu, sửa đúng mục đổi, thêm dòng vào bảng phê duyệt. Plan là tài liệu được duyệt — viết đè làm mất dấu vết thứ đã thống nhất.
