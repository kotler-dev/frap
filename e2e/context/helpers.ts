import type { WithFlettaOptions } from '@frap/playwright';

export const CONTEXT_REPORT_DIR = './frap-reports/context';

export const CONTEXT_PATH = {
  checkoutSlow: '/context/checkout.html?mode=slow',
  checkoutNormal: '/context/checkout.html?mode=normal',
  cartSlow: '/context/cart.html?delay=600',
  cartFast: '/context/cart.html?delay=100',
};

export function contextFletta(opts: Partial<WithFlettaOptions> = {}): WithFlettaOptions {
  return {
    reportDir: CONTEXT_REPORT_DIR,
    enableHealing: false,
    enableReporting: true,
    captureAll: true,
    ...opts,
  };
}
