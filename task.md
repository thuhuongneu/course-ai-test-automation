# Framework Setup Progress — crm-playwright-automation

Stack: Playwright + TypeScript · POM · Allure 3 (CLI cục bộ) · Parallel 5 luồng · GitHub Actions
Target: https://crm.anhtester.com (login: /admin/authentication)

- [x] Bước 1: Thu thập yêu cầu + xác nhận với user
- [x] Bước 2: Scaffold project structure
- [x] Bước 3: Sinh base classes (env.config, BasePage, fixtures, logger, TestData, allureMeta)
- [x] Bước 4: Sinh example tests — locator lấy từ DOM thật (5 TC: 4 login + 1 dashboard)
- [x] Bước 5: Reporting (Allure 3 cục bộ + HTML) & CI GitHub Actions
- [x] Bước 6: Verify & Deliver

## Kết quả verify

| Hạng mục | Kết quả |
|---|---|
| `npm run typecheck` | ✅ 0 lỗi |
| Suite chạy headed | ✅ 5/5 PASS, **2 lần liên tiếp** |
| Parallel | ✅ "Running 5 tests using 5 workers" |
| Viewport thực tế | ✅ 1920×1080 (đo bằng `page.viewportSize()`) |
| `--grep @smoke` | ✅ lọc ra 2 test |
| Allure CLI cục bộ | ✅ `npx allure --version` → 3.17.0, không cài gì lên máy |
| Metadata report | ✅ 5/5 test đủ tên Tiếng Việt · description · severity · tags · testId |
| Ảnh cuối test | ✅ PASS → `trang_thai_cuoi_cua_test` · FAIL → `trang_thai_khi_that_bai` (đã test thật) |
| Attachment stdout/stderr | ✅ không có |
| Lộ mật khẩu trong report | ✅ không (đã bật `detail: false`) |
| Output gom vào `reports/` | ✅ root project sạch |
| CI workflow | ✅ `.github/workflows/crm-playwright.yml` — headless, parallel, secrets, `if: always()` |

## Code review

Đã review bằng subagent theo Definition of Done → 3 lỗi nghiêm trọng + 7 mục nên sửa, **đã sửa hết**:

1. `test:smoke` chạy 0 test (tag Allure không phải tag Playwright) → chuyển sang `{ tag: [...] }`
2. Viewport 1920×1080 bị preset `devices['Desktop Chrome']` ghi đè thành 1280×720 → chuyển vào project `use`
3. TC_002 hardcode email tài khoản thật trong JSON → lấy từ `env.username`
4. TC_004 tên không khớp assertion → đổi tên + assert đúng hành vi thật
5. Thiếu assertion message · 6. Test gọi thẳng locator → thêm `LoginPage.submit()`
7. Xoá code chết · 8. Validate `WORKERS`/`TIMEOUT` · 9. README sai lệch · 10. Thêm `npm run typecheck` + step CI
