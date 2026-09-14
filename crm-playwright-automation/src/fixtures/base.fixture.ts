import { test as base } from '@playwright/test';
import { LoginPage } from '../pages/login.page';
import { DashboardPage } from '../pages/dashboard.page';
import { env } from '../utils/env.config';

type Pages = {
  loginPage: LoginPage;
  dashboardPage: DashboardPage;
};

type AutoFixtures = {
  /** Fixture tự chạy: đính ảnh trạng thái cuối cho MỌI test — cả PASS lẫn FAIL */
  attachFinalScreenshot: void;
};

type LoggedInFixtures = {
  /** Trang đã đăng nhập sẵn — dùng cho test không quan tâm bước login */
  authenticatedDashboard: DashboardPage;
};

export const test = base.extend<Pages & AutoFixtures & LoggedInFixtures>({
  loginPage: async ({ page }, use) => {
    await use(new LoginPage(page));
  },

  dashboardPage: async ({ page }, use) => {
    await use(new DashboardPage(page));
  },

  authenticatedDashboard: async ({ loginPage, dashboardPage }, use) => {
    await loginPage.open();
    await loginPage.login(env.username, env.password);
    await dashboardPage.waitForLoaded();
    await use(dashboardPage);
  },

  attachFinalScreenshot: [
    async ({ page }, use, testInfo) => {
      await use();

      if (page.isClosed()) {
        return;
      }
      const name = testInfo.status === 'passed'
        ? 'trang_thai_cuoi_cua_test'
        : 'trang_thai_khi_that_bai';

      await testInfo.attach(name, {
        body: await page.screenshot({ fullPage: true }),
        contentType: 'image/png',
      });
    },
    { auto: true },
  ],
});

export { expect } from '@playwright/test';
