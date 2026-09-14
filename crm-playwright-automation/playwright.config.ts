import { defineConfig, devices } from '@playwright/test';
import { env } from './src/utils/env.config';

export default defineConfig({
  testDir: './src/tests',
  timeout: env.timeout,
  expect: { timeout: 15_000 },

  // Parallel LUÔN bật — đổi số luồng bằng biến WORKERS trong .env, KHÔNG sửa file này
  fullyParallel: true,
  workers: env.workers,
  retries: process.env.CI ? 2 : 0,
  forbidOnly: !!process.env.CI,

  // Mọi output gom vào reports/ — root project luôn sạch
  outputDir: 'reports/test-artifacts',
  reporter: [
    ['list'],
    ['html', { outputFolder: 'reports/html', open: 'never' }],
    // detail: false -> report chỉ hiện step Arrange/Act/Assert do mình đặt tên,
    // bỏ step nội bộ của Playwright (Fill/Click/Expect) vốn in cả giá trị đã nhập (lộ mật khẩu)
    ['allure-playwright', { resultsDir: 'reports/allure-results', detail: false }],
  ],

  use: {
    baseURL: env.baseURL,
    headless: env.headless,
    // 'off' vì ảnh được attach thủ công ở fixture cho CẢ pass lẫn fail — tránh đính 2 lần
    screenshot: 'off',
    video: 'retain-on-failure',
    trace: 'retain-on-failure',
    actionTimeout: 20_000,
    navigationTimeout: 45_000,
    ignoreHTTPSErrors: true,
  },

  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        // PHẢI khai sau spread: preset Desktop Chrome đã có sẵn viewport 1280x720,
        // đặt ở `use` cấp config sẽ bị preset của project ghi đè mà không báo gì
        viewport: { width: 1920, height: 1080 },
      },
    },
  ],
});
