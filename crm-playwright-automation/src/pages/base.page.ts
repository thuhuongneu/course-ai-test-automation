import { Page, Locator, Response, expect } from '@playwright/test';
import { logger } from '../utils/logger';

/**
 * Lớp cha của mọi Page Object.
 * Chỉ chứa thao tác UI dùng chung — KHÔNG chứa assertion nghiệp vụ (assertion nằm ở test).
 * Toàn bộ chờ đợi dựa vào auto-waiting + web-first assertion của Playwright, không hard sleep.
 */
export abstract class BasePage {
  /** Thẻ body — hệ thống gắn class theo trang và theo người dùng (login_admin, dashboard, user-id-N) */
  readonly body: Locator;

  constructor(protected readonly page: Page) {
    this.body = page.locator('body');
  }

  async goto(path = ''): Promise<Response | null> {
    logger.info(`Điều hướng tới: ${path}`);
    const response = await this.page.goto(path);
    await this.page.waitForLoadState('domcontentloaded');
    return response;
  }

  async reload(): Promise<void> {
    await this.page.reload();
    await this.page.waitForLoadState('domcontentloaded');
  }

  /** Click sau khi element đã sẵn sàng nhận tương tác */
  protected async clickWhenReady(locator: Locator): Promise<void> {
    await expect(locator).toBeEnabled();
    await locator.click();
  }

  /** Điền text, xoá sạch giá trị cũ trước khi nhập */
  protected async fillField(locator: Locator, value: string): Promise<void> {
    await expect(locator).toBeVisible();
    await locator.fill(value);
  }
}
