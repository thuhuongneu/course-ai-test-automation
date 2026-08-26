# MSI Login Automation

Playwright + TypeScript automation framework on the MSI login flow
(`https://msi.dev.connectpos.com/login`).

## Structure

```
src/
  config/env.ts      # test credentials (overridable via .env)
  pages/LoginPage.ts # Page Object for the login screen
tests/
  login.spec.ts       # login test cases
playwright.config.ts  # baseURL, reporters, browser projects
```

## Setup

```bash
npm install
npx playwright install chromium
cp .env.example .env   # optional, defaults already match the dev env
```

## Run

```bash
npm test              # headless
npm run test:headed   # headed
npm run test:ui       # Playwright UI mode
npm run report        # open the last HTML report
```

## Test cases covered

- Successful login with valid credentials (`msicore` / `msicore123`), redirects to `/product/list`.
- Invalid password shows "Username or password is incorrect. Please try again!".
- Submitting with empty fields keeps the user on the login page.
