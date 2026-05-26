import type { WithFlettaOptions } from '@fletta/playwright';

/** Fletta artifacts for the Conference Playwright project */
export const CONF_REPORT_DIR = './fletta-reports/conference';

export const CONF_PATH = {
  index: '/conference/index.html',
  scheduleV1: '/conference/schedule-v1.html',
  scheduleV2: '/conference/schedule-v2.html',
  scheduleHeal: '/conference/schedule-heal.html',
  register: '/conference/register.html',
  cfp: '/conference/cfp.html',
  speakers: '/conference/speakers.html',
  speaker: (id: string) => `/conference/speaker.html?id=${id}`,
  talk: (id: string) => `/conference/talk.html?id=${id}`,
};

export function confFletta(opts: Partial<WithFlettaOptions> = {}): WithFlettaOptions {
  return {
    reportDir: CONF_REPORT_DIR,
    enableHealing: true,
    enableReporting: true,
    minConfidence: 0.85,
    ...opts,
  };
}
