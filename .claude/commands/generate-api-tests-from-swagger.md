---
description: Sinh API test cases và automation scripts từ Swagger/OpenAPI specification. Hỗ trợ 2 mode — SPEC (chỉ test cases) và FULL (test cases + automation scripts).
skills:
  - skills-qa-automation-engineer
  - skills-test-data-generator
---

# Command: Sinh API Tests từ Swagger/OpenAPI

> **BẮT BUỘC (MANDATORY SKILL):** Bạn PHẢI nạp và đọc kỹ nội dung của skill **`skills-qa-automation-engineer`** (tại `.claude/skills/skills-qa-automation-engineer/SKILL.md`) trước khi bắt đầu. Ngoài ra, tham khảo thêm skill **`skills-test-data-generator`** để sinh test data đúng chuẩn.

Command này giúp agent phân tích Swagger/OpenAPI specification, xác định các endpoints, sinh API test cases có cấu trúc, và (tùy mode) tự động sinh automation scripts hoàn chỉnh.

## ⚠️ Nguyên tắc thực thi

- **Tất cả output bằng Tiếng Việt**
- **KHÔNG đoán** schema/endpoint — phải đọc spec thực tế (JSON/YAML)
- **KHÔNG tin spec suông** — spec là *lời khai*, không phải *hành vi*. Phải gọi thật để kiểm chứng (Bước 1b)
- **Phải chờ user xác nhận** scope tại Bước 2 trước khi sinh chi tiết
- Nếu user chưa cung cấp Swagger URL/file → hỏi trước khi bắt đầu
- ⚠️ **Rule E3:** Khi test FAIL → tự đọc log → phân tích → sửa → chạy lại. KHÔNG hỏi user trong quá trình fix lỗi

## 🚨 Quy tắc dữ liệu test (BẮT BUỘC — đọc trước khi gọi request đầu tiên)

> Vi phạm mục này đã từng **xoá mất tài khoản gốc của hệ thống demo**. Đây không phải khuyến nghị.

| Quy tắc | Chi tiết |
|---|---|
| **TUYỆT ĐỐI KHÔNG lấy bản ghi có sẵn làm mục tiêu ghi/xoá** | Test BOLA/IDOR **phải** tự tạo **2 tài khoản của riêng mình** (`userA`, `userB`) rồi dùng token của A tấn công tài nguyên của B. **CẤM** lấy `id` bất kỳ từ `GET /api/...` danh sách rồi `PATCH`/`DELETE` — id đó là dữ liệu thật của người khác |
| **Chỉ ghi/xoá trên bản ghi do chính test tạo ra** | Áp dụng cho **mọi** method ghi (`POST`/`PUT`/`PATCH`/`DELETE`), kể cả khi user nói "được xoá thoải mái" |
| **Đặt tên traceable** | `auto_<module>_<timestamp>` cho mọi bản ghi sinh ra — để dọn được và truy ngược được |
| **Dọn ngay sau khi xong** | Xoá hết bản ghi đã tạo trong cùng phiên. Ghi rõ ở báo cáo: đã tạo bao nhiêu, đã dọn bao nhiêu |
| **Ghi nhật ký sự cố** | Lỡ tay đụng dữ liệu thật → báo user **ngay**, khôi phục tối đa, ghi vào mục "Sự cố" của `api_map.md`: khôi phục được gì, **không** khôi phục được gì |
| **Xoá là không hoàn tác được kể cả khi tạo lại** | ID sinh mới, các bản ghi liên quan mất liên kết (mồ côi). "Tạo lại y hệt" **không** bằng "chưa từng xoá" |

## 2 Chế độ (Mode)

| Mode | Khi nào sử dụng | Output |
|---|---|---|
| **SPEC** (mặc định) | User cần API test cases dưới dạng tài liệu | API Test Cases (Markdown) + Test Data Matrix |
| **FULL** | User yêu cầu cả automation scripts | Như SPEC + Automation Scripts + Project Structure |

> Nếu user nói "generate automation", "viết code test API", hoặc yêu cầu scripts → tự động chuyển sang **Mode FULL**.

## Các bước thực hiện

### Bước 1: Tiếp nhận & Phân tích Spec (Parse & Analyze)

1. **Thu thập Swagger/OpenAPI spec** từ user:
   - **URL trực tiếp** (VD: `https://api.example.com/swagger.json`) → dùng `WebFetch` để fetch
   - **File local** (JSON/YAML) → dùng `Read` để đọc
   - **Swagger UI URL** → trích xuất URL spec gốc (thường là `/v2/api-docs` hoặc `/v3/api-docs`)
   - **Scalar API Reference URL** → inspect HTML để tìm `data-configuration` chứa URL spec (thường là `/swagger/json`, `/reference/json`, hoặc relative path trong attribute `url`). VD: `https://book.anhtester.com/swagger` → spec tại `https://book.anhtester.com/swagger/json`
   - **Các dạng API Doc khác** (Redoc, Stoplight, RapiDoc) → tìm URL spec trong page source hoặc network requests
2. **Snapshot spec ngay** vào `docs/requirements/_<hệ-thống>/_discovery/sources/openapi_<YYYY-MM-DD>.json` — spec online sẽ đổi, tài liệu phải neo vào một bản cố định.
3. **Parse spec** và trích xuất thông tin:
   - Base URL, API version, authentication scheme (Bearer, API Key, OAuth2, Basic)
   - Danh sách tất cả endpoints: `method + path`
   - Request parameters: path, query, header, body (schema + required fields)
   - Response schemas: status codes, response body structure
   - Models/Definitions: reusable data models
4. **🔑 Lập ma trận Auth theo TỪNG operation (BẮT BUỘC — dễ bỏ sót nhất):**

   `security` khai ở **cấp gốc** là mặc định, nhưng **mỗi operation được ghi đè**. Operation có `security: []` là **công khai — không cần token**, dù spec gốc khai `BearerAuth`. Swagger UI **không** hiển thị rõ khác biệt này.

   ```js
   // Với mỗi operation:
   const congKhai = Array.isArray(op.security) && op.security.length === 0;
   ```

   Xuất bảng: `| Method | Path | 🌐 Công khai / 🔒 Cần token |` và **soi kỹ**: endpoint nào trả dữ liệu cá nhân (user, đơn hàng, hồ sơ) mà lại công khai → đánh dấu 🔴 ngay.

5. **Phân loại endpoints** theo nhóm:
   - **CRUD operations** — Create, Read, Update, Delete
   - **Authentication** — Login, Register, Token refresh
   - **Business Logic** — Các API xử lý nghiệp vụ phức tạp
   - **Utility** — Health check, config, metadata
6. **Gán mã module + mã hệ thống**:
   - Ưu tiên dùng `tags` của spec làm ranh giới module
   - Mã hệ thống ngắn (2–4 ký tự, VD `BK`) đặt trước mã module → `REQ-<HỆ_THỐNG>-<MODULE>-<nn>` · TC ID `<HỆ_THỐNG>_<MODULE>_TC_<nnn>`
   - **Đọc `README.md` danh mục trước** để không trùng prefix đã chiếm

### Bước 1b: Kiểm chứng spec bằng gọi thật (BẮT BUỘC — KHÔNG được bỏ)

> Spec khai `403` không có nghĩa hệ thống **trả** `403`. Spec khai `required` không có nghĩa hệ thống **chặn**. Sinh TC từ spec chưa kiểm chứng = sinh ra một tập TC sai kỳ vọng, chạy cái nào cũng FAIL nhầm.

Gọi **10–20 request thật** (đọc trước, ghi sau — và ghi thì tuân thủ "Quy tắc dữ liệu test" ở trên):

| Nhóm probe | Kiểm cái gì | Vì sao |
|---|---|---|
| **GET công khai không token** | Ma trận auth ở Bước 1.4 có đúng không | Bắt được rò rỉ PII — phát hiện giá trị nhất, rẻ nhất |
| **Gọi endpoint cần token, không gửi token** | Có thật sự trả 401 không | Xác nhận hàng rào auth tồn tại |
| **Đăng ký tài khoản mới → gọi endpoint nhạy cảm** | User quyền thấp nhất làm được gì | Lộ ra hệ thống **có** mô hình phân quyền hay không |
| **Bơm giá trị ngoài ràng buộc** (số âm, ngoài enum, khoá ngoại không tồn tại) | Validate ở server hay chỉ khai trong spec | Phân biệt ràng buộc **thật** với ràng buộc **trên giấy** |
| **Đọc response header + `Set-Cookie`** | `HttpOnly`/`Secure`/`SameSite`, header lộ phiên bản | Không nhìn thấy trong spec, chỉ thấy khi gọi thật |
| **Đối chiếu status code thực tế vs spec** | 200 vs 201, 400 vs 422 | Kỳ vọng sai là nguồn FAIL giả số 1 |

Mỗi sai lệch ghi thành một **phát hiện** `F-nn` kèm **bằng chứng nguyên văn** (request + status + trích body), phân mức 🔴/🟠/🟡. Chỗ nào hệ thống làm **đúng** cũng ghi lại — đó là TC chống hồi quy.

### Bước 1c: Ghi bản đồ API xuống đĩa

Xuất `docs/requirements/_<hệ-thống>/_discovery/api_map.md` — **tên file bất biến**, gồm:

1. Bảng module & prefix (mã module · số op · số công khai · dải REQ · dải TC ID)
2. Danh mục endpoint đầy đủ, có cột **Auth** và cột **status codes khai trong spec**
3. Kết quả kiểm chứng Bước 1b — phát hiện `F-nn` kèm bằng chứng
4. **Ambiguity `AMB-<HỆ_THỐNG>-nn`** — thứ spec không trả lời được (mô hình phân quyền · ràng buộc nghiệp vụ · rate limit · giới hạn upload)
5. Ghi chú kỹ thuật cho automation (schema rỗng · nhiều content-type · cookie vs header · dữ liệu rác sẵn có)
6. Nhật ký khám phá + mục **Sự cố** (nếu có)

Cập nhật `README.md` danh mục hệ thống (tạo mới nếu chưa có): bảng module · prefix đã chiếm · AMB 🔴 treo · phát hiện bảo mật.

> ⚠️ **Tầng khám phá KHÔNG cấp mã `REQ`** — chỉ cấp **mã module**. Cùng quy tắc với `_discovery/system_map.md` bên nhánh UI.

### Bước 2: Xác nhận Scope & Tech Stack (CHECKPOINT — ⏸️ DỪNG LẠI)

1. **Trình bày tóm tắt** cho user review:
   - Tổng số endpoints phát hiện (phân nhóm) + **số công khai / số cần token**
   - Authentication method
   - Danh sách endpoint groups + số lượng API mỗi nhóm
   - **Phát hiện `F-nn` mức 🔴 từ Bước 1b** — báo ngay, đừng đợi đến báo cáo cuối
   - **Ambiguity 🔴 đang chặn** — kèm rõ nó chặn nhóm TC nào
   - Mode đề xuất (SPEC hay FULL)
2. **Hỏi user xác nhận:**
   - "Bạn muốn test tất cả endpoints hay chỉ tập trung vào nhóm nào?"
   - "Bạn muốn output là test cases (SPEC) hay cả automation scripts (FULL)?"
   - **Từng AMB 🔴:** hành vi quan sát được là **lỗi cần báo** hay **thiết kế cần xác nhận**? Câu trả lời đổi hẳn nội dung TC — cùng một hành vi ra TC bug hoặc TC hồi quy
   - Nếu Mode FULL: "Tech stack mong muốn?" (mặc định theo bảng bên dưới)
3. **Chờ user xác nhận** scope trước khi sang Bước 3

> Nếu user chưa chốt được AMB 🔴 (phải hỏi dev/BA): **vẫn sinh TC** cho phần không phụ thuộc, đánh dấu phần còn lại `⚪ Chờ chốt <mã AMB>` — **không** dừng cả workflow vì một câu hỏi treo.

**Tech Stack mặc định (Mode FULL):**

| Framework | Ngôn ngữ | Khi nào dùng |
|---|---|---|
| **REST Assured** | Java | Mặc định cho Java projects, TestNG runner |
| **Playwright API Testing** | TypeScript | Khi user dùng Playwright hoặc TypeScript stack |
| **Supertest + Jest** | TypeScript/JS | Khi user dùng Node.js backend |
| **Requests + Pytest** | Python | Khi user dùng Python stack |

### Bước 3: Sinh API Test Scenarios & Test Data

1. **Bao phủ 12 HTTP Status Codes tiêu chuẩn:**
   - **200/201:** Success
   - **400:** Validation error / Malformed JSON
   - **401:** Missing / Invalid Auth Token
   - **403:** Forbidden / BOLA IDOR User A -> User B
   - **404:** Not Found
   - **406:** Not Acceptable (Accept header mismatch)
   - **409:** Conflict (Duplicate record / Race condition concurrency)
   - **413:** Payload Too Large
   - **415:** Unsupported Media Type (Content-Type mismatch)
   - **429:** Too Many Requests (Rate Limiting)
   - **500:** Server error

2. **Với mỗi endpoint** trong scope đã xác nhận, sinh test scenarios theo 7 loại:
   - **✅ Happy Path** — Request hợp lệ, response đúng schema + status code
   - **❌ Negative — Validation** — Thiếu required fields, sai data type, vượt max length
   - **❌ Negative — Auth** — Không có token, token hết hạn, token sai role
   - **🔲 Boundary** — Min/max values, empty string, null, special characters
   - **⚡ Edge Cases** — Concurrent requests, duplicate creation, large payload, unicode/emoji
   - **🔒 Security** — SQL injection, XSS, IDOR, Mass Assignment, ReDoS, sensitive data exposure
   - **📄 Pagination & Filtering** — Phân trang, sắp xếp, tìm kiếm

3. **Sinh Test Data Matrix** (sử dụng skill `skills-test-data-generator`):
   - Data valid cho Happy Path
   - Data invalid cho Negative cases (mỗi field 1 bộ negative)
   - Boundary values theo schema constraints (minLength, maxLength, min, max, pattern)
   - Data phải **unique + traceable** (VD: `auto_api_1712049200@test.com`)

4. **Field-Level Validation cho Request Body (BẮT BUỘC):**

   Với mỗi endpoint có request body (POST/PUT/PATCH), agent **PHẢI liệt kê từng field** trong body và sinh negative TCs riêng cho TỪNG field (String, Email, Phone, Number, Boolean, Date, Enum, Array, Nested Object, File).

5. **OWASP API Security Testing Checklist:**

   | Loại | Test Scenarios |
   |---|---|
   | **Injection** | SQL injection trong query params (`?id=1 OR 1=1`) · SQL injection trong body fields · XSS trong input fields · Command injection (nếu API xử lý shell) |
   | **BOLA / IDOR (403)** | Truy cập resource của user khác bằng ID (`GET /users/999` khi user chỉ có quyền xem user 123) · Thay đổi ID trong PUT/DELETE để sửa/xóa resource không phải của mình |
   | **Mass Assignment** | Gửi kèm trường đặc quyền trong request body (như `role: "admin"`, `is_admin: true`) xem API có tự gán quyền không |
   | **Auth Bypass (401/403)** | Gọi API không có token → 401 · Token hết hạn → 401 · Token role thấp gọi API role cao → 403 · Token bị tamper → 401 |
   | **ReDoS** | Truyền chuỗi quá dài (> 10.000 chars) vào các field validate regex để kiểm tra Denial of Service |
   | **Sensitive Data Exposure** | Response không trả về password/hash/secretKeys · Response không leak internal IDs/stack traces · Headers không leak server info (`X-Powered-By`, `Server`) |
   | **Rate Limiting (429)** | Gửi nhiều request liên tục → phải bị giới hạn (429) · Brute force login → lock account |
   | **CORS & Headers** | Kiểm tra `Access-Control-Allow-Origin` header · Content-Type header (`415`) · Accept header (`406`) |

6. **Pagination & Filtering Tests (cho GET List endpoints):**

   Pagination (page/limit), Sorting (sort/order), Filtering (by status, date, name), Search (partial match, case-insensitive).

7. **Phân biệt PUT vs PATCH (nếu API có cả 2):**
   - **PUT:** Gửi đầy đủ fields -> update toàn bộ, reset missing optional fields.
   - **PATCH:** Gửi partial fields -> chỉ update field chỉ định, giữ nguyên các field khác.

### Bước 4: Đóng gói API Test Cases (Output — Mode SPEC)

1. **Ghi file đúng chỗ — KHÔNG để lạc ra root:**

   ```
   docs/testcases/_<hệ-thống>/<module>/test_cases_<module>.md    ← INDEX, TÊN FILE BẤT BIẾN
   docs/testcases/_<hệ-thống>/<module>/parts/part_NN_<slug>.md   ← khi > 40 TC
   ```

   **Một file mỗi module**, không gộp 35 endpoint vào một file khổng lồ. Tên file bám đúng mẫu để `/generate-traceability-matrix` map được RTM.

2. Cấu trúc mỗi file:
   - **Tổng quan** — Base URL, Version, Auth method, số endpoint của module, link ngược về `_discovery/api_map.md`
   - **Endpoint Catalog** — Bảng: `| # | Method | Path | Auth | Mô tả | Số Test Cases |`
   - **Test Cases chi tiết** — theo từng endpoint, mỗi TC có **TC ID** `<HỆ_THỐNG>_<MODULE>_TC_<nnn>` và cột **REQ liên quan**
   - **Test Data Matrix** — data valid/invalid/boundary cho mỗi model
   - **Dependencies & Execution Order** — thứ tự chạy test
   - **TC treo** — TC chưa viết được, kèm mã AMB đang chặn

3. **Cập nhật `README.md` danh mục**: dải TC ID đã dùng · mã kế tiếp · ngày cập nhật · dòng nhật ký.

4. Nếu user chọn **Mode SPEC** → **KẾT THÚC** tại đây

### Bước 5: Sinh Automation Scripts (Mode FULL)

1. **Thiết kế project structure** phù hợp với framework (REST Assured / Playwright API / Pytest Requests / Supertest).
2. **Sinh code:** Base API class, Model/DTO classes, API client classes, Test Data generators, Test classes.
3. **Assertions bắt buộc:**
   - ✅ HTTP Status Code (exact match)
   - ✅ Response body structure & JSON Schema validation
   - ✅ Response time SLA (< 2 giây)
   - ✅ Headers & Sensitive Data masking check
4. **Best practices:**
   - Dynamic Auth Token (không hardcode) — base URL + credentials để ở `.env`, **không** commit
   - Parameterized tests
   - Teardown/Cleanup data sau khi test (DELETE record vừa tạo)
   - **Fixture 2 tài khoản** (`userA`, `userB`) do chính suite tạo ra, dùng cho mọi TC BOLA/IDOR — theo "Quy tắc dữ liệu test" ở đầu file
5. **Output report** gom vào `reports/` theo [`reporting_rules.md`](../rules/reporting_rules.md) — tên test Tiếng Việt, có Severity/Tags/TC ID, step Arrange/Act/Assert. Với API test, phần thay cho screenshot là **attach request + response thật** (đã che token và dữ liệu nhạy cảm) ở cuối **mọi** test, cả PASS lẫn FAIL.

### Bước 6: Chạy thử nghiệm & Tự sửa lỗi (Execution & Auto-Heal)

1. **Chạy test** bằng tool `Bash` (hoặc `PowerShell` trên Windows) — `npm test`, `mvn test`, `pytest`…
2. **Test chạy lâu** → `run_in_background: true`, nhận thông báo khi xong. **KHÔNG** poll bằng `sleep`.
3. **Auto-Heal:** Nếu FAIL → tự đọc log, sửa code và chạy lại (tối đa 5 lần) mà KHÔNG làm phiền user.
4. **⚠️ Chỉ sửa test, KHÔNG sửa kỳ vọng để né bug app.** Test FAIL vì hệ thống sai (như phát hiện `F-nn` ở Bước 1b) → giữ nguyên assertion, đánh dấu TC là **đang phơi bug**, đề xuất `/create-bug-report`. Hạ assertion cho xanh báo cáo là **cấm**.
5. **Dọn dữ liệu** sau lần chạy cuối. Báo cáo rõ: tạo bao nhiêu bản ghi, dọn bao nhiêu, còn sót gì.

## Output

### Cả 2 mode — tầng khám phá (Bước 1b + 1c)

| File | Nội dung |
|---|---|
| `docs/requirements/_<hệ-thống>/_discovery/sources/openapi_<ngày>.json` | Snapshot spec gốc |
| `docs/requirements/_<hệ-thống>/_discovery/api_map.md` | Bản đồ API · ma trận auth · phát hiện `F-nn` có bằng chứng · ambiguity `AMB-nn` · ghi chú automation |
| `docs/requirements/_<hệ-thống>/README.md` | Danh mục hệ thống — module · prefix · AMB 🔴 · phát hiện bảo mật |

### Mode SPEC
- `docs/testcases/_<hệ-thống>/<module>/test_cases_<module>.md` — mỗi module 1 file, đủ 12 Status Codes, 7 loại Test Scenarios, OWASP Security, Test Data Matrix, TC ID truy vết được.

### Mode FULL
- Tất cả output của Mode SPEC, cộng source code automation hoàn chỉnh + report trong `reports/`.
- **"PASS 100%" chỉ áp dụng cho TC mà hệ thống đúng.** TC phơi bug thật thì FAIL là kết quả đúng — báo cáo tách riêng 2 nhóm, kèm đề xuất bug report.

---

## Checklist trước khi báo hoàn thành

- [ ] Spec đã snapshot xuống `_discovery/sources/`
- [ ] Ma trận auth lập theo **từng operation**, đã soi endpoint công khai trả dữ liệu cá nhân
- [ ] Đã gọi thật kiểm chứng, mỗi sai lệch có mã `F-nn` + bằng chứng nguyên văn
- [ ] Ambiguity ghi thành `AMB-nn`, AMB 🔴 đã báo user ở checkpoint Bước 2
- [ ] File TC nằm đúng `docs/testcases/_<hệ-thống>/<module>/`, **không** lạc ra root
- [ ] `README.md` danh mục đã cập nhật prefix · dải TC ID · nhật ký
- [ ] Mọi bản ghi test đã dọn; số tạo / số dọn ghi rõ trong báo cáo
- [ ] **Không** bản ghi có sẵn nào của hệ thống bị sửa hoặc xoá