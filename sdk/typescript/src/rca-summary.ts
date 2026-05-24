import type { RcaReport } from './rca';

export function formatRcaSummary(report: RcaReport): string {
  return `[fletta-rca] ${report.primary_cause} (${report.confidence.toFixed(2)}): ${report.recommendation}`;
}
