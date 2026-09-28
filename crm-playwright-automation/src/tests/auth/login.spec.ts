import { test, expect } from '../../fixtures/base.fixture';
import { absoluteUrl, env, routes } from '../../utils/env.config';
import { allureMeta, Severity } from '../../utils/allure';
import { TestData } from '../../utils/test-data';

const DASHBOARD_URL = absoluteUrl(routes.dashboardPath);
const PM_HIDDEN_MENUS = ['Subscriptions', 'Expenses', 'Estimate Request', 'Knowledge Base', 'Reports'];
/** a:2:{s:7:"user_id";s:1:"<id>";s:3:"key";s:16:"<16 hex>";} — chỉ so hình thái, KHÔNG in giá trị */
const REMEMBER_ME_COOKIE_SHAPE = /^a:2:\{s:7:"user_id";s:\d+:"\d+";s:3:"key";s:16:"[0-9a-f]{16}";\}$/;

const EMAIL_VARIANTS = [
  { key: 'a', name: 'lẫn hoa thường', email: TestData.mixedCaseEmail(env.username) },
  { key: 'b', name: 'toàn chữ HOA', email: env.username.toUpperCase() },
  { key: 'c', name: '2 dấu cách mỗi đầu', email: `  ${env.username}  ` },
  { key: 'd', name: '1 dấu cách cuối', email: `${env.username} ` },
];

test.describe('Đăng nhập thành công & Ghi nhớ đăng nhập', () => {
  test(
    'Đăng nhập bằng tài khoản Admin hợp lệ thì vào được Dashboard',
    { tag: ['@smoke', '@criticalpath', '@techcheck', '@login'] },
    async ({ loginPage, dashboardPage, page }) => {
      await allureMeta({
        testId: 'CRM_LOGIN_TC_005',
        description:
          'Kiểm tra đăng nhập bằng tài khoản Admin hợp lệ, không tích Remember me, thì hệ thống chuyển vào Dashboard '
          + 'tại /admin/, không báo lỗi, hiển thị menu điều hướng bên trái và ảnh đại diện góc trên bên phải.',
        severity: Severity.BLOCKER,
      });

      await test.step('Arrange: Mở trang đăng nhập, ô Remember me để trống', async () => {
        await loginPage.open();
        await expect(loginPage.rememberMeCheckbox, 'Ô Remember me phải để trống').not.toBeChecked();
      });

      const loginResponse = await test.step('Act: Đăng nhập bằng tài khoản Admin', async () => {
        const responsePromise = page.waitForResponse(
          r => r.request().method() === 'POST' && r.url() === absoluteUrl(routes.login),
        );
        await loginPage.login(env.username, env.password);
        return responsePromise;
      });

      await test.step('Assert: Hệ thống chuyển vào Dashboard, không báo lỗi', async () => {
        await expect(page, 'Phải dừng ở /admin/, không quay lại trang đăng nhập').toHaveURL(DASHBOARD_URL);
        await expect(page, 'Tab trình duyệt phải có chữ Dashboard').toHaveTitle(/Dashboard/);
        await expect(loginPage.errorAlert, 'Không được có dải báo lỗi đỏ nào').toHaveCount(0);
      });

      await test.step('Assert: Trang hiện menu điều hướng bên trái và ảnh đại diện', async () => {
        await expect(dashboardPage.sidebarMenu, 'Menu điều hướng bên trái phải hiển thị').toBeVisible();
        await expect(dashboardPage.profileAvatar, 'Ảnh đại diện góc trên bên phải phải hiển thị').toBeVisible();
      });

      await test.step('Assert: Ghi chú kỹ thuật — POST trả 303 về /admin/, body mang đủ class', async () => {
        expect(loginResponse.status(), 'POST /admin/authentication phải trả 303').toBe(303);
        expect(loginResponse.headers()['location'], 'Header location phải trỏ về /admin/').toBe(DASHBOARD_URL);
        await expect(dashboardPage.body, 'Body phải mang class app, admin, dashboard, user-id-2')
          .toContainClass('app admin dashboard user-id-2');
      });
    },
  );

  test(
    'Đăng nhập bằng tài khoản Project Manager thì vào Dashboard với bộ menu rút gọn',
    { tag: ['@regression', '@permission', '@techcheck', '@login'] },
    async ({ loginPage, dashboardPage, page }) => {
      await allureMeta({
        testId: 'CRM_LOGIN_TC_006',
        description:
          'Kiểm tra đăng nhập bằng tài khoản Project Manager thì vào Dashboard, menu trái chỉ có 9 mục và không có '
          + 'Subscriptions, Expenses, Estimate Request, Knowledge Base, Reports.',
        severity: Severity.CRITICAL,
      });

      await test.step('Arrange: Mở trang đăng nhập', async () => {
        await loginPage.open();
      });

      await test.step('Act: Đăng nhập bằng tài khoản Project Manager', async () => {
        await loginPage.login(env.pmEmail, env.pmPassword);
      });

      await test.step('Assert: Đăng nhập thành công, dừng ở Dashboard, không báo lỗi', async () => {
        await expect(page, 'Phải dừng ở /admin/').toHaveURL(DASHBOARD_URL);
        await expect(loginPage.errorAlert, 'Không được có dải báo lỗi đỏ nào').toHaveCount(0);
      });

      await test.step('Assert: Menu trái có đúng 9 mục', async () => {
        await expect(dashboardPage.menuItems, 'Project Manager phải thấy đúng 9 mục menu').toHaveCount(9);
      });

      await test.step(`Assert: Không có mục ${PM_HIDDEN_MENUS.join(', ')}`, async () => {
        for (const menu of PM_HIDDEN_MENUS) {
          await expect(dashboardPage.menuItem(menu), `Project Manager không được thấy mục ${menu}`).toHaveCount(0);
        }
      });

      await test.step('Assert: Ghi chú kỹ thuật — body mang class user-id-3', async () => {
        await expect(dashboardPage.body, 'Body phải mang class user-id-3').toContainClass('user-id-3');
      });
    },
  );

  for (const variant of EMAIL_VARIANTS) {
    test(
      `Email được chuẩn hoá — khác kiểu chữ và thừa khoảng trắng vẫn đăng nhập được (biến thể ${variant.key}: ${variant.name})`,
      { tag: ['@regression', '@boundary', '@login'] },
      async ({ loginPage, page }) => {
        await allureMeta({
          testId: 'CRM_LOGIN_TC_007',
          description:
            `Kiểm tra email Admin nhập theo biến thể ${variant.key} (${variant.name}) vẫn được hệ thống chuẩn hoá: `
            + 'không báo "Invalid email or password" và vào được Dashboard.',
          severity: Severity.CRITICAL,
        });

        await test.step('Arrange: Mở trang đăng nhập', async () => {
          await loginPage.open();
        });

        await test.step(`Act: Đăng nhập bằng email biến thể ${variant.key} và mật khẩu Admin`, async () => {
          await loginPage.login(variant.email, env.password);
        });

        await test.step('Assert: Không báo lỗi, dừng ở Dashboard', async () => {
          await expect(page, 'Phải dừng ở /admin/ — email phải được chuẩn hoá').toHaveURL(DASHBOARD_URL);
          await expect(page, 'Tab trình duyệt phải có chữ Dashboard').toHaveTitle(/Dashboard/);
          await expect(loginPage.errorAlert, 'Không được hiện dải "Invalid email or password"').toHaveCount(0);
        });
      },
    );
  }

  test(
    'Tích Ghi nhớ đăng nhập thì hệ thống phát hành cookie ghi nhớ',
    { tag: ['@regression', '@security', '@techcheck', '@login'] },
    async ({ loginPage, page }) => {
      await allureMeta({
        testId: 'CRM_LOGIN_TC_008',
        description:
          'Kiểm tra đăng nhập Admin có tích Remember me từ trình duyệt sạch cookie thì đăng nhập thành công và hệ thống '
          + 'phát hành cookie autologin đúng hình thái (key dài đúng 16 ký tự hex).',
        severity: Severity.CRITICAL,
      });

      await test.step('Arrange: Xác nhận trình duyệt sạch cookie, mở trang đăng nhập', async () => {
        const cookieNames = (await page.context().cookies()).map(c => c.name);
        expect(cookieNames, 'Trước bước 1 danh sách cookie phải rỗng').toEqual([]);
        await loginPage.open();
      });

      await test.step('Act: Nhập tài khoản Admin và tích ô Remember me', async () => {
        await loginPage.fillCredentials(env.username, env.password);
        await loginPage.checkRememberMe();
      });

      await test.step('Assert: Ô Remember me đã được tích', async () => {
        await expect(loginPage.rememberMeCheckbox, 'Ô Remember me phải chuyển sang tích').toBeChecked();
      });

      await test.step('Act: Bấm nút Login', async () => {
        await loginPage.submit();
      });

      await test.step('Assert: Đăng nhập thành công, dừng ở Dashboard, không báo lỗi', async () => {
        await expect(page, 'Phải dừng ở /admin/').toHaveURL(DASHBOARD_URL);
        await expect(loginPage.errorAlert, 'Không được có dải báo lỗi nào').toHaveCount(0);
      });

      await test.step('Assert: Ghi chú kỹ thuật — có cookie autologin đúng hình thái', async () => {
        const cookieValue = await loginPage.rememberMeCookieValue();
        expect(cookieValue !== undefined, 'Phải tồn tại cookie autologin sau khi đăng nhập có tích Remember me')
          .toBe(true);
        expect(
          REMEMBER_ME_COOKIE_SHAPE.test(cookieValue ?? ''),
          'Cookie autologin phải khớp hình thái a:2:{user_id; key} với key đúng 16 ký tự hex',
        ).toBe(true);
      });
    },
  );

  test(
    'Không tích Ghi nhớ đăng nhập thì hoàn toàn không phát hành cookie ghi nhớ',
    { tag: ['@regression', '@security', '@techcheck', '@login'] },
    async ({ loginPage, page }) => {
      await allureMeta({
        testId: 'CRM_LOGIN_TC_009',
        description:
          'Kiểm tra sau khi xoá cookie và kết thúc phiên cũ, đăng nhập Admin không tích Remember me thì đăng nhập '
          + 'thành công nhưng hệ thống không phát hành cookie autologin.',
        severity: Severity.CRITICAL,
      });

      await test.step('Arrange: Xoá toàn bộ cookie và mở URL đăng xuất để kết thúc phiên còn sót', async () => {
        await page.context().clearCookies();
        await loginPage.endSessionViaLogoutUrl();
      });

      await test.step('Arrange: Mở trang đăng nhập, ô Remember me rỗng, chưa có cookie ghi nhớ', async () => {
        await loginPage.open();
        await expect(loginPage.rememberMeCheckbox, 'Ô Remember me phải đang rỗng').not.toBeChecked();
        await expect(loginPage.rememberMeCheckbox, 'Ô Remember me không được bị khoá').toBeEnabled();
        expect(await loginPage.rememberMeCookieValue() === undefined, 'Trước khi đăng nhập chưa được có cookie autologin')
          .toBe(true);
      });

      await test.step('Act: Đăng nhập bằng tài khoản Admin, không động vào ô Remember me', async () => {
        await loginPage.login(env.username, env.password);
      });

      await test.step('Assert: Đăng nhập thành công, dừng ở Dashboard', async () => {
        await expect(page, 'Phải dừng ở /admin/').toHaveURL(DASHBOARD_URL);
      });

      await test.step('Assert: Ghi chú kỹ thuật — không tồn tại cookie autologin', async () => {
        expect(await loginPage.rememberMeCookieValue() === undefined, 'Không tích Remember me thì không được có cookie autologin')
          .toBe(true);
      });
    },
  );

  test(
    'Bấm nút Login hai lần liên tiếp vẫn vào đúng Dashboard, không sinh trang lỗi',
    { tag: ['@regression', '@racecondition', '@login'] },
    async ({ loginPage, dashboardPage, page }) => {
      await allureMeta({
        testId: 'CRM_LOGIN_TC_010',
        description:
          'Kiểm tra bấm nút Login hai lần liên tiếp trong chưa tới 1 giây vẫn vào đúng Dashboard, không treo, '
          + 'không có dải báo lỗi và không xuất hiện trang 419 Page Expired!.',
        severity: Severity.NORMAL,
      });

      await test.step('Arrange: Mở trang đăng nhập', async () => {
        await loginPage.open();
      });

      await test.step('Act: Nhập tài khoản Admin rồi bấm Login hai lần liên tiếp', async () => {
        await loginPage.fillCredentials(env.username, env.password);
        await loginPage.submitTwiceQuickly();
      });

      await test.step('Assert: Trang dừng chuyển hướng ở Dashboard, không báo lỗi', async () => {
        await expect(page, 'Phải dừng ở /admin/').toHaveURL(DASHBOARD_URL);
        await expect(page, 'Tab trình duyệt phải có chữ Dashboard').toHaveTitle(/Dashboard/);
        await expect(dashboardPage.sidebarMenu, 'Dashboard phải tải xong, không treo').toBeVisible();
        await expect(loginPage.errorAlert, 'Không được có dải báo lỗi đỏ nào').toHaveCount(0);
      });

      await test.step('Assert: Không xuất hiện dòng 419 Page Expired!', async () => {
        await expect(loginPage.pageExpiredMessage, 'Không được sinh trang 419 Page Expired!').toHaveCount(0);
      });
    },
  );
});
