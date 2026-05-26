import type { HealPolicy } from './healing-semantics';

export interface FlettaConfig {
  minConfidence: number;
  reportDir: string;
  enableHealing: boolean;
  enableReporting: boolean;
  debug?: boolean | 'verbose';
  /** Default: allow. Use deny for stable-selector gates (CP001). */
  healPolicy?: HealPolicy;
  /** Capture network, console, and UI events into `fletta-context.json` (F002). */
  captureAll?: boolean;
}

export const DEFAULT_CONFIG: FlettaConfig = {
  minConfidence: 0.85,
  reportDir: './fletta-reports',
  enableHealing: true,
  enableReporting: true,
  debug: true,
  healPolicy: 'allow',
};

export function mergeConfig(userConfig?: Partial<FlettaConfig>): FlettaConfig {
  return {
    ...DEFAULT_CONFIG,
    ...userConfig,
  };
}

export interface HealingReport {
  timestamp: string;
  testName: string;
  selector: string;
  healed: boolean;
  confidence: number;
  newSelector?: string;
  candidates: Array<{
    selector: string;
    confidence: number;
  }>;
  error?: string;
}

export interface JUnitTestCase {
  name: string;
  classname: string;
  time: number;
  healing?: {
    healed: boolean;
    originalSelector: string;
    newSelector?: string;
    confidence: number;
  };
  failure?: {
    message: string;
    type: string;
  };
}
