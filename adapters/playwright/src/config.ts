import { FrapConfig, mergeConfig as mergeBaseConfig } from '@frap/frap';
import type { PlaywrightTestConfig, TestInfo } from '@playwright/test';

export interface FrapPlaywrightConfig extends FrapConfig {
  playwrightConfig?: Partial<PlaywrightTestConfig>;
}

/** Options for {@link withFrap} (Playwright-specific) */
export interface WithFrapOptions extends Partial<FrapConfig> {
  /** Pass `test.info()` from the spec so reports get the correct Playwright test id */
  testInfo?: TestInfo;
}

export function mergePlaywrightConfig(
  userConfig?: Partial<FrapPlaywrightConfig>
): FrapPlaywrightConfig {
  return {
    ...mergeBaseConfig(userConfig),
    playwrightConfig: userConfig?.playwrightConfig || {},
  };
}
