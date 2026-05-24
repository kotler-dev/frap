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
      const { createRequire } = await import('node:module');
      const { dirname, join } = await import('node:path');
      const { fileURLToPath, pathToFileURL } = await import('node:url');
      const require = createRequire(import.meta.url);
      // file:// URL — Node native loader; avoids Playwright/babel parsing .wasm as JS.
      const distDir = dirname(fileURLToPath(import.meta.url));
      let wasmJs = join(distDir, '../wasm/fletta_core.js');
      try {
        const sdkRoot = dirname(require.resolve('@fletta/sdk/package.json'));
        wasmJs = join(sdkRoot, 'wasm/fletta_core.js');
      } catch {
        // Monorepo — path next to dist/rca.js
      }
      const wasm = (await import(pathToFileURL(wasmJs).href)) as unknown as WasmRcaModule;
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

