import type { WithFrapOptions } from '@frap/playwright';

export const REPORT_DIR = './frap-reports';

export const DEMO_PATH = {
  scheduleHeal: '/conference/schedule-heal.html',
};

export function demoFrap(opts: Partial<WithFrapOptions> = {}): WithFrapOptions {
  return {
    reportDir: REPORT_DIR,
    enableHealing: true,
    enableReporting: true,
    minConfidence: 0.85,
    ...opts,
  };
}
