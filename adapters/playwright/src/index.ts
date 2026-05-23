import type { PlaywrightTestConfig } from '@playwright/test';
import { FlettaConfig } from '@fletta/sdk';
import { FlettaPlaywrightConfig, mergePlaywrightConfig } from './config';
import { createFlettaSelectorEngine, initFlettaEngine, recordSignature } from './selector-engine';
import { withFletta, getLastHealResult } from './wrapper';
import { FlettaReporter, generateJsonReport } from './reporter';

export { 
  withFletta, 
  getLastHealResult,
  createFlettaSelectorEngine,
  initFlettaEngine,
  recordSignature,
  FlettaReporter,
  generateJsonReport,
};
export type { FlettaPlaywrightConfig, WithFlettaOptions } from './config';
export type { FlettaHealingEvent, HealingEvent, FlettaReportSummary } from './reporter';
export {
  setCurrentPlaywrightTestId,
  getCurrentPlaywrightTestId,
  recordHealingEvent,
  consumeHealingEvents,
} from './healing-events';

export function flettaPlaywright(
  userConfig?: Partial<FlettaPlaywrightConfig>
): Partial<PlaywrightTestConfig> {
  const config = mergePlaywrightConfig(userConfig);
  
  const baseReporter = config.playwrightConfig?.reporter;
  const reporters: any[] = baseReporter
    ? (Array.isArray(baseReporter) ? baseReporter : [baseReporter])
    : [['list']];
  
  reporters.push(['@fletta/playwright/dist/reporter.js', { 
    minConfidence: config.minConfidence,
    reportDir: config.reportDir,
    enableHealing: config.enableHealing,
    enableReporting: config.enableReporting,
  }]);
  
  const playwrightConfig: Partial<PlaywrightTestConfig> = {
    ...config.playwrightConfig,
    reporter: reporters as any,
  };

  return playwrightConfig;
}

export async function registerFlettaSelector(
  selectors: any,
  config?: Partial<FlettaConfig>
): Promise<void> {
  const fullConfig: FlettaConfig = {
    minConfidence: 0.85,
    reportDir: './fletta-reports',
    enableHealing: true,
    enableReporting: true,
    ...config,
  };

  await initFlettaEngine(fullConfig);
  
  const engine = createFlettaSelectorEngine(fullConfig);
  await selectors.register('fletta', engine);
}

export const VERSION = '0.1.0';
