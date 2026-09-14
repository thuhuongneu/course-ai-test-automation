import { defineConfig } from 'allure';

/** Report gộp 1 file HTML — dùng để gửi qua chat/email cho máy không cài gì. */
export default defineConfig({
  name: 'CRM Automation Report',
  output: 'reports/allure-report-single',
  plugins: {
    awesome: {
      options: {
        singleFile: true,
        reportLanguage: 'en',
      },
    },
  },
});
