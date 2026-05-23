import type { ContextTimeline } from './context';

export type PrimaryCause =
  | 'ui_change'
  | 'api_error'
  | 'infrastructure'
  | 'flaky'
  | 'unknown';

export interface CauseDetails {
  endpoint?: string;
  status?: number;
  element?: string;
  pattern?: string;
  correlation?: number;
  component?: string;
}

export interface RcaReport {
  version: number;
  primary_cause: PrimaryCause;
  confidence: number;
  timeline_excerpt: import('./context').ContextEvent[];
  recommendation: string;
  details: CauseDetails;
  failure_at_ms?: number;
}

export interface WasmRcaModule {
  analyzeRcaJson(timelineJson: string, failureAtMs: bigint): string;
}

let wasmModule: WasmRcaModule | null = null;
let wasmLoadPromise: Promise<WasmRcaModule | null> | null = null;

async function loadWasmModule(): Promise<WasmRcaModule | null> {
  if (wasmModule) {
    return wasmModule;
  }
  if (wasmLoadPromise) {
    return wasmLoadPromise;
  }

  wasmLoadPromise = (async () => {
    try {
      const wasm = (await import('../wasm/fletta_core.js')) as unknown as WasmRcaModule;
      if (typeof wasm.analyzeRcaJson !== 'function') {
        console.warn('[fletta] analyzeRcaJson export missing from fletta_core wasm');
        return null;
      }
      wasmModule = wasm;
      return wasmModule;
    } catch (err) {
      console.warn(
        '[fletta] WASM module not loaded for RCA; run `npm run build:wasm` in sdk/typescript',
        err
      );
      return null;
    }
  })();

  return wasmLoadPromise;
}

/** Analyze timeline and return RCA report (requires WASM build). */
export async function analyzeRca(
  timeline: ContextTimeline,
  failureAtMs = 0
): Promise<RcaReport> {
  const wasm = await loadWasmModule();
  if (!wasm) {
    throw new Error(
      'RCA requires fletta WASM. Run `npm run build:wasm` in sdk/typescript.'
    );
  }

  const timelineJson = JSON.stringify(timeline);
  const reportJson = wasm.analyzeRcaJson(timelineJson, BigInt(failureAtMs));
  return JSON.parse(reportJson) as RcaReport;
}

export function formatRcaSummary(report: RcaReport): string {
  return `[fletta-rca] ${report.primary_cause} (${report.confidence.toFixed(2)}): ${report.recommendation}`;
}
