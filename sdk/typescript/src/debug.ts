export type DebugStepName = 'dom_parsed' | 'clusters_built' | 'candidates_ranked' | 'healing_decision';

export interface DebugStep {
  name: DebugStepName;
  timestamp: string;
  duration_ms: number;
  input?: unknown;
  output?: unknown;
}

export interface ClusterView {
  id: string;
  prefix: string;
  element_count: number;
  elements: Array<{
    selector: string;
    signature_preview: string;
    text_content?: string;
  }>;
}

export interface HealingDebugInfo {
  healed: boolean;
  selector: string;
  confidence: number;
  top_candidates: Array<{
    selector: string;
    confidence: number;
  }>;
}

export interface DebugReport {
  timestamp: string;
  testName: string;
  duration_ms: number;
  steps: DebugStep[];
  clusters: ClusterView[];
  healing: HealingDebugInfo;
}

export class DebugTracer {
  private steps: DebugStep[] = [];
  private stepStartTimes: Map<string, number> = new Map();
  private startTime: number;

  constructor(private testName: string) {
    this.startTime = Date.now();
  }

  step(name: DebugStepName, input?: unknown): void {
    this.stepStartTimes.set(name, Date.now());
    this.steps.push({
      name,
      timestamp: new Date().toISOString(),
      duration_ms: 0,
      input,
    });
  }

  endStep(name: DebugStepName, output?: unknown): void {
    const startTime = this.stepStartTimes.get(name);
    const duration = startTime ? Date.now() - startTime : 0;

    const step = this.steps.find(s => s.name === name && s.duration_ms === 0);
    if (step) {
      step.duration_ms = duration;
      step.output = output;
    }
  }

  buildReport(clusters: ClusterView[], healing: HealingDebugInfo): DebugReport {
    return {
      timestamp: new Date().toISOString(),
      testName: this.testName,
      duration_ms: Date.now() - this.startTime,
      steps: this.steps,
      clusters,
      healing,
    };
  }
}
