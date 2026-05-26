import { defineConfig, devices } from '@playwright/test';
import { frapPlaywright } from '@frap/playwright';
import { CONTEXT_REPORT_DIR } from './context/helpers';

export default defineConfig({
  ...frapPlaywright({
    minConfidence: 0.85,
    reportDir: CONTEXT_REPORT_DIR,
    enableHealing: false,
    enableReporting: true,
    captureAll: true,
    playwrightConfig: {
      testDir: './context',
      testIgnore: process.env.FLETTA_BENCH_OVERHEAD ? [] : ['**/overhead.spec.ts'],
      timeout: 30_000,
      expect: { timeout: 5000 },
      fullyParallel: false,
      forbidOnly: !!process.env.CI,
      retries: 0,
      workers: 1,
      reporter: [['list']],
      use: {
        baseURL: 'http://localhost:3000',
        trace: 'on-first-retry',
        screenshot: 'only-on-failure',
        headless: true,
        channel: 'chrome',
      },
      projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
    },
  }),
});
