# CRM Playwright Automation

Framework E2E automation cho **Perfex CRM** (`https://crm.anhtester.com`) — Playwright + TypeScript, Page Object Model, Allure report, chạy song song sẵn, có CI GitHub Actions.

---

## 1. Yêu cầu môi trường

| Cần | Phiên bản | Ghi chú |
|---|---|---|
| Node.js | ≥ 18 (khuyến nghị 20) | Bắt buộc — cả test lẫn Allure CLI đều chạy bằng Node |
| Java | **KHÔNG cần** | Allure 3 viết bằng TypeScript, không dùng JRE |

> Không phải cài Allure lên máy. CLI nằm trong `devDependencies` của project.

## 2. Cài đặt

```bash
npm install
npx playwright install chromium
```

Tạo file `.env` từ mẫu rồi điền tài khoản test:

```bash
cp .env.example .env
```

| Biến | Ý nghĩa |
|---|---|
| `BASE_URL` | `https://crm.anhtester.com` |
| `TEST_USERNAME` / `TEST_PASSWORD` | Tài khoản test — **không commit** (`.env` đã bị `.gitignore` chặn) |
| `HEADLESS` | `false` = mở browser thật (debug local) · `true` = headless (CI) |
| `WORKERS` | Số luồng song song — **nơi duy nhất chỉnh số luồng** |
| `TIMEOUT` | Timeout mỗi test (ms) |

## 3. Chạy test

```bash
npm test                 # chạy toàn bộ suite (headless/headed theo HEADLESS trong .env)
npm run test:headed      # ép mở browser thật, bất kể .env
npm run test:smoke       # chỉ nhóm @smoke
npm run typecheck        # kiểm tra kiểu TypeScript (Playwright KHÔNG tự type-check)
npx playwright test src/tests/auth/login.spec.ts   # chạy 1 file
```

### Đổi số luồng song song

Parallel **luôn bật**, mặc định **5 luồng**. Đổi ở **đúng một chỗ**:

```bash
# Cách 1 — sửa .env
WORKERS=2

# Cách 2 — chỉ cho lần chạy này
npx playwright test --workers=2
```

Trên CI, số luồng khai ở `env.WORKERS` trong `.github/workflows/crm-playwright.yml` ở gốc repo (runner GitHub 2 vCPU nên cân nhắc hạ xuống 2–3).

> 🚨 Test đỏ khi chạy song song là **test sai**, phải sửa test data / khử state dùng chung — **không** hạ `WORKERS=1` để giấu.

## 4. Mở report

```bash
npm run report        # sinh Allure report rồi mở trình duyệt
```

Không có bước cài đặt nào lên máy — `allure` nằm trong `node_modules`.

| Tình huống | Cách xem |
|---|---|
| Máy có Node (thường gặp) | `npm run report` |
| Máy **không có gì** | `npm run report:single` → gửi file `reports/allure-report-single/index.html` (1 file, mở bằng trình duyệt bất kỳ) |
| Chỉ cần report của Playwright | `npx playwright show-report reports/html` |

Toàn bộ output nằm trong `reports/` (đã `.gitignore`):

```
reports/
├── allure-results/         # raw results
├── allure-report/          # HTML report Allure
├── allure-report-single/   # report gộp 1 file
├── html/                   # HTML report của Playwright
├── logs/                   # log thực thi (winston)
└── test-artifacts/         # video / trace khi fail
```

> 🔒 Ảnh và video trong `reports/` chụp hệ thống thật — **không commit, không gửi ra ngoài** khi chưa rà lại nội dung.

## 5. Cấu trúc project

```
crm-playwright-automation/
├── playwright.config.ts        # viewport 1920x1080, parallel, reporter, baseURL
├── allurerc.mjs                # output Allure report
├── allurerc.single.mjs         # bản report gộp 1 file
├── .env.example                # mẫu biến môi trường
├── src/
│   ├── pages/                  # Page Object — locator + hành vi, KHÔNG chứa assertion
│   │   ├── base.page.ts
│   │   ├── login.page.ts
│   │   └── dashboard.page.ts
│   ├── fixtures/
│   │   └── base.fixture.ts     # inject page object + fixture đăng nhập sẵn + ảnh cuối test
│   ├── utils/
│   │   ├── env.config.ts       # đọc .env + danh sách route
│   │   ├── allure.ts           # khai metadata test (testId/severity/description)
│   │   ├── test-data.ts        # sinh data unique + traceable
│   │   └── logger.ts           # winston -> reports/logs/
│   └── tests/
│       ├── auth/               # login.spec.ts · login-invalid.spec.ts
│       └── dashboard/          # dashboard.spec.ts
└── test-data/                  # data cho test data-driven
```

> File CI **không** nằm trong thư mục này — xem mục 8.

## 6. Quy ước viết test

| Mục | Quy ước |
|---|---|
| Tên file test | `<module>.spec.ts` — kebab-case |
| Page class | PascalCase + hậu tố `Page`, file `<ten>.page.ts` |
| Tên test | **Tiếng Việt**, mô tả hành vi (`Đăng nhập thành công với tài khoản admin hợp lệ`) |
| Metadata | Mỗi test gọi `allureMeta({ testId, description, severity })` ở dòng đầu |
| Tag | Khai bằng `test('...', { tag: ['@smoke'] }, ...)` của Playwright — để `--grep @smoke` lọc được; `allure-playwright` tự map sang tag trong report. **Không** khai tag trong `allureMeta` |
| TC ID | `CRM_<MODULE>_TC_<3 số>` — khoá truy vết sang manual test case |
| Thân test | Bọc trong `test.step('Arrange: …')` / `'Act: …'` / `'Assert: …'` |
| Locator | Khai trong Page class, ưu tiên `getByRole` / `getByLabel`; CSS chỉ khi không còn lựa chọn semantic |
| Chờ đợi | Chỉ smart wait (`expect().toBeVisible()`…) — **cấm** `waitForTimeout` |
| Test data | Field unique sinh bằng `TestData` (timestamp + random), không hardcode |
| Assertion | Luôn kèm message Tiếng Việt giải thích kỳ vọng |

### Thêm một test mới

1. Thêm locator + hành vi vào Page class (hoặc tạo page mới kế thừa `BasePage`).
2. Đăng ký page vào `src/fixtures/base.fixture.ts` nếu muốn inject thẳng vào test.
3. Viết spec trong `src/tests/<module>/`, gọi `allureMeta` rồi chia 3 step Arrange/Act/Assert.

## 7. Test hiện có

| TC ID | Tên test | Nhóm |
|---|---|---|
| `CRM_LOGIN_TC_001` | Đăng nhập thành công với tài khoản admin hợp lệ | smoke |
| `CRM_LOGIN_TC_002` | Đăng nhập thất bại khi nhập sai mật khẩu | regression |
| `CRM_LOGIN_TC_003` | Đăng nhập thất bại khi email không tồn tại trong hệ thống | regression |
| `CRM_LOGIN_TC_004` | Không đăng nhập được khi bỏ trống email và mật khẩu | regression |
| `CRM_DASH_TC_001` | Điều hướng từ Dashboard sang trang Khách hàng | smoke |

## 8. CI/CD

Workflow nằm ở **`.github/workflows/crm-playwright.yml` tại gốc repo** (không nằm trong thư mục này) — GitHub Actions chỉ đọc `.github/workflows/` ở gốc repository, đặt trong thư mục con sẽ không chạy.

Chạy khi `push` vào `main`, mỗi `pull_request`, và chạy tay qua `workflow_dispatch`.

Trước khi bật CI, khai 3 **GitHub Secrets**: `BASE_URL`, `TEST_USERNAME`, `TEST_PASSWORD`. Toàn bộ `reports/` được upload thành artifact `test-reports` (giữ 14 ngày), kể cả khi test fail.

> Nếu tách thư mục này thành repo riêng: chuyển file workflow vào `.github/workflows/` của repo mới và **bỏ** 3 dòng `working-directory` / `cache-dependency-path` / tiền tố `crm-playwright-automation/` trong `path` của bước upload artifact.

## 9. Giới hạn đã biết

- Hệ thống đích là **môi trường demo dùng chung** — dữ liệu có thể bị người khác thay đổi. Test hiện tại chỉ đọc và điều hướng, chưa tạo/sửa/xoá bản ghi. Khi thêm test có ghi dữ liệu, phải sinh data riêng bằng `TestData` và tự dọn ở cuối test.
- Trang login đặt ở `/admin/authentication`; sau khi đăng nhập hệ thống chuyển về `/admin/`.
- `CRM_LOGIN_TC_002` (sai mật khẩu) cố tình đăng nhập hỏng vào **chính tài khoản trong `.env`** — đúng bản chất ca kiểm thử, nên `test-data/login-invalid.json` để `email: null` và lấy từ `env.username` thay vì chép email ra file commit được. Nếu sau này hệ thống bật khoá tài khoản sau N lần sai, TC này sẽ làm đỏ các test khác chạy song song — khi đó phải cấp một tài khoản riêng chỉ dùng cho ca sai mật khẩu.
- `CRM_LOGIN_TC_004`: bấm Login khi form trống thì hệ thống **không hiển thị thông báo nào**, chỉ đứng yên. Test đang khẳng định đúng hành vi thực tế này, không phải khẳng định "có báo lỗi".
- Link trên sidebar dùng icon Font Awesome — glyph của icon cộng vào accessible name, nên `menuItem()` cố ý **không** dùng `exact: true`.
