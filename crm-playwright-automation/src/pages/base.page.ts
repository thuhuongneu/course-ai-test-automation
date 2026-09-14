import { Page, Locator, expect } from '@playwright/test';
import { logger } from '../utils/logger';

/**
 * Lớp cha của mọi Page Object.
 * Chỉ chứa thao tác UI dùng chung — KHÔNG chứa assertion nghiệp vụ (assertion nằm ở test).
 * Toàn bộ chờ đợi dựa vào auto-waiting + web-first assertion của Playwright, không hard sleep.
 */
export abstract class BasePage {
  constructor(protected readonly page: Page) {}

  async goto(path = ''): Promise<void> {
    logger.info(`Điều hướng tới: ${path}`);
    await this.page.goto(path);
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
