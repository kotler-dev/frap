import { FrapConfig } from './config';
import { DebugTracer, DebugReport, ClusterView, writeDebugReport } from './debug';
import { fallbackExtractSignature, fallbackHeal } from './core-fallback';
import type {
  Candidate,
  DOMSnapshot,
  HealResult,
  Signature,
  WasmHealModule,
} from './core-types';

export type {
  Signature,
  DOMToken,
  Candidate,
  HealResult,
  DOMElementInfo,
  DOMSnapshot,
} from './core-types';

export class HealingEngine {
  private config: FrapConfig;
  private wasmModule: WasmHealModule | null = null;
  private wasmLoadAttempted = false;

  constructor(config: FrapConfig) {
    this.config = config;
  }

  async init(): Promise<void> {
    if (process.env.FLETTA_TS_FALLBACK === '1') {
      console.warn('[frap] FLETTA_TS_FALLBACK=1 — using TypeScript healing (dev only)');
      this.wasmLoadAttempted = true;
      return;
    }

    try {
      const wasm = (await import('../wasm/frap_core.js')) as WasmHealModule;
      if (typeof wasm.healJson !== 'function') {
        throw new Error('healJson export missing from frap_core wasm');
      }
      this.wasmModule = wasm;
    } catch (err) {
      console.warn(
        '[frap] WASM module not loaded; run `npm run build:wasm` in sdk/typescript or set FLETTA_TS_FALLBACK=1',
        err
      );
    } finally {
      this.wasmLoadAttempted = true;
    }
  }

  private ensureWasmLoaded(): void {
    if (!this.wasmLoadAttempted) {
      throw new Error('HealingEngine.init() must be called before heal()');
    }
  }

  extractSignature(dom: string, selector: string): Signature {
    return fallbackExtractSignature(dom, selector);
  }

  heal(
    primarySelector: string,
    originalSignature: Signature,
    domSnapshot: DOMSnapshot,
    testName?: string
  ): HealResult {
    this.ensureWasmLoaded();

    const tracer = this.config.debug ? new DebugTracer(testName || 'unknown') : null;

    if (this.wasmModule) {
      if (tracer) {
        tracer.step('dom_parsed', { selector: primarySelector, element_count: domSnapshot.elements.length });
      }

      const request = {
        primary_selector: primarySelector,
        original_signature: originalSignature,
        dom_snapshot: domSnapshot,
        min_confidence: this.config.minConfidence,
      };

      const raw = this.wasmModule.healJson(JSON.stringify(request));
      const result = JSON.parse(raw) as HealResult;

      if (tracer) {
        tracer.endStep('dom_parsed', {
          healed: result.healed,
          confidence: result.confidence,
          selector: result.selector,
        });
        if (this.config.debug && result.top_candidates.length > 0) {
          this.saveDebugReport(
            tracer.buildReport(
              this.buildClusterViews(result.top_candidates, domSnapshot),
              {
                healed: result.healed,
                selector: result.selector,
                confidence: result.confidence,
                top_candidates: result.top_candidates.map((c) => ({
                  selector: c.selector,
                  confidence: c.confidence,
                })),
              }
            )
          );
        }
      }

      return result;
    }

    if (process.env.FLETTA_TS_FALLBACK !== '1') {
      console.warn('[frap] WASM unavailable — using TS fallback for this heal() call');
    }

    return this.healWithFallback(primarySelector, originalSignature, domSnapshot, tracer);
  }

  private healWithFallback(
    primarySelector: string,
    originalSignature: Signature,
    domSnapshot: DOMSnapshot,
    tracer: DebugTracer | null
  ): HealResult {
    if (tracer) {
      tracer.step('dom_parsed', { selector: primarySelector, element_count: domSnapshot.elements.length });
    }

    const result = fallbackHeal(
      primarySelector,
      originalSignature,
      domSnapshot,
      this.config.minConfidence
    );

    if (tracer) {
      tracer.endStep('dom_parsed', { found: !result.healed && result.confidence === 1.0 });
      tracer.step('healing_decision');
      tracer.endStep('healing_decision', {
        healed: result.healed,
        confidence: result.confidence,
      });
    }

    return result;
  }

  private saveDebugReport(report: DebugReport): void {
    writeDebugReport(this.config.reportDir, report);
    console.log(`[frap:debug] Report saved for "${report.testName}"`);
  }

  private buildClusterViews(candidates: Candidate[], snapshot: DOMSnapshot): ClusterView[] {
    const clusters = new Map<string, ClusterView>();

    for (const candidate of candidates) {
      const prefix = candidate.signature.prefix;
      if (!clusters.has(prefix)) {
        clusters.set(prefix, {
          id: `cluster_${prefix.replace(/>/g, '_')}`,
          prefix,
          element_count: 0,
          elements: [],
        });
      }
      const cluster = clusters.get(prefix)!;
      cluster.element_count++;
      if (cluster.elements.length < 5) {
        cluster.elements.push({
          selector: candidate.selector,
          signature_preview: `${candidate.signature.path[0]?.tag || 'unknown'}:${candidate.signature.text_content?.substring(0, 20) || ''}`,
          text_content: candidate.signature.text_content?.substring(0, 50),
        });
      }
    }

    return Array.from(clusters.values());
  }
}

export async function createHealingEngine(config: FrapConfig): Promise<HealingEngine> {
  const engine = new HealingEngine(config);
  await engine.init();
  return engine;
}
