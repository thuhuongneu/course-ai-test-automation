import { Page, Locator, Response, test } from '@playwright/test';
import { BasePage } from './base.page';
import { routes } from '../utils/env.config';

const REMEMBER_ME_COOKIE = 'autologin';
const THIRD_PARTY_LOGIN = /google|facebook|microsoft|sign in with|continue with/i;

/**
 * Trang đăng nhập Perfex CRM — /admin/authentication
 * Locator lấy từ DOM thật (label "Email Address" / "Password" / "Remember me", button submit "Login").
 */
export class LoginPage extends BasePage {
  readonly heading: Locator;
  readonly logoLink: Locator;
  readonly form: Locator;
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly rememberMeCheckbox: Locator;
  readonly rememberMeLabel: Locator;
  readonly loginButton: Locator;
  readonly forgotPasswordLink: Locator;
  /**
   * Mọi dải báo lỗi đỏ. Lỗi validate nằm thẳng trong <form>, lỗi sai thông tin nằm trong #alerts —
   * hệ thống không gắn role, chỉ chung class bootstrap alert-danger.
   */
  readonly errorAlert: Locator;
  readonly pageExpiredMessage: Locator;
  readonly buttons: Locator;
  readonly submitButtons: Locator;
  readonly thirdPartyLoginEntries: Locator;
  readonly captchaElements: Locator;
  readonly scripts: Locator;

  constructor(page: Page) {
    super(page);
    this.heading = page.getByRole('heading', { name: 'Login', exact: true, level: 1 });
    this.logoLink = page.getByRole('link', { name: 'Perfex CRM | Anh Tester Demo' });
    this.form = page.locator('form');
    this.emailInput = page.getByLabel('Email Address');
    this.passwordInput = page.getByLabel('Password', { exact: true });
    this.rememberMeCheckbox = page.getByRole('checkbox', { name: 'Remember me' });
    this.rememberMeLabel = page.getByText('Remember me', { exact: true });
    this.loginButton = page.getByRole('button', { name: 'Login' });
    this.forgotPasswordLink = page.getByRole('link', { name: 'Forgot Password?' });
    this.errorAlert = page.locator('.alert-danger');
    this.pageExpiredMessage = page.getByText('419 Page Expired!');
    this.buttons = page.getByRole('button');
    this.submitButtons = page.locator('button[type=submit], input[type=submit]');
    this.thirdPartyLoginEntries = page.getByRole('button', { name: THIRD_PARTY_LOGIN })
      .or(page.getByRole('link', { name: THIRD_PARTY_LOGIN }));
    this.captchaElements = page.locator('.g-recaptcha, [class*=captcha], iframe[src*=recaptcha]');
    this.scripts = page.locator('script');
  }

  async open(): Promise<Response | null> {
    return this.goto(routes.login);
  }

  /** Mở thẳng URL đăng xuất để kết thúc mọi phiên còn sót */
  async endSessionViaLogoutUrl(): Promise<void> {
    await test.step('Mở URL đăng xuất để kết thúc phiên còn sót', () => this.goto(routes.logout));
  }

  async fillCredentials(email: string, password: string): Promise<void> {
    await test.step(`Nhập email: ${email}`, () => this.fillField(this.emailInput, email));
    await this.fillPassword(password);
  }

  async fillPassword(password: string): Promise<void> {
    await test.step('Nhập mật khẩu', () => this.fillField(this.passwordInput, password));
  }

  async login(email: string, password: string): Promise<void> {
    await this.fillCredentials(email, password);
    await this.submit();
  }

  async submit(): Promise<void> {
    await test.step('Bấm nút Login', () => this.clickWhenReady(this.loginButton));
  }

  async submitTwiceQuickly(): Promise<void> {
    await test.step('Bấm nút Login hai lần liên tiếp', () => this.loginButton.dblclick());
  }

  /** Thao tác theo trạng thái tích — KHÔNG dựa vào value="estimate" của ô (AMB-LOGIN-11) */
  async checkRememberMe(): Promise<void> {
    await test.step('Tích ô Remember me', () => this.rememberMeCheckbox.check());
  }

  async clickRememberMeLabel(): Promise<void> {
    await test.step('Bấm vào chữ Remember me', () => this.rememberMeLabel.click());
  }

  async clickLogo(): Promise<void> {
    await test.step('Bấm logo phía trên khối biểu mẫu', () => this.clickWhenReady(this.logoLink));
  }

  /** Giá trị đã giải mã của cookie ghi nhớ, undefined nếu hệ thống không phát hành. KHÔNG log giá trị này */
  async rememberMeCookieValue(): Promise<string | undefined> {
    const cookie = (await this.page.context().cookies()).find(c => c.name === REMEMBER_ME_COOKIE);
    return cookie ? decodeURIComponent(cookie.value) : undefined;
  }
}
