import { FlettaConfig, mergeConfig as mergeBaseConfig } from '@fletta/sdk';
import type { PlaywrightTestConfig, TestInfo } from '@playwright/test';

export interface FlettaPlaywrightConfig extends FlettaConfig {
  playwrightConfig?: Partial<PlaywrightTestConfig>;
}

/** Options for {@link withFletta} (Playwright-specific) */
export interface WithFlettaOptions extends Partial<FlettaConfig> {
  /** Pass `test.info()` from the spec so reports get the correct Playwright test id */
  testInfo?: TestInfo;
}

export function mergePlaywrightConfig(
  userConfig?: Partial<FlettaPlaywrightConfig>
): FlettaPlaywrightConfig {
  return {
    ...mergeBaseConfig(userConfig),
    playwrightConfig: userConfig?.playwrightConfig || {},
  };
}
