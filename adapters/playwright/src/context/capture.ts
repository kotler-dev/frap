import type { Page, Request, Response, WebSocket } from '@playwright/test';
import type {
  ContextEvent,
  LogLevel,
  MessageDirection,
  NetworkPhase,
  NetworkProtocol,
} from '@fletta/sdk';
import { randomUUID } from 'crypto';
import {
  getContextTraceId,
  markContextTestStart,
  pushContextEvent,
  setContextTraceId,
} from './store';
import { getCurrentPlaywrightTestId } from '../healing-events';

export interface ContextCaptureOptions {
  reportDir: string;
  testId?: string;
  traceId?: string;
}

const PAYLOAD_PREVIEW_MAX = 256;
const pendingRequests = new Map<string, { startMs: number; method: string; url: string }>();

function nowMs(): number {
  return Date.now();
}

function resolveTestId(explicit?: string): string | undefined {
  return explicit ?? getCurrentPlaywrightTestId();
}

function resolveTraceId(reportDir: string, testId: string | undefined, explicit?: string): string {
  const existing = explicit ?? getContextTraceId(reportDir, testId);
  if (existing) {
    return existing;
  }
  const id = randomUUID();
  setContextTraceId(reportDir, testId, id);
  return id;
}

function mapConsoleLevel(type: string): LogLevel {
  switch (type) {
    case 'error':
      return 'error';
    case 'warning':
      return 'warn';
    case 'info':
      return 'info';
    case 'debug':
      return 'debug';
    default:
      return 'log';
  }
}

function truncatePayload(payload: string | Buffer): string {
  const text = typeof payload === 'string' ? payload : payload.toString('utf-8');
  if (text.length <= PAYLOAD_PREVIEW_MAX) {
    return text;
  }
  return `${text.slice(0, PAYLOAD_PREVIEW_MAX)}…`;
}

function pushNetwork(
  reportDir: string,
  testId: string | undefined,
  traceId: string,
  phase: NetworkPhase,
  method: string,
  url: string,
  options: {
    status?: number;
    durationMs?: number;
    errorText?: string;
    protocol?: NetworkProtocol;
    direction?: MessageDirection;
    payloadPreview?: string;
  } = {}
): void {
  const event: ContextEvent = {
    kind: 'network',
    timestamp_ms: nowMs(),
    trace_id: traceId,
    request: {
      method,
      url,
      status: options.status,
      duration_ms: options.durationMs,
      phase,
      protocol: options.protocol ?? 'http',
      direction: options.direction,
      payload_preview: options.payloadPreview,
      error_text: options.errorText,
    },
  };
  pushContextEvent(reportDir, event, testId);
}

function attachWebSocketCapture(
  page: Page,
  reportDir: string,
  testId: string | undefined,
  traceId: string
): void {
  page.on('websocket', (ws: WebSocket) => {
    pushNetwork(reportDir, testId, traceId, 'open', 'WS', ws.url(), {
      protocol: 'websocket',
    });

    ws.on('framesent', (event) => {
      pushNetwork(reportDir, testId, traceId, 'message', 'WS', ws.url(), {
        protocol: 'websocket',
        direction: 'sent',
        payloadPreview: truncatePayload(event.payload),
      });
    });

    ws.on('framereceived', (event) => {
      pushNetwork(reportDir, testId, traceId, 'message', 'WS', ws.url(), {
        protocol: 'websocket',
        direction: 'received',
        payloadPreview: truncatePayload(event.payload),
      });
    });

    ws.on('close', () => {
      pushNetwork(reportDir, testId, traceId, 'close', 'WS', ws.url(), {
        protocol: 'websocket',
      });
    });
  });
}

/**
 * Attach network and console listeners for unified context capture (F002).
 * Call once per page in `beforeEach` when `captureAll` is enabled.
 */
export function attachFlettaContext(page: Page, options: ContextCaptureOptions): string {
  const testId = resolveTestId(options.testId);
  markContextTestStart(options.reportDir, testId);
  const traceId = resolveTraceId(options.reportDir, testId, options.traceId);

  page.on('request', (request: Request) => {
    const key = requestKey(request);
    pendingRequests.set(key, {
      startMs: nowMs(),
      method: request.method(),
      url: request.url(),
    });
    pushNetwork(
      options.reportDir,
      testId,
      traceId,
      'request',
      request.method(),
      request.url()
    );
  });

  page.on('response', (response: Response) => {
    const request = response.request();
    const key = requestKey(request);
    const pending = pendingRequests.get(key);
    const start = pending?.startMs ?? nowMs();
    pendingRequests.delete(key);
    pushNetwork(
      options.reportDir,
      testId,
      traceId,
      'response',
      request.method(),
      request.url(),
      { status: response.status(), durationMs: nowMs() - start }
    );
  });

  page.on('requestfailed', (request: Request) => {
    const key = requestKey(request);
    const pending = pendingRequests.get(key);
    pendingRequests.delete(key);
    const start = pending?.startMs ?? nowMs();
    pushNetwork(
      options.reportDir,
      testId,
      traceId,
      'failed',
      request.method(),
      request.url(),
      {
        durationMs: nowMs() - start,
        errorText: request.failure()?.errorText ?? 'request failed',
      }
    );
  });

  page.on('console', (msg) => {
    const location = msg.location();
    const event: ContextEvent = {
      kind: 'log',
      timestamp_ms: nowMs(),
      trace_id: traceId,
      log: {
        level: mapConsoleLevel(msg.type()),
        message: msg.text(),
        source: location.url || 'console',
      },
    };
    pushContextEvent(options.reportDir, event, testId);
  });

  page.on('pageerror', (error) => {
    const event: ContextEvent = {
      kind: 'log',
      timestamp_ms: nowMs(),
      trace_id: traceId,
      log: {
        level: 'error',
        message: error.message,
        source: 'pageerror',
      },
    };
    pushContextEvent(options.reportDir, event, testId);
  });

  attachWebSocketCapture(page, options.reportDir, testId, traceId);

  return traceId;
}

export function recordContextUiEvent(
  reportDir: string,
  element: string,
  action: string,
  detail?: string,
  testId?: string
): void {
  const resolvedTestId = resolveTestId(testId);
  const traceId = resolveTraceId(reportDir, resolvedTestId);
  const event: ContextEvent = {
    kind: 'ui',
    timestamp_ms: nowMs(),
    trace_id: traceId,
    element,
    action,
    detail,
  };
  pushContextEvent(reportDir, event, resolvedTestId);
}

function requestKey(request: Request): string {
  return `${request.method()}:${request.url()}`;
}
