import * as fs from 'fs';
import * as path from 'path';
import type { DebugReport, DebugStep } from '@fletta/sdk';

export function generateDebugHtml(report: DebugReport, outputDir: string): string {
  const status = getOverallStatus(report);
  const statusBanner = getStatusBanner(status, report);

  const html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Fletta Debug Report - ${escapeHtml(report.testName)}</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }
    .container { max-width: 1200px; margin: 0 auto; }
    h1 { color: #333; border-bottom: 3px solid #ddd; padding-bottom: 10px; }

    /* Status Banner */
    .status-banner {
      padding: 20px;
      border-radius: 8px;
      margin-bottom: 20px;
      text-align: center;
      font-size: 1.5em;
      font-weight: 600;
      box-shadow: 0 2px 8px rgba(0,0,0,0.15);
    }
    .status-success {
      background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
      color: white;
      border-left: 5px solid #2E7D32;
    }
    .status-warning {
      background: linear-gradient(135deg, #FF9800 0%, #F57C00 100%);
      color: white;
      border-left: 5px solid #E65100;
    }
    .status-failure {
      background: linear-gradient(135deg, #f44336 0%, #d32f2f 100%);
      color: white;
      border-left: 5px solid #b71c1c;
    }

    /* Summary Box */
    .summary {
      background: white;
      padding: 20px;
      border-radius: 8px;
      margin-bottom: 20px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      border-left: 4px solid ${getStatusColor(status)};
    }

    /* Timeline */
    .timeline {
      background: white;
      padding: 20px;
      border-radius: 8px;
      margin-bottom: 20px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }
    .step {
      display: flex;
      align-items: center;
      padding: 12px;
      border-left: 3px solid #ddd;
      margin: 8px 0;
      background: #fafafa;
      border-radius: 0 4px 4px 0;
    }
    .step-success { border-left-color: #4CAF50; background: #f1f8f4; }
    .step-warning { border-left-color: #FF9800; background: #fff8f0; }
    .step-failure { border-left-color: #f44336; background: #fdf2f2; }
    .step-name { font-weight: 600; min-width: 200px; color: #333; }
    .step-duration { color: #666; font-size: 0.9em; margin-left: 20px; }
    .step-output { margin-left: auto; color: #666; font-size: 0.9em; }

    /* Clusters */
    .clusters {
      background: white;
      padding: 20px;
      border-radius: 8px;
      margin-bottom: 20px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      border-left: 4px solid #2196F3;
    }
    .cluster { margin: 15px 0; padding: 15px; background: #f9f9f9; border-radius: 6px; border-left: 3px solid #2196F3; }
    .cluster-header { font-weight: 600; color: #333; margin-bottom: 10px; }
    .cluster-elements { font-size: 0.9em; color: #666; }

    /* Decision */
    .decision {
      background: white;
      padding: 20px;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      border-left: 4px solid ${getStatusColor(status)};
    }
    .decision-healed { color: #4CAF50; font-weight: 600; font-size: 1.2em; }
    .decision-failed { color: #f44336; font-weight: 600; font-size: 1.2em; }
    .decision-warning { color: #FF9800; font-weight: 600; font-size: 1.2em; }

    /* Tables */
    .candidates-table { width: 100%; border-collapse: collapse; margin-top: 15px; }
    .candidates-table th, .candidates-table td { padding: 10px; text-align: left; border-bottom: 1px solid #ddd; }
    .candidates-table th { background: #f5f5f5; font-weight: 600; }
    .confidence-high { color: #4CAF50; font-weight: 600; }
    .confidence-medium { color: #FF9800; font-weight: 600; }
    .confidence-low { color: #f44336; font-weight: 600; }

    /* Metrics */
    .metrics { display: flex; gap: 20px; margin-top: 15px; }
    .metric-box {
      flex: 1;
      padding: 15px;
      background: #f9f9f9;
      border-radius: 6px;
      text-align: center;
    }
    .metric-value { font-size: 2em; font-weight: 700; color: #333; }
    .metric-label { font-size: 0.9em; color: #666; margin-top: 5px; }
  </style>
</head>
<body>
  <div class="container">
    <h1>Fletta Debug Report</h1>

    ${statusBanner}

    <div class="summary">
      <strong>Test:</strong> ${escapeHtml(report.testName)}<br>
      <strong>Timestamp:</strong> ${report.timestamp}<br>
      <strong>Duration:</strong> ${report.duration_ms}ms<br>
      <strong>Elements Scanned:</strong> ${getTotalElements(report)}
    </div>

    <div class="metrics">
      <div class="metric-box">
        <div class="metric-value">${report.steps.length}</div>
        <div class="metric-label">Steps Executed</div>
      </div>
      <div class="metric-box">
        <div class="metric-value">${report.clusters.length}</div>
        <div class="metric-label">DOM Clusters</div>
      </div>
      <div class="metric-box">
        <div class="metric-value">${report.healing.top_candidates.length}</div>
        <div class="metric-label">Candidates Found</div>
      </div>
    </div>

    <div class="timeline">
      <h2>Execution Timeline</h2>
      ${report.steps.map(step => {
        const stepStatus = getStepStatus(step, report);
        return `
        <div class="step step-${stepStatus}">
          <span class="step-name">${formatStepName(step.name)}</span>
          <span class="step-duration">${step.duration_ms}ms</span>
          <span class="step-output">${formatStepOutput(step)}</span>
        </div>
      `;
      }).join('')}
    </div>

    <div class="clusters">
      <h2>DOM Clusters (${report.clusters.length})</h2>
      ${report.clusters.map(cluster => `
        <div class="cluster">
          <div class="cluster-header">${escapeHtml(cluster.prefix)} (${cluster.element_count} elements)</div>
          <div class="cluster-elements">
            ${cluster.elements.map(e => escapeHtml(e.selector)).join(', ')}
          </div>
        </div>
      `).join('')}
    </div>

    <div class="decision">
      <h2>Healing Decision</h2>
      <div class="${getDecisionClass(status)}">
        ${getDecisionIcon(status)} ${getDecisionText(status, report)}
      </div>
      <p>Selector: <code>${escapeHtml(report.healing.selector) || 'N/A'}</code></p>
      <p>Confidence: <span class="${getConfidenceClass(report.healing.confidence)}">${report.healing.confidence.toFixed(2)}</span></p>
      ${getFailureReason(report)}

      <h3>Top Candidates</h3>
      <table class="candidates-table">
        <tr><th>Rank</th><th>Selector</th><th>Confidence</th></tr>
        ${report.healing.top_candidates.map((c, i) => `
          <tr>
            <td>${i + 1}</td>
            <td><code>${escapeHtml(c.selector)}</code></td>
            <td class="${getConfidenceClass(c.confidence)}">${c.confidence.toFixed(2)}</td>
          </tr>
        `).join('')}
      </table>
    </div>
  </div>
</body>
</html>`;

  const htmlPath = path.join(outputDir, 'fletta-debug.html');
  fs.writeFileSync(htmlPath, html);
  console.log(`[fletta:debug] HTML report: ${htmlPath}`);
  return htmlPath;
}

type StatusType = 'success' | 'warning' | 'failure';

function getOverallStatus(report: DebugReport): StatusType {
  if (report.healing.healed) return 'success';

  // Check if failed due to ambiguity
  const decisionStep = report.steps.find(s => s.name === 'healing_decision');
  if (decisionStep?.output) {
    const output = decisionStep.output as Record<string, unknown>;
    if (output.reason === 'ambiguous') return 'warning';
  }

  return 'failure';
}

function getStatusColor(status: StatusType): string {
  switch (status) {
    case 'success': return '#4CAF50';
    case 'warning': return '#FF9800';
    case 'failure': return '#f44336';
  }
}

function getStatusBanner(status: StatusType, report: DebugReport): string {
  const titles = {
    success: '✓ HEALING SUCCESSFUL',
    warning: '⚠ HEALING WARNING',
    failure: '✗ HEALING FAILED'
  };

  const subtitles = {
    success: `Element found with confidence ${report.healing.confidence.toFixed(2)}`,
    warning: 'Multiple similar candidates detected',
    failure: report.healing.top_candidates.length === 0
      ? 'No matching candidates found'
      : 'Confidence below threshold'
  };

  return `
    <div class="status-banner status-${status}">
      <div>${titles[status]}</div>
      <div style="font-size: 0.6em; margin-top: 8px; opacity: 0.9;">${subtitles[status]}</div>
    </div>
  `;
}

function getDecisionClass(status: StatusType): string {
  return `decision-${status === 'success' ? 'healed' : status}`;
}

function getDecisionIcon(status: StatusType): string {
  switch (status) {
    case 'success': return '✓';
    case 'warning': return '⚠';
    case 'failure': return '✗';
  }
}

function getDecisionText(status: StatusType, report: DebugReport): string {
  if (status === 'success') {
    return `HEALED (confidence: ${report.healing.confidence.toFixed(2)})`;
  }
  if (status === 'warning') {
    return 'AMBIGUOUS - Multiple similar candidates';
  }
  return 'FAILED - No suitable element found';
}

function getFailureReason(report: DebugReport): string {
  const decisionStep = report.steps.find(s => s.name === 'healing_decision');
  if (!decisionStep?.output) return '';

  const output = decisionStep.output as Record<string, unknown>;
  if (!output.reason || output.healed) return '';

  const reasons: Record<string, string> = {
    'no_candidates': 'No candidates met minimum confidence threshold (0.5)',
    'threshold_not_met': `Best candidate confidence (${output.confidence}) below threshold`,
    'ambiguous': 'Top candidates too similar (difference < 0.1)',
  };

  return `<p style="color: #f44336; background: #ffebee; padding: 10px; border-radius: 4px; margin-top: 10px;">
    <strong>Reason:</strong> ${reasons[output.reason as string] || output.reason}
  </p>`;
}

function getStepStatus(step: DebugStep, report: DebugReport): StatusType {
  if (step.name === 'healing_decision') {
    return getOverallStatus(report);
  }
  if (step.name === 'candidates_ranked') {
    const output = step.output as Record<string, unknown> | undefined;
    const top3 = output?.top_3 as Array<{confidence: number}> | undefined;
    if (!top3 || top3.length === 0) return 'failure';
    if (top3.length > 1 && top3[0].confidence - top3[1].confidence < 0.1) return 'warning';
  }
  return 'success';
}

function getTotalElements(report: DebugReport): number {
  const domStep = report.steps.find(s => s.name === 'dom_parsed');
  if (!domStep?.output) return 0;
  const output = domStep.output as Record<string, unknown>;
  return (output.total_elements as number) || (output.element_count as number) || 0;
}

function escapeHtml(str: string): string {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function formatStepName(name: string): string {
  return name.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
}

function formatStepOutput(step: DebugStep): string {
  if (!step.output) return '';
  const output = step.output as Record<string, unknown>;
  if (step.name === 'dom_parsed') {
    return output.found ? `Found: ${output.selector}` : `Elements: ${output.total_elements}`;
  }
  if (step.name === 'clusters_built') {
    return `Candidates: ${output.candidates_found}`;
  }
  if (step.name === 'candidates_ranked') {
    const top3 = (output.top_3 as Array<{confidence: number}>) || [];
    return `Top: ${top3[0]?.confidence.toFixed(2) || 'N/A'}`;
  }
  if (step.name === 'healing_decision') {
    return output.healed ? `Healed (${output.confidence})` : `Failed: ${output.reason}`;
  }
  return '';
}

function getConfidenceClass(confidence: number): string {
  if (confidence >= 0.85) return 'confidence-high';
  if (confidence >= 0.5) return 'confidence-medium';
  return 'confidence-low';
}
