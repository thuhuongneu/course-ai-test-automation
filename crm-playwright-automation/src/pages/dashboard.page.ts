import { Page, Locator, test } from '@playwright/test';
import { BasePage } from './base.page';
import { routes } from '../utils/env.config';

/**
 * Trang Dashboard sau khi đăng nhập — /admin/
 * Locator lấy từ DOM thật: sidebar #side-menu, mỗi mục là #side-menu > li > a.
 */
export class DashboardPage extends BasePage {
  readonly sidebarMenu: Locator;
  /** Các mục menu cấp 1 — mỗi mục có class menu-item-<tên>; li đầu tiên là logo, không mang class này */
  readonly menuItems: Locator;
  readonly profileAvatar: Locator;

  constructor(page: Page) {
    super(page);
    this.sidebarMenu = page.locator('#side-menu');
    this.menuItems = this.sidebarMenu.locator('> li[class*="menu-item-"]');
    this.profileAvatar = page.locator('#header a.profile img.staff-profile-image-small');
  }

  /**
   * Mục menu cấp 1 trên sidebar, VD: 'Dashboard', 'Customers', 'Projects'.
   * Không dùng getByRole vì icon Font Awesome sinh glyph qua ::before, glyph đó cộng vào
   * accessible name nên không so khớp được theo tên. Giới hạn ở con trực tiếp (> li > a)
   * để mục cấp 1 không đụng mục con cùng chữ khi menu con nở ra.
   */
  menuItem(name: string): Locator {
    return this.sidebarMenu.locator('> li > a').filter({ hasText: name });
  }

  /** Cổng đồng bộ cho fixture đăng nhập sẵn — chờ, không assert nghiệp vụ */
  async waitForLoaded(): Promise<void> {
    await test.step('Chờ Dashboard hiển thị', async () => {
      await this.page.waitForURL(routes.dashboard);
      await this.sidebarMenu.waitFor({ state: 'visible' });
    });
  }

  async openMenu(name: string): Promise<void> {
    await test.step(`Mở menu ${name}`, () => this.clickWhenReady(this.menuItem(name)));
  }
}
