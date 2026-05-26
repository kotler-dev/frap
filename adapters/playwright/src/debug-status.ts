import type { DebugReport, DebugStep } from '@frap/sdk';

export type DebugStatusType = 'success' | 'warning' | 'failure';

export function isElementFoundWithoutHealing(report: DebugReport): boolean {
  const decisionStep = report.steps.find(s => s.name === 'healing_decision');
  if (!decisionStep?.output) return false;
  return (decisionStep.output as Record<string, unknown>).reason === 'element_found';
}

export function getOverallStatus(report: DebugReport): DebugStatusType {
  if (report.semantics?.outcome === 'unexpected_heal') {
    return 'warning';
  }
  if (isElementFoundWithoutHealing(report)) {
    return 'success';
  }
  if (report.healing.healed) {
    return report.semantics?.policy === 'deny' ? 'warning' : 'success';
  }

  const decisionStep = report.steps.find(s => s.name === 'healing_decision');
  if (decisionStep?.output) {
    const output = decisionStep.output as Record<string, unknown>;
    if (output.reason === 'ambiguous') return 'warning';
  }

  return 'failure';
}

export function getStepStatus(step: DebugStep, report: DebugReport): DebugStatusType {
  if (step.name === 'healing_decision') {
    return getOverallStatus(report);
  }
  if (step.name === 'candidates_ranked') {
    const output = step.output as Record<string, unknown> | undefined;
    const top3 = output?.top_3 as Array<{ confidence: number }> | undefined;
    if (!top3 || top3.length === 0) return 'failure';
    if (top3.length > 1 && top3[0].confidence - top3[1].confidence < 0.1) return 'warning';
  }
  return 'success';
}

export function statusTagClass(status: DebugStatusType): string {
  if (status === 'success') return 'tag-stable';
  if (status === 'warning') return 'tag-drift';
  return 'tag-failed';
}
