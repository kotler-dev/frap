import { FlettaConfig, mergeConfig as mergeBaseConfig } from '@fletta/sdk';
import type { PlaywrightTestConfig } from '@playwright/test';

export interface FlettaPlaywrightConfig extends FlettaConfig {
  playwrightConfig?: Partial<PlaywrightTestConfig>;
}

export function mergePlaywrightConfig(
  userConfig?: Partial<FlettaPlaywrightConfig>
): FlettaPlaywrightConfig {
  return {
    ...mergeBaseConfig(userConfig),
    playwrightConfig: userConfig?.playwrightConfig || {},
  };
}
