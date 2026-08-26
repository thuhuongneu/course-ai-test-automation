import { expect, type Locator, type Page } from '@playwright/test';

export class LoginPage {
  readonly page: Page;
  readonly usernameInput: Locator;
  readonly passwordInput: Locator;
  readonly togglePasswordVisibilityButton: Locator;
  readonly rememberMeCheckbox: Locator;
  readonly loginButton: Locator;
  readonly errorMessage: Locator;

  constructor(page: Page) {
    this.page = page;
    this.usernameInput = page.locator('#user-name');
    this.passwordInput = page.locator('#password');
    this.togglePasswordVisibilityButton = page.getByRole('button', { name: 'toggle password visibility' });
    this.rememberMeCheckbox = page.getByRole('checkbox');
    this.loginButton = page.getByRole('button', { name: 'Login', exact: true });
    this.errorMessage = page.getByText('Username or password is incorrect. Please try again!');
  }

  async goto(): Promise<void> {
    await this.page.goto('/login', { waitUntil: 'domcontentloaded' });
    await this.usernameInput.waitFor({ state: 'visible' });
  }

  async fillUsername(username: string): Promise<void> {
    await this.usernameInput.fill(username);
  }

  async fillPassword(password: string): Promise<void> {
    await this.passwordInput.fill(password);
  }

  async submit(): Promise<void> {
    await this.loginButton.click();
  }

  async login(username: string, password: string): Promise<void> {
    await this.fillUsername(username);
    await this.fillPassword(password);
    await this.submit();
  }

  async expectLoggedIn(): Promise<void> {
    await this.page.waitForURL('**/product/list');
  }

  async expectLoginError(): Promise<void> {
    await expect(this.errorMessage).toBeVisible();
  }

  async expectStillOnLoginPage(): Promise<void> {
    await expect(this.page).toHaveURL(/\/login$/);
    await expect(this.usernameInput).toBeVisible();
  }
}
