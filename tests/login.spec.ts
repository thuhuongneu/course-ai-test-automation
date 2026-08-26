import { test, expect } from '@playwright/test';
import { LoginPage } from '../src/pages/LoginPage';
import { credentials } from '../src/config/env';

test.describe('MSI Login', () => {
  let loginPage: LoginPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    await loginPage.goto();
  });

  test('logs in successfully with valid credentials', async ({ page }) => {
    await loginPage.login(credentials.validUsername, credentials.validPassword);

    await loginPage.expectLoggedIn();
    expect(page.url()).toContain('/product/list');
  });

  test('shows an error message with an invalid password', async () => {
    await loginPage.login(credentials.validUsername, 'wrong-password');

    await loginPage.expectLoginError();
  });

  test('stays on the login page when submitted with empty credentials', async () => {
    await loginPage.submit();

    await loginPage.expectStillOnLoginPage();
  });
});
