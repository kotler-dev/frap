import type { PlaywrightTestConfig } from '@playwright/test';
import { FrapConfig } from '@frap/sdk';
import { FrapPlaywrightConfig, mergePlaywrightConfig } from './config';
import { createFrapSelectorEngine, initFrapEngine, recordSignature } from './selector-engine';
import { withFrap, getLastHealResult } from './wrapper';
import { FrapReporter, generateJsonReport } from './reporter';
import {
  attachFrapContext,
  recordContextUiEvent,
  writeContextReport,
  getContextTimeline,
  clearContextBuffers,
  loadAllContextEvents,
} from './context';

export {
  withFrap,
  getLastHealResult,
  createFrapSelectorEngine,
  initFrapEngine,
  recordSignature,
  FrapReporter,
  generateJsonReport,
};
export type { FrapPlaywrightConfig, WithFrapOptions } from './config';
export type { FrapHealingEvent, HealingEvent, FrapReportSummary } from './reporter';
export {
  setCurrentPlaywrightTestId,
  getCurrentPlaywrightTestId,
  recordHealingEvent,
  consumeHealingEvents,
} from './healing-events';

export {
  attachFrapContext,
  recordContextUiEvent,
  writeContextReport,
  getContextTimeline,
  clearContextBuffers,
  loadAllContextEvents,
};
export type { ContextCaptureOptions } from './context';

export function frapPlaywright(
  userConfig?: Partial<FrapPlaywrightConfig>
): Partial<PlaywrightTestConfig> {
  const config = mergePlaywrightConfig(userConfig);
  
  const baseReporter = config.playwrightConfig?.reporter;
  const reporters: any[] = baseReporter
    ? (Array.isArray(baseReporter) ? baseReporter : [baseReporter])
    : [['list']];
  
  reporters.push(['@frap/playwright/dist/reporter.js', { 
    minConfidence: config.minConfidence,
    reportDir: config.reportDir,
    enableHealing: config.enableHealing,
    enableReporting: config.enableReporting,
    captureAll: config.captureAll,
    debug: config.debug,
  }]);
  
  const userBuild = config.playwrightConfig?.build;
  const userExternal = userBuild?.external;
  const externalList = Array.isArray(userExternal)
    ? userExternal
    : userExternal
      ? [userExternal]
      : [];

  const playwrightConfig: Partial<PlaywrightTestConfig> = {
    ...config.playwrightConfig,
    reporter: reporters as any,
    build: {
      ...userBuild,
      // Keep @frap/sdk (and WASM) on Node's native loader — Playwright must not babel-parse .wasm.
      external: [...externalList, '@frap/sdk'],
    },
  };

  return playwrightConfig;
}

export async function registerFrapSelector(
  selectors: any,
  config?: Partial<FrapConfig>
): Promise<void> {
  const fullConfig: FrapConfig = {
    minConfidence: 0.85,
    reportDir: './frap-reports',
    enableHealing: true,
    enableReporting: true,
    ...config,
  };

  await initFrapEngine(fullConfig);

  const engine = createFrapSelectorEngine(fullConfig);
  await selectors.register('frap', engine);
}

export const VERSION = '0.1.0';
