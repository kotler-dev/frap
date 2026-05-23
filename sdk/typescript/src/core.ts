import { FlettaConfig } from './config';
import { DebugTracer, DebugReport, ClusterView, writeDebugReport } from './debug';
import type { HealingSemantics } from './healing-semantics';

export interface Signature {
  path: DOMToken[];
  prefix: string;
  stable_attrs: Record<string, string>;
  text_content?: string;
  position_in_parent?: number;
  children_hash: number;
  depth: number;
}

export interface DOMToken {
  tag: string;
  role?: string;
  semantic_type?: string;
  structural_class?: string;
  depth: number;
}

export interface Candidate {
  selector: string;
  signature: Signature;
  confidence: number;
}

export interface HealResult {
  healed: boolean;
  selector: string;
  confidence: number;
  diff?: string;
  top_candidates: Candidate[];
  original_signature: Signature;
  semantics?: HealingSemantics;
}

export interface DOMElementInfo {
  selector: string;
  tag: string;
  attributes: Record<string, string>;
  text_content?: string;
  path: string[];
}

export interface DOMSnapshot {
  html: string;
  elements: DOMElementInfo[];
}

export class HealingEngine {
  private config: FlettaConfig;
  private wasmModule: any | null = null;

  constructor(config: FlettaConfig) {
    this.config = config;
  }

  async init(): Promise<void> {
    // WASM module is optional - fallback TypeScript implementation works without it
    // To enable WASM: cd crates && wasm-pack build --target bundler --out-dir ../sdk/typescript/wasm
    this.wasmModule = null;
  }

  extractSignature(dom: string, selector: string): Signature {
    if (this.wasmModule?.extract_signature) {
      return this.wasmModule.extract_signature(dom, selector);
    }

    return this.fallbackExtractSignature(dom, selector);
  }

  heal(
    primarySelector: string,
    originalSignature: Signature,
    domSnapshot: DOMSnapshot,
    testName?: string
  ): HealResult {
    const tracer = this.config.debug ? new DebugTracer(testName || 'unknown') : null;

    if (tracer) {
      tracer.step('dom_parsed', { selector: primarySelector, element_count: domSnapshot.elements.length });
    }

    const element = domSnapshot.elements.find(e => e.selector === primarySelector);

    if (element) {
      if (tracer) {
        tracer.endStep('dom_parsed', { found: true, selector: primarySelector });
      }
      return {
        healed: false,
        selector: primarySelector,
        confidence: 1.0,
        top_candidates: [],
        original_signature: originalSignature,
      };
    }

    if (tracer) {
      tracer.endStep('dom_parsed', { found: false, total_elements: domSnapshot.elements.length });
      tracer.step('clusters_built');
    }

    console.log(`[fletta:engine] Looking for candidates in ${domSnapshot.elements.length} elements...`);
    const candidates = this.findCandidates(originalSignature, domSnapshot);
    console.log(`[fletta:engine] Found ${candidates.length} candidates with confidence >= 0.5`);

    if (tracer) {
      tracer.endStep('clusters_built', {
        candidates_found: candidates.length,
      });
      tracer.step('candidates_ranked');
    }

    if (candidates.length === 0) {
      console.log(`[fletta:engine] No candidates found, healing failed`);

      if (tracer) {
        tracer.endStep('candidates_ranked', { top_3: [] });
        tracer.step('healing_decision');
        tracer.endStep('healing_decision', {
          healed: false,
          confidence: 0.0,
          reason: 'no_candidates'
        });
        this.saveDebugReport(tracer.buildReport([], {
          healed: false,
          selector: '',
          confidence: 0.0,
          top_candidates: []
        }));
      }

      return {
        healed: false,
        selector: '',
        confidence: 0.0,
        diff: 'No suitable candidate found',
        top_candidates: [],
        original_signature: originalSignature,
      };
    }

    const sorted = candidates.sort((a, b) => b.confidence - a.confidence);
    const best = sorted[0];
    const secondBest = sorted[1];

    console.log(`[fletta:engine] Best candidate: "${best.selector}" confidence=${best.confidence.toFixed(2)}, threshold=${this.config.minConfidence}`);
    if (secondBest) {
      console.log(`[fletta:engine] Second best: "${secondBest.selector}" confidence=${secondBest.confidence.toFixed(2)}, diff=${(best.confidence - secondBest.confidence).toFixed(2)}`);
    }

    if (tracer) {
      tracer.endStep('candidates_ranked', {
        top_3: sorted.slice(0, 3).map(c => ({ selector: c.selector, confidence: c.confidence }))
      });
      tracer.step('healing_decision');
    }

    // Safety check: if top two candidates are too similar (within 0.1 confidence), it's ambiguous
    if (secondBest && (best.confidence - secondBest.confidence) < 0.1) {
      console.log(`[fletta:engine] Ambiguous: top candidates too similar, refusing to heal`);

      if (tracer) {
        tracer.endStep('healing_decision', {
          healed: false,
          confidence: 0.0,
          reason: 'ambiguous'
        });
        this.saveDebugReport(tracer.buildReport(
          this.buildClusterViews(candidates, domSnapshot),
          {
            healed: false,
            selector: '',
            confidence: 0.0,
            top_candidates: sorted.slice(0, 3).map(c => ({ selector: c.selector, confidence: c.confidence }))
          }
        ));
      }

      return {
        healed: false,
        selector: '',
        confidence: 0.0,
        diff: 'Ambiguous: multiple similar candidates found',
        top_candidates: candidates.slice(0, 3),
        original_signature: originalSignature,
      };
    }

    if (best.confidence >= this.config.minConfidence) {
      if (tracer) {
        tracer.endStep('healing_decision', {
          healed: true,
          confidence: best.confidence,
          reason: 'threshold_met'
        });
        this.saveDebugReport(tracer.buildReport(
          this.buildClusterViews(candidates, domSnapshot),
          {
            healed: true,
            selector: best.selector,
            confidence: best.confidence,
            top_candidates: sorted.slice(0, 3).map(c => ({ selector: c.selector, confidence: c.confidence }))
          }
        ));
      }

      return {
        healed: true,
        selector: best.selector,
        confidence: best.confidence,
        diff: `Healed with confidence ${best.confidence.toFixed(2)}`,
        top_candidates: candidates.slice(0, 3),
        original_signature: originalSignature,
      };
    }

    if (tracer) {
      tracer.endStep('healing_decision', {
        healed: false,
        confidence: best.confidence,
        reason: 'threshold_not_met'
      });
      this.saveDebugReport(tracer.buildReport(
        this.buildClusterViews(candidates, domSnapshot),
        {
          healed: false,
          selector: '',
          confidence: best.confidence,
          top_candidates: sorted.slice(0, 3).map(c => ({ selector: c.selector, confidence: c.confidence }))
        }
      ));
    }

    return {
      healed: false,
      selector: '',
      confidence: 0.0,
      diff: 'No candidate met minConfidence threshold',
      top_candidates: candidates.slice(0, 3),
      original_signature: originalSignature,
    };
  }

  private saveDebugReport(report: DebugReport): void {
    writeDebugReport(this.config.reportDir, report);
    console.log(`[fletta:debug] Report saved for "${report.testName}"`);
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
          elements: []
        });
      }
      const cluster = clusters.get(prefix)!;
      cluster.element_count++;
      if (cluster.elements.length < 5) {
        cluster.elements.push({
          selector: candidate.selector,
          signature_preview: `${candidate.signature.path[0]?.tag || 'unknown'}:${candidate.signature.text_content?.substring(0, 20) || ''}`,
          text_content: candidate.signature.text_content?.substring(0, 50)
        });
      }
    }

    return Array.from(clusters.values());
  }

  private findCandidates(original: Signature, snapshot: DOMSnapshot): Candidate[] {
    const candidates: Candidate[] = [];

    for (const element of snapshot.elements) {
      const elementSig = this.fallbackExtractSignatureFromElement(element);
      const confidence = this.calculateConfidence(original, elementSig);
      
      if (confidence >= 0.5) {
        candidates.push({
          selector: element.selector,
          signature: elementSig,
          confidence,
        });
      }
    }

    return candidates.sort((a, b) => b.confidence - a.confidence);
  }

  private calculateConfidence(original: Signature, candidate: Signature): number {
    const pathSim = this.calculatePathSimilarity(original.prefix, candidate.prefix);
    const tokenSim = this.calculateTokenSimilarity(original.path, candidate.path);
    const structuralSim = original.children_hash === candidate.children_hash ? 1.0 : 
      (original.children_hash === 0 || candidate.children_hash === 0) ? 0.5 : 0.0;
    
    const bonus = this.calculateAttributeBonus(original, candidate);
    const confidence = Math.min(1.0, 0.5 * pathSim + 0.3 * tokenSim + 0.2 * structuralSim + bonus);
    
    console.log(`[fletta:engine] Confidence calculation: pathSim=${pathSim.toFixed(2)}, tokenSim=${tokenSim.toFixed(2)}, structuralSim=${structuralSim.toFixed(2)}, bonus=${bonus.toFixed(2)} => ${confidence.toFixed(2)}`);
    console.log(`[fletta:engine]   Original text: "${original.text_content}", Candidate text: "${candidate.text_content}"`);
    
    return confidence;
  }

  private calculatePathSimilarity(a: string, b: string): number {
    const distance = this.levenshteinDistance(a, b);
    const maxLen = Math.max(a.length, b.length);
    return maxLen === 0 ? 1.0 : 1.0 - (distance / maxLen);
  }

  private calculateTokenSimilarity(a: DOMToken[], b: DOMToken[]): number {
    if (a.length === 0 && b.length === 0) return 1.0;
    
    const lcsLen = this.longestCommonSubsequenceLen(
      a, 
      b, 
      (t1, t2) => t1.tag === t2.tag && t1.role === t2.role
    );
    return lcsLen / Math.max(a.length, b.length);
  }

  private calculateAttributeBonus(original: Signature, candidate: Signature): number {
    let bonus = 0.0;

    if (original.text_content && candidate.text_content && 
        original.text_content === candidate.text_content && 
        original.text_content.length > 0) {
      bonus += 0.1;
    }

    for (const [key, value] of Object.entries(original.stable_attrs)) {
      if (candidate.stable_attrs[key] === value) {
        bonus += 0.05;
      }
    }

    return bonus;
  }

  private levenshteinDistance(a: string, b: string): number {
    const matrix: number[][] = [];
    
    for (let i = 0; i <= a.length; i++) {
      matrix[i] = [i];
    }
    
    for (let j = 0; j <= b.length; j++) {
      matrix[0][j] = j;
    }

    for (let i = 1; i <= a.length; i++) {
      for (let j = 1; j <= b.length; j++) {
        const cost = a[i - 1] === b[j - 1] ? 0 : 1;
        matrix[i][j] = Math.min(
          matrix[i - 1][j] + 1,
          matrix[i][j - 1] + 1,
          matrix[i - 1][j - 1] + cost
        );
      }
    }

    return matrix[a.length][b.length];
  }

  private longestCommonSubsequenceLen<T>(
    a: T[], 
    b: T[], 
    matcher: (x: T, y: T) => boolean
  ): number {
    const dp: number[][] = Array(a.length + 1)
      .fill(0)
      .map(() => Array(b.length + 1).fill(0));

    for (let i = 1; i <= a.length; i++) {
      for (let j = 1; j <= b.length; j++) {
        if (matcher(a[i - 1], b[j - 1])) {
          dp[i][j] = dp[i - 1][j - 1] + 1;
        } else {
          dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
        }
      }
    }

    return dp[a.length][b.length];
  }

  private fallbackExtractSignature(dom: string, selector: string): Signature {
    const element = this.parseDOMElement(dom, selector);
    return this.fallbackExtractSignatureFromElement(element);
  }

  private fallbackExtractSignatureFromElement(element: DOMElementInfo): Signature {
    const tokens: DOMToken[] = element.path.map((pathToken, i) => {
      const parts = pathToken.split(':');
      return {
        tag: parts[0] || 'unknown',
        role: parts[1] && parts[1] !== '-' ? parts[1] : undefined,
        depth: i,
      };
    });

    const stableKeys = ['role', 'type', 'placeholder', 'aria-label', 'name'];
    const stable_attrs: Record<string, string> = {};
    
    for (const key of stableKeys) {
      if (element.attributes[key]) {
        stable_attrs[key] = element.attributes[key];
      }
    }

    return {
      path: tokens,
      prefix: tokens
        .slice(0, 5)
        .map(t => `${t.tag}:${t.role || '-'}`)
        .join('>'),
      stable_attrs,
      text_content: element.text_content,
      position_in_parent: undefined,
      children_hash: 0,
      depth: tokens.length,
    };
  }

  private parseDOMElement(dom: string, selector: string): DOMElementInfo {
    return {
      selector,
      tag: 'div',
      attributes: {},
      text_content: undefined,
      path: ['div:-'],
    };
  }
}

export async function createHealingEngine(config: FlettaConfig): Promise<HealingEngine> {
  const engine = new HealingEngine(config);
  await engine.init();
  return engine;
}
