import { test, expect } from '../../fixtures/base.fixture';
import { routes } from '../../utils/env.config';
import { allureMeta, Severity } from '../../utils/allure';

test.describe('Dashboard', () => {
  test(
    'Điều hướng từ Dashboard sang trang Khách hàng',
    { tag: ['@smoke', '@dashboard'] },
    async ({ authenticatedDashboard, page }) => {
      await allureMeta({
        testId: 'CRM_DASH_TC_001',
        description:
          'Kiểm tra người dùng đã đăng nhập bấm menu Customers trên sidebar thì hệ thống '
          + 'chuyển sang trang danh sách Khách hàng.',
        severity: Severity.CRITICAL,
      });

      await test.step('Arrange: Xác nhận đang ở Dashboard', async () => {
        await expect(authenticatedDashboard.sidebarMenu, 'Sidebar phải hiển thị').toBeVisible();
      });

      await test.step('Act: Bấm menu Customers trên sidebar', async () => {
        await authenticatedDashboard.openMenu('Customers');
      });

      await test.step('Assert: Hệ thống chuyển sang trang Khách hàng', async () => {
        await expect(page, 'URL phải chuyển sang /admin/clients').toHaveURL(routes.customers);
        await expect(authenticatedDashboard.sidebarMenu, 'Sidebar vẫn hiển thị sau khi điều hướng').toBeVisible();
      });
    },
  );
});
