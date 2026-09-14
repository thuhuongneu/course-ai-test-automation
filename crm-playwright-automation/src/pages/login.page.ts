import { Page, Locator, test } from '@playwright/test';
import { BasePage } from './base.page';
import { routes } from '../utils/env.config';

/**
 * Trang đăng nhập Perfex CRM — /admin/authentication
 * Locator lấy từ DOM thật (label "Email Address" / "Password", button submit "Login").
 */
export class LoginPage extends BasePage {
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly loginButton: Locator;
  /** Khối thông báo lỗi #alerts — hệ thống không gắn role, dùng id container + class bootstrap */
  readonly errorAlert: Locator;

  constructor(page: Page) {
    super(page);
    this.emailInput = page.getByLabel('Email Address');
    this.passwordInput = page.getByLabel('Password', { exact: true });
    this.loginButton = page.getByRole('button', { name: 'Login' });
    this.errorAlert = page.locator('#alerts .alert-danger');
  }

  async open(): Promise<void> {
    await this.goto(routes.login);
  }

  async login(email: string, password: string): Promise<void> {
    await test.step(`Nhập email: ${email}`, () => this.fillField(this.emailInput, email));
    await test.step('Nhập mật khẩu', () => this.fillField(this.passwordInput, password));
    await this.submit();
  }

  async submit(): Promise<void> {
    await test.step('Bấm nút Login', () => this.clickWhenReady(this.loginButton));
  }
}
