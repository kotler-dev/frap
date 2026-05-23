import { defineConfig, devices } from '@playwright/test';
import { flettaPlaywright } from '@fletta/playwright';

export default defineConfig({
  ...flettaPlaywright({
    minConfidence: 0.85,
    reportDir: './fletta-reports',
    enableHealing: true,
    enableReporting: true,
    playwrightConfig: {
      testDir: './',
      testIgnore: ['**/conference/**'],
      timeout: 10000,
      expect: {
        timeout: 5000,
      },
      fullyParallel: true,
      forbidOnly: !!process.env.CI,
      retries: process.env.CI ? 2 : 0,
      workers: process.env.CI ? 1 : undefined,
      reporter: [
        ['list'],
        ['json', { outputFile: 'results.json' }],
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
