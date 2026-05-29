import { defineConfig, devices } from '@playwright/test';
import { frapPlaywright } from '@frap/playwright';

const baseURL = process.env.TEST_SERVER_URL ?? 'http://localhost:3000';

export default defineConfig({
  ...frapPlaywright({
    minConfidence: 0.85,
    reportDir: './frap-reports',
    enableHealing: true,
    enableReporting: true,
    playwrightConfig: {
      testDir: './tests',
      timeout: 15_000,
      expect: { timeout: 5_000 },
      fullyParallel: false,
      forbidOnly: !!process.env.CI,
      retries: process.env.CI ? 1 : 0,
      reporter: [['list']],
      use: {
        baseURL,
        headless: true,
        channel: 'chrome',
      },
      projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
    },
  }),
});
