/** JSON schema aligned with `crates/context` (fletta-context). */

export type LogLevel = 'log' | 'info' | 'warn' | 'error' | 'debug';

export type NetworkProtocol = 'http' | 'websocket';

export type MessageDirection = 'sent' | 'received';

export type NetworkPhase =
  | 'request'
  | 'response'
  | 'failed'
  | 'open'
  | 'message'
  | 'close';

export interface NetworkEventPayload {
  method: string;
  url: string;
  status?: number;
  duration_ms?: number;
  phase: NetworkPhase;
  protocol?: NetworkProtocol;
  direction?: MessageDirection;
  payload_preview?: string;
  error_text?: string;
}

export interface LogEventPayload {
  level: LogLevel;
  message: string;
  source: string;
}

export type ContextEvent =
  | {
      kind: 'ui';
      timestamp_ms: number;
      trace_id?: string;
      element: string;
      action: string;
      detail?: string;
    }
  | {
      kind: 'network';
      timestamp_ms: number;
      trace_id?: string;
      request: NetworkEventPayload;
    }
  | {
      kind: 'log';
      timestamp_ms: number;
      trace_id?: string;
      log: LogEventPayload;
    };

export interface ContextTimeline {
  events: ContextEvent[];
}

export function eventTimestampMs(event: ContextEvent): number {
  return event.timestamp_ms;
}

export function sortTimelineEvents(events: ContextEvent[]): ContextEvent[] {
  return [...events].sort((a, b) => eventTimestampMs(a) - eventTimestampMs(b));
}

/** Events in [failureAtMs - windowMs, failureAtMs + windowMs]. */
export function getTimelineWindow(
  timeline: ContextTimeline,
  failureAtMs: number,
  windowMs = 5000
): ContextEvent[] {
  const start = failureAtMs - windowMs;
  const end = failureAtMs + windowMs;
  return timeline.events.filter((e) => {
    const t = eventTimestampMs(e);
    return t >= start && t <= end;
  });
}

export function eventsByTraceId(timeline: ContextTimeline, traceId: string): ContextEvent[] {
  return sortTimelineEvents(
    timeline.events.filter((e) => 'trace_id' in e && e.trace_id === traceId)
  );
}

function isNetworkFailure(req: NetworkEventPayload): boolean {
  return req.phase === 'failed' || !!req.error_text || (req.status !== undefined && req.status >= 400);
}

/** True when a failed network event precedes a UI failure in the window. */
export function networkBeforeUiFailure(
  timeline: ContextTimeline,
  failureAtMs: number,
  windowMs = 5000
): boolean {
  const window = getTimelineWindow(timeline, failureAtMs, windowMs);
  let lastNetworkFail: number | undefined;
  let uiFailAt: number | undefined;

  for (const event of window) {
    if (event.kind === 'network') {
      if (isNetworkFailure(event.request)) {
        lastNetworkFail = event.timestamp_ms;
      }
    } else if (event.kind === 'ui' && (event.action === 'not_found' || event.action === 'failure')) {
      uiFailAt = event.timestamp_ms;
    }
  }

  return lastNetworkFail !== undefined && uiFailAt !== undefined && lastNetworkFail < uiFailAt;
}
