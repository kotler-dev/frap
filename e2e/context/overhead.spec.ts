import { test, expect } from '@playwright/test';
import { attachFlettaContext } from '@frap/playwright';
import { CONTEXT_PATH, CONTEXT_REPORT_DIR } from './helpers';

const WARMUP = 1;
const ITERATIONS = 3;
const OVERHEAD_LIMIT = 0.2;

async function measureCartLoad(
  browser: import('@playwright/test').Browser,
  withCapture: boolean
): Promise<number> {
  const times: number[] = [];
  const totalRuns = WARMUP + ITERATIONS;
  for (let i = 0; i < totalRuns; i++) {
    const page = await browser.newPage();
    if (withCapture) {
      attachFlettaContext(page, {
        reportDir: CONTEXT_REPORT_DIR,
        testId: `overhead-${withCapture ? 'capture' : 'base'}-${i}`,
      });
    }
    const start = Date.now();
    await page.goto(CONTEXT_PATH.cartFast);
    await page.getByTestId('cart-ready').waitFor();
    if (i >= WARMUP) {
      times.push(Date.now() - start);
    }
    await page.close();
  }
  return times.reduce((sum, t) => sum + t, 0) / times.length;
}

test.describe('F002 capture overhead', () => {
  test('captureAll overhead stays below 20%', async ({ browser }) => {
    const baselineAvg = await measureCartLoad(browser, false);
    const captureAvg = await measureCartLoad(browser, true);
    const overhead = (captureAvg - baselineAvg) / baselineAvg;

    console.log(
      `[overhead] baseline=${baselineAvg.toFixed(0)}ms capture=${captureAvg.toFixed(0)}ms overhead=${(overhead * 100).toFixed(1)}%`
    );

    expect(overhead).toBeLessThan(OVERHEAD_LIMIT);
  });
});
