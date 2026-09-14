import { test, expect } from '../../fixtures/base.fixture';
import { env } from '../../utils/env.config';
import { allureMeta, Severity } from '../../utils/allure';
import loginData from '../../../test-data/login-invalid.json';

test.describe('Đăng nhập - dữ liệu không hợp lệ', () => {
  for (const data of loginData.invalidLogins) {
    // email = null trong file data nghĩa là dùng chính tài khoản test (ca "sai mật khẩu")
    const email = data.email ?? env.username;

    test(data.title, { tag: ['@regression', '@login', '@negative'] }, async ({ loginPage }) => {
      await allureMeta({
        testId: data.testId,
        description:
          `Kiểm tra hệ thống từ chối đăng nhập với bộ dữ liệu không hợp lệ và hiển thị thông báo `
          + `"${data.expectedError}", người dùng vẫn ở lại trang Đăng nhập.`,
        severity: Severity.CRITICAL,
      });

      await test.step('Arrange: Mở trang Đăng nhập', async () => {
        await loginPage.open();
      });

      await test.step('Act: Đăng nhập bằng dữ liệu không hợp lệ', async () => {
        await loginPage.login(email, data.password);
      });

      await test.step('Assert: Hệ thống báo lỗi và giữ người dùng ở trang Đăng nhập', async () => {
        await expect(loginPage.errorAlert, 'Thông báo lỗi phải hiển thị').toBeVisible();
        await expect(loginPage.errorAlert, 'Nội dung thông báo lỗi không đúng kỳ vọng')
          .toHaveText(data.expectedError);
        await expect(loginPage.loginButton, 'Vẫn phải ở trang Đăng nhập').toBeVisible();
      });
    });
  }
});
