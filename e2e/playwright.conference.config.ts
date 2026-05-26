import { defineConfig, devices } from '@playwright/test';
import { frapPlaywright } from 'frapcode-playwright';

export default defineConfig({
  ...frapPlaywright({
    minConfidence: 0.85,
    reportDir: './frap-reports/conference',
    enableHealing: true,
    enableReporting: true,
    playwrightConfig: {
      testDir: './conference',
      timeout: 10000,
      expect: { timeout: 5000 },
      fullyParallel: false,
      workers: 1,
      forbidOnly: !!process.env.CI,
      retries: process.env.CI ? 2 : 0,
      reporter: [
        ['list'],
        ['json', { outputFile: 'conference-results.json' }],
      ],
      use: {
        baseURL: 'http://localhost:3000',
        trace: 'on-first-retry',
        screenshot: 'only-on-failure',
        headless: true,
        channel: 'chrome',
        actionTimeout: 5000,
      },
      projects: [
        {
          name: 'chromium',
          use: { ...devices['Desktop Chrome'] },
        },
      ],
    },
  }),
});
