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
  debugReportSlug,
  debugReportsDir,
  debugReportJsonPath,
  writeDebugReport,
  clearDebugReports,
  buildElementFoundDebugReport,
} from './debug';

export {
  HealTrigger,
  HealPolicy,
  HealOutcome,
  HealingSemantics,
  classifyHealOutcome,
} from './healing-semantics';

export {
  ContextEvent,
  ContextTimeline,
  LogLevel,
  NetworkPhase,
  NetworkProtocol,
  MessageDirection,
  NetworkEventPayload,
  LogEventPayload,
  eventTimestampMs,
  sortTimelineEvents,
  getTimelineWindow,
  eventsByTraceId,
  networkBeforeUiFailure,
} from './context';

export const VERSION = '0.1.0';

export function greet(name: string): string {
  return `Hello, ${name}! Welcome to fletta v${VERSION}`;
}
