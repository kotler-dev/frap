export {
  FlettaConfig,
  DEFAULT_CONFIG,
  mergeConfig,
  HealingReport,
  JUnitTestCase,
} from './config';

export {
  Signature,
  DOMToken,
  Candidate,
  HealResult,
  DOMElementInfo,
  DOMSnapshot,
  HealingEngine,
  createHealingEngine,
} from './core';

export {
  DebugTracer,
  DebugReport,
  DebugStep,
  DebugStepName,
  ClusterView,
  HealingDebugInfo,
} from './debug';

export const VERSION = '0.1.0';

export function greet(name: string): string {
  return `Hello, ${name}! Welcome to fletta v${VERSION}`;
}
