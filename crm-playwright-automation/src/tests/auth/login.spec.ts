import { test, expect } from '../../fixtures/base.fixture';
import { env, routes } from '../../utils/env.config';
import { allureMeta, Severity } from '../../utils/allure';

test.describe('Đăng nhập', () => {
  test(
    'Đăng nhập thành công với tài khoản admin hợp lệ',
    { tag: ['@smoke', '@login'] },
    async ({ loginPage, dashboardPage, page }) => {
      await allureMeta({
        testId: 'CRM_LOGIN_TC_001',
        description:
          'Kiểm tra người dùng đăng nhập bằng email và mật khẩu hợp lệ thì được chuyển vào '
          + 'trang Dashboard và thấy thanh menu điều hướng bên trái.',
        severity: Severity.BLOCKER,
      });

      await test.step('Arrange: Mở trang Đăng nhập', async () => {
        await loginPage.open();
        await expect(loginPage.loginButton, 'Form đăng nhập phải hiển thị').toBeVisible();
      });

      await test.step('Act: Đăng nhập bằng tài khoản admin', async () => {
        await loginPage.login(env.username, env.password);
      });

      await test.step('Assert: Hệ thống chuyển vào Dashboard', async () => {
        await expect(page, 'URL phải chuyển sang trang Dashboard').toHaveURL(routes.dashboard);
        await expect(dashboardPage.sidebarMenu, 'Menu điều hướng phải hiển thị sau khi đăng nhập').toBeVisible();
        await expect(dashboardPage.menuItem('Dashboard'), 'Menu Dashboard phải có trên sidebar').toBeVisible();
      });
    },
  );

  test(
    'Không đăng nhập được khi bỏ trống email và mật khẩu',
    { tag: ['@regression', '@login'] },
    async ({ loginPage, page }) => {
      await allureMeta({
        testId: 'CRM_LOGIN_TC_004',
        description:
          'Kiểm tra khi bấm Login mà chưa nhập email/mật khẩu thì hệ thống không đăng nhập, '
          + 'người dùng vẫn ở nguyên trang Đăng nhập. Lưu ý: hệ thống hiện KHÔNG hiển thị thông báo '
          + 'nào trong trường hợp này (hành vi thực tế đã khảo sát).',
        severity: Severity.NORMAL,
      });

      await test.step('Arrange: Mở trang Đăng nhập', async () => {
        await loginPage.open();
      });

      await test.step('Act: Bấm Login khi form còn trống', async () => {
        await loginPage.submit();
      });

      await test.step('Assert: Không vào được hệ thống, vẫn ở trang Đăng nhập', async () => {
        await expect(page, 'URL phải vẫn là trang Đăng nhập').toHaveURL(new RegExp(`${routes.login}$`));
        await expect(loginPage.loginButton, 'Nút Login vẫn hiển thị nghĩa là chưa rời trang').toBeVisible();
      });
    },
  );
});
