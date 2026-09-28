import { test, expect } from '../../fixtures/base.fixture';
import { absoluteUrl, routes } from '../../utils/env.config';
import { allureMeta, Severity } from '../../utils/allure';

test.describe('Đăng nhập - Giao diện & cấu trúc trang', () => {
  test(
    'Mở trang đăng nhập khi chưa có phiên thì thấy biểu mẫu đăng nhập',
    { tag: ['@smoke', '@criticalpath', '@techcheck', '@login'] },
    async ({ loginPage, page }) => {
      await allureMeta({
        testId: 'CRM_LOGIN_TC_001',
        description:
          'Kiểm tra trình duyệt chưa có phiên mở /admin/authentication thì trang tải bình thường, dừng đúng '
          + 'địa chỉ đó, tab ghi "Perfex CRM | Anh Tester Demo - Login" và khối biểu mẫu có tiêu đề Login.',
        severity: Severity.BLOCKER,
      });

      await test.step('Arrange: Xác nhận trình duyệt sạch, chưa có phiên nào', async () => {
        const cookieNames = (await page.context().cookies()).map(c => c.name);
        expect(cookieNames, 'Trình duyệt phải sạch, chưa có cookie nào').toEqual([]);
      });

      const response = await test.step('Act: Mở trang đăng nhập', () => loginPage.open());

      await test.step('Assert: Trang tải xong bình thường, không hiện trang lỗi', async () => {
        expect(response?.status(), 'Request tài liệu phải trả HTTP 200').toBe(200);
        await expect(loginPage.form, 'Biểu mẫu đăng nhập phải hiển thị').toBeVisible();
      });

      await test.step('Assert: Thanh địa chỉ dừng đúng ở trang đăng nhập', async () => {
        await expect(page, 'Không được bị chuyển hướng đi nơi khác').toHaveURL(absoluteUrl(routes.login));
      });

      await test.step('Assert: Tab trình duyệt và tiêu đề biểu mẫu đúng', async () => {
        await expect(page, 'Tab trình duyệt phải ghi đúng tiêu đề').toHaveTitle('Perfex CRM | Anh Tester Demo - Login');
        await expect(loginPage.heading, 'Khối biểu mẫu phải có tiêu đề Login').toBeVisible();
      });

      await test.step('Assert: Ghi chú kỹ thuật — body mang class login_admin', async () => {
        await expect(loginPage.body, 'Thẻ body phải mang class login_admin').toContainClass('login_admin');
      });
    },
  );

  test(
    'Biểu mẫu đăng nhập đủ thành phần và đúng trạng thái mặc định',
    { tag: ['@smoke', '@criticalpath', '@accessibility', '@techcheck', '@login'] },
    async ({ loginPage }) => {
      await allureMeta({
        testId: 'CRM_LOGIN_TC_002',
        description:
          'Kiểm tra trang đăng nhập vừa nạp có đủ 5 thành phần, con trỏ nằm sẵn ở ô Email, ô Remember me rỗng '
          + 'và không bị khoá, nút Login bấm được, ô Password che ký tự và bấm vào chữ Remember me thì ô được tích.',
        severity: Severity.BLOCKER,
      });

      await test.step('Arrange: Mở trang đăng nhập, chưa nhập gì và chưa bấm gì', async () => {
        await loginPage.open();
      });

      await test.step('Assert: Mục 1 — Đủ 5 thành phần, Forgot Password? nằm dưới nút Login', async () => {
        await expect(loginPage.emailInput, 'Phải có ô nhập Email Address').toBeVisible();
        await expect(loginPage.passwordInput, 'Phải có ô nhập Password').toBeVisible();
        await expect(loginPage.rememberMeCheckbox, 'Phải có ô tích Remember me').toBeVisible();
        await expect(loginPage.loginButton, 'Phải có nút Login').toBeVisible();
        await expect(loginPage.forgotPasswordLink, 'Phải có liên kết Forgot Password?').toBeVisible();

        const loginBox = await loginPage.loginButton.boundingBox();
        const forgotBox = await loginPage.forgotPasswordLink.boundingBox();
        expect(forgotBox!.y, 'Liên kết Forgot Password? phải nằm dưới nút Login')
          .toBeGreaterThanOrEqual(loginBox!.y + loginBox!.height);
      });

      await test.step('Assert: Mục 2 — Con trỏ nằm sẵn trong ô Email, không cần bấm chuột', async () => {
        await expect(loginPage.emailInput, 'Ô Email phải được focus sẵn khi trang vừa nạp').toBeFocused();
      });

      await test.step('Assert: Mục 3 — Ô Remember me rỗng và không bị khoá', async () => {
        await expect(loginPage.rememberMeCheckbox, 'Ô Remember me phải đang rỗng').not.toBeChecked();
        await expect(loginPage.rememberMeCheckbox, 'Ô Remember me không được bị khoá').toBeEnabled();
      });

      await test.step('Assert: Mục 4 — Nút Login bấm được kể cả khi hai ô còn rỗng', async () => {
        await expect(loginPage.emailInput, 'Ô Email phải còn rỗng').toHaveValue('');
        await expect(loginPage.passwordInput, 'Ô Password phải còn rỗng').toHaveValue('');
        await expect(loginPage.loginButton, 'Nút Login phải bấm được').toBeEnabled();
      });

      await test.step('Act: Gõ Abc12345 vào ô Password', async () => {
        await loginPage.fillPassword('Abc12345');
      });

      await test.step('Assert: Mục 5 — Ô Password che ký tự, không hiện chữ', async () => {
        await expect(loginPage.passwordInput, 'Ô Password phải che ký tự').toHaveAttribute('type', 'password');
        await expect(loginPage.passwordInput, 'Ô Password phải nhận đủ 8 ký tự đã gõ').toHaveValue('Abc12345');
      });

      await test.step('Act: Bấm vào chữ Remember me (không bấm ô vuông)', async () => {
        await loginPage.clickRememberMeLabel();
      });

      await test.step('Assert: Mục 6 — Ô Remember me chuyển sang tích', async () => {
        await expect(loginPage.rememberMeCheckbox, 'Nhãn Remember me phải ăn khớp với ô tích').toBeChecked();
      });

      await test.step('Assert: Ghi chú kỹ thuật — form, autofocus và nhãn khớp id', async () => {
        await expect(loginPage.form, 'Form phải gửi tới /admin/authentication')
          .toHaveAttribute('action', absoluteUrl(routes.login));
        await expect(loginPage.form, 'Form phải dùng method post').toHaveAttribute('method', 'post');
        await expect(loginPage.emailInput, 'Ô Email phải mang autofocus="1"').toHaveAttribute('autofocus', '1');
        await expect(loginPage.emailInput, 'Nhãn Email Address phải trỏ tới id email').toHaveId('email');
        await expect(loginPage.passwordInput, 'Nhãn Password phải trỏ tới id password').toHaveId('password');
        await expect(loginPage.rememberMeCheckbox, 'Nhãn Remember me phải trỏ tới id remember').toHaveId('remember');
      });
    },
  );

  test(
    'Trang đăng nhập không có CAPTCHA, không đăng nhập bên thứ ba, không nạp JavaScript',
    { tag: ['@regression', '@security', '@techcheck', '@login'] },
    async ({ loginPage, page }) => {
      await allureMeta({
        testId: 'CRM_LOGIN_TC_003',
        description:
          'Kiểm tra trang đăng nhập không có khối CAPTCHA, chỉ có một nút gửi Login, không có nút đăng nhập bên thứ ba '
          + 'và không nạp bất kỳ kịch bản JavaScript nào.',
        severity: Severity.NORMAL,
      });

      const scriptRequests: string[] = [];

      await test.step('Arrange: Mở trang đăng nhập và nạp lại một lần', async () => {
        page.on('request', request => {
          if (request.resourceType() === 'script') {
            scriptRequests.push(request.url());
          }
        });
        await loginPage.open();
        await loginPage.reload();
      });

      await test.step('Assert: Mục 1 — Không có khối CAPTCHA nào', async () => {
        await expect(loginPage.captchaElements, 'Không được có phần tử CAPTCHA nào').toHaveCount(0);
      });

      await test.step('Assert: Mục 2 — Chỉ một nút gửi Login, không có đăng nhập bên thứ ba', async () => {
        await expect(loginPage.form, 'Toàn trang phải có đúng 1 biểu mẫu').toHaveCount(1);
        await expect(loginPage.submitButtons, 'Toàn trang phải có đúng 1 nút gửi').toHaveCount(1);
        await expect(loginPage.buttons, 'Nút duy nhất trên trang phải là Login').toHaveCount(1);
        await expect(loginPage.loginButton, 'Nút gửi phải là Login').toBeVisible();
        await expect(loginPage.thirdPartyLoginEntries, 'Không được có nút đăng nhập bên thứ ba').toHaveCount(0);
      });

      await test.step('Assert: Mục 3 — Trang dùng được ngay, không nạp kịch bản nào', async () => {
        await expect(loginPage.emailInput, 'Ô Email phải nhập được ngay').toBeEditable();
        await expect(loginPage.loginButton, 'Nút Login phải bấm được ngay').toBeEnabled();
        await expect(loginPage.scripts, 'Trang không được có thẻ <script> nào').toHaveCount(0);
        expect(scriptRequests, 'Không được có request JavaScript nào').toEqual([]);
      });
    },
  );

  test(
    'Bấm logo trên trang đăng nhập thì về trang chủ công khai',
    {
      tag: ['@regression', '@login'],
      annotation: {
        type: 'issue',
        description: 'Khảo sát 28-09-2026: "/" chuyển tiếp sang /authentication/login. Bug TC004 đang chờ PO chốt '
          + '(docs/testcases/login/review/automation_review_web_20260919.md) — giữ nguyên kỳ vọng của TC.',
      },
    },
    async ({ loginPage, page }) => {
      await allureMeta({
        testId: 'CRM_LOGIN_TC_004',
        description:
          'Kiểm tra bấm logo phía trên khối biểu mẫu đăng nhập thì rời khu quản trị và dừng ở trang chủ công khai '
          + 'https://crm.anhtester.com/.',
        severity: Severity.MINOR,
      });

      await test.step('Arrange: Mở trang đăng nhập', async () => {
        await loginPage.open();
      });

      await test.step('Act: Bấm logo phía trên khối biểu mẫu', async () => {
        await loginPage.clickLogo();
      });

      await test.step('Assert: Trang rời khỏi khu /admin', async () => {
        await expect(page, 'Sau khi bấm logo không được còn ở khu /admin').not.toHaveURL(/\/admin(\/|$)/);
      });

      await test.step('Assert: Thanh địa chỉ dừng ở trang chủ công khai', async () => {
        await expect(page, 'Thanh địa chỉ phải dừng ở trang chủ công khai').toHaveURL(absoluteUrl(routes.home));
      });
    },
  );
});
