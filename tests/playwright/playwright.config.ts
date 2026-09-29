import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './',
  timeout: 30 * 1000,
  expect: {
    timeout: 5000
  },
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [['html'], ['list']],
  use: {
    baseURL: process.env.APP_URL || 'https://ais-dev-ysij5gfggpt2akxlk7vm3b-58584043488.asia-east1.run.app',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'Mobile Chrome (OnePlus & Pixel emulation)',
      use: { ...devices['Pixel 5'], viewport: { width: 393, height: 851 } },
    },
    {
      name: 'Mobile Safari',
      use: { ...devices['iPhone 14'] },
    },
    {
      name: 'Desktop Web Browser',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
