import { defineConfig } from 'allure';

export default defineConfig({
  name: 'CRM Automation Report',
  output: 'reports/allure-report',
  plugins: {
    awesome: {
      options: {
        singleFile: false,
        reportLanguage: 'en',
      },
    },
  },
});
