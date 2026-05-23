import * as fs from 'fs';
import * as path from 'path';
import type { DebugReport, DebugStep } from '@fletta/sdk';
import { debugReportSlug } from '@fletta/sdk';
import {
  escapeHtml,
  renderGroupedIndexList,
  renderIndexSummary,
  renderReportHeader,
} from './debug-chrome';
import {
  buildManifest,
  type DebugManifest,
  writeManifest,
} from './debug-manifest';
import { generateDebugExplorerPage } from './debug-explorer';
import {
  getOverallStatus,
  getStepStatus,
  isElementFoundWithoutHealing,
  type DebugStatusType,
} from './debug-status';
import {
  REPORT_EMBED_DETECT,
  REPORT_EMBED_THEME_LISTENER,
  REPORT_ICONS_SVG,
  REPORT_STYLES,
  REPORT_THEME_INIT,
  REPORT_THEME_TOGGLE,
} from './debug-report-styles';

export interface GenerateDebugHtmlOptions {
  htmlFileName?: string;
  manifest?: DebugManifest;
  currentEntryId?: string;
}

const EMBED_STYLES = `
html.embed-mode .site-header { display: none; }
html.embed-mode body { padding: 1rem 1.25rem; }
`;

export function generateDebugHtml(
  report: DebugReport,
  outputDir: string,
  htmlFileNameOrOpts: string | GenerateDebugHtmlOptions = 'fletta-debug.html'
): string {
  const opts: GenerateDebugHtmlOptions =
    typeof htmlFileNameOrOpts === 'string'
      ? { htmlFileName: htmlFileNameOrOpts }
      : htmlFileNameOrOpts;

  const htmlFileName = opts.htmlFileName ?? 'fletta-debug.html';
  const status = getOverallStatus(report);
  const manifest = opts.manifest;
  const multiReport = manifest && manifest.reportCount > 1;
  const entryId = opts.currentEntryId ?? debugReportSlug(report.testName);

  const header = multiReport
    ? renderReportHeader({
        title: 'Fletta',
        subtitle: `Debug report · ${report.testName}`,
        manifest,
        currentEntryId: entryId,
        indexHref: '../fletta-debug.html',
        explorerHref: '../fletta-debug-explorer.html',
        showExplorerLink: true,
      })
    : renderReportHeader({
        title: 'Fletta',
        subtitle: `Debug report · ${report.testName}`,
      });

  const html = `<!DOCTYPE html>
<html lang="en" data-theme="light" data-palette="warm-neutrals-semantic">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Fletta Debug — ${escapeHtml(report.testName)}</title>
  <script>${REPORT_THEME_INIT}</script>
  <script>${REPORT_EMBED_DETECT}</script>
  <style>${REPORT_STYLES}${EMBED_STYLES}</style>
</head>
<body>
  ${REPORT_ICONS_SVG}

  <div class="container">
    ${header}

    ${getStatusCallout(status, report)}

    <section class="section">
      <h2>Summary</h2>
      <div class="panel summary-panel">
        <dl class="meta-list">
          <div><strong>Test</strong> — ${escapeHtml(report.testName)}</div>
          <div><strong>Timestamp</strong> — ${escapeHtml(report.timestamp)}</div>
          <div><strong>Duration</strong> — ${report.duration_ms}ms</div>
          <div><strong>Elements scanned</strong> — ${getTotalElements(report)}</div>
        </dl>
        <div class="metrics-grid">
          <div class="metric-card">
            <div class="metric-value">${report.steps.length}</div>
            <div class="metric-label">Steps</div>
          </div>
          <div class="metric-card">
            <div class="metric-value">${report.clusters.length}</div>
            <div class="metric-label">Clusters</div>
          </div>
          <div class="metric-card">
            <div class="metric-value">${report.healing.top_candidates.length}</div>
            <div class="metric-label">Candidates</div>
          </div>
        </div>
      </div>
    </section>

    <section class="section">
      <h2>Execution timeline</h2>
      <div class="panel">
        <div class="element-list">
          ${report.steps.map(step => renderTimelineStep(step, report)).join('')}
        </div>
      </div>
    </section>

    <section class="section">
      <h2>DOM clusters (${report.clusters.length})</h2>
      <div class="panel">
        ${report.clusters.length === 0
          ? '<p style="color:var(--text-muted);font-size:0.875rem">No clusters recorded.</p>'
          : report.clusters.map(cluster => `
          <div class="cluster-block">
            <div class="cluster-prefix">${escapeHtml(cluster.prefix)} <span style="color:var(--text-muted);font-weight:400">(${cluster.element_count})</span></div>
            <div class="cluster-elements">${cluster.elements.map(e => escapeHtml(e.selector)).join(', ')}</div>
          </div>
        `).join('')}
      </div>
    </section>

    <section class="section">
      <h2>Healing decision</h2>
      <div class="panel">
        <div class="decision-line">
          ${getDecisionTag(status, report)}
        </div>
        <dl class="meta-list">
          <div><strong>Selector</strong> — <code>${escapeHtml(report.healing.selector) || 'N/A'}</code></div>
          <div><strong>Confidence</strong> — <span class="${getConfidenceValueClass(report.healing.confidence)}">${report.healing.confidence.toFixed(2)}</span></div>
        </dl>
        ${getSemanticsMeta(report)}
        ${getFailureReason(status, report)}

        <h3>Top candidates</h3>
        ${renderCandidates(report)}
      </div>
    </section>

    <footer class="report-footer">
      Generated by Fletta · warm neutrals report theme
    </footer>
  </div>

  <script>${REPORT_EMBED_THEME_LISTENER}</script>
  <script>${REPORT_THEME_TOGGLE}</script>
</body>
</html>`;

  const htmlPath = path.join(outputDir, htmlFileName);
  fs.writeFileSync(htmlPath, html);
  console.log(`[fletta:debug] HTML report: ${htmlPath}`);
  return htmlPath;
}

export function generateClassicIndexHtml(reportDir: string, manifest: DebugManifest): void {
  const grouped = renderGroupedIndexList(manifest);
  const summary = renderIndexSummary(manifest);

  const html = `<!DOCTYPE html>
<html lang="en" data-theme="light" data-palette="warm-neutrals-semantic">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Fletta Debug Reports</title>
  <script>${REPORT_THEME_INIT}</script>
  <style>${REPORT_STYLES}</style>
</head>
<body>
  ${REPORT_ICONS_SVG}
  <div class="container">
    ${renderReportHeader({
      title: 'Fletta',
      subtitle: `${manifest.reportCount} tests with debug enabled · run ${manifest.generatedAt}`,
      explorerHref: 'fletta-debug-explorer.html',
      showExplorerLink: true,
    })}
    <section class="section">
      <div class="index-hero">
        <h2>Debug reports</h2>
        ${summary}
      </div>
      <p class="index-hint">Only tests with <code>debug: true</code> appear here. Open <a class="nav-view-link" href="fletta-debug-explorer.html">Explorer view (B)</a> for sidebar navigation.</p>
      <div class="panel">${grouped}</div>
    </section>
  </div>
  <script>${REPORT_THEME_TOGGLE}</script>
</body>
</html>`;

  const indexPath = path.join(reportDir, 'fletta-debug.html');
  fs.writeFileSync(indexPath, html);
  console.log(
    `[fletta:debug] Classic index: ${indexPath} (${manifest.reportCount} reports)`
  );
}

export function generateAllDebugHtml(reportDir: string): DebugManifest | null {
  const subDir = path.join(reportDir, 'debug-reports');
  const items: Array<{ report: DebugReport; jsonFileName: string; htmlName: string }> = [];

  if (fs.existsSync(subDir)) {
    for (const file of fs.readdirSync(subDir).filter(f => f.endsWith('.json') && f !== 'manifest.json').sort()) {
      const report = JSON.parse(
        fs.readFileSync(path.join(subDir, file), 'utf-8')
      ) as DebugReport;
      const htmlName = file.replace(/\.json$/, '.html');
      items.push({ report, jsonFileName: file, htmlName });
    }
  }

  const legacyJson = path.join(reportDir, 'fletta-debug.json');
  if (items.length === 0 && fs.existsSync(legacyJson)) {
    const report = JSON.parse(fs.readFileSync(legacyJson, 'utf-8')) as DebugReport;
    generateDebugHtml(report, reportDir);
    return null;
  }

  if (items.length === 0) {
    return null;
  }

  const manifest = buildManifest(
    items.map(({ report, jsonFileName }) => ({ report, jsonFileName }))
  );
  writeManifest(reportDir, manifest);

  for (const item of items) {
    generateDebugHtml(item.report, subDir, {
      htmlFileName: item.htmlName,
      manifest,
      currentEntryId: item.jsonFileName.replace(/\.json$/, ''),
    });
  }

  if (items.length === 1) {
    fs.copyFileSync(
      path.join(subDir, items[0].htmlName),
      path.join(reportDir, 'fletta-debug.html')
    );
    generateDebugExplorerPage(reportDir, manifest);
    return manifest;
  }

  generateClassicIndexHtml(reportDir, manifest);
  generateDebugExplorerPage(reportDir, manifest);
  return manifest;
}

function getSemanticsMeta(report: DebugReport): string {
  if (!report.semantics) return '';
  const s = report.semantics;
  return `
        <dl class="meta-list semantics-meta">
          <div><strong>Trigger</strong> — <code>${escapeHtml(s.trigger)}</code></div>
          <div><strong>Policy</strong> — <code>${escapeHtml(s.policy)}</code></div>
          <div><strong>Outcome</strong> — <code>${escapeHtml(s.outcome)}</code></div>
        </dl>`;
}

function getStatusCallout(status: DebugStatusType, report: DebugReport): string {
  const unexpected = report.semantics?.outcome === 'unexpected_heal';
  const noHealingNeeded = isElementFoundWithoutHealing(report);
  const config = {
    success: {
      class: 'callout-success',
      icon: 'icon-check',
      title: noHealingNeeded
        ? 'No healing needed'
        : unexpected
          ? 'Unexpected heal'
          : 'Healing successful',
      sub: noHealingNeeded
        ? `Selector matched: ${report.healing.selector}`
        : unexpected
          ? `Healing ran but policy is "${report.semantics!.policy}" (stable UI gate violated)`
          : `Element found with confidence ${report.healing.confidence.toFixed(2)}`,
    },
    warning: {
      class: 'callout-warning',
      icon: 'icon-warning',
      title: unexpected ? 'Unexpected heal' : 'Healing warning',
      sub: unexpected
        ? `Policy "${report.semantics!.policy}" forbids healing on this test`
        : 'Multiple similar candidates detected',
    },
    failure: {
      class: 'callout-failure',
      icon: 'icon-warning',
      title: 'Healing failed',
      sub: report.healing.top_candidates.length === 0
        ? 'No matching candidates found'
        : 'Confidence below threshold',
    },
  }[status];

  return `
    <div class="callout ${config.class}">
      <svg class="icon icon--md callout-icon" aria-hidden="true"><use href="#${config.icon}"/></svg>
      <div>
        <span class="callout-title">${config.title}</span>
        <span class="callout-sub">${config.sub}</span>
      </div>
    </div>
  `;
}

function getDecisionTag(status: DebugStatusType, report: DebugReport): string {
  if (report.semantics?.outcome === 'unexpected_heal') {
    return `<span class="tag tag-drift"><svg class="icon icon--sm" aria-hidden="true"><use href="#icon-warning"/></svg> unexpected heal · ${report.healing.confidence.toFixed(2)}</span>`;
  }
  if (isElementFoundWithoutHealing(report)) {
    return `<span class="tag tag-stable"><svg class="icon icon--sm" aria-hidden="true"><use href="#icon-check"/></svg> no healing needed</span>`;
  }
  if (status === 'success') {
    return `<span class="tag tag-stable"><svg class="icon icon--sm" aria-hidden="true"><use href="#icon-check"/></svg> healed · ${report.healing.confidence.toFixed(2)}</span>`;
  }
  if (status === 'warning') {
    return `<span class="tag tag-drift"><svg class="icon icon--sm" aria-hidden="true"><use href="#icon-warning"/></svg> ambiguous</span>`;
  }
  return `<span class="tag tag-failed"><svg class="icon icon--sm" aria-hidden="true"><use href="#icon-warning"/></svg> failed</span>`;
}

function renderTimelineStep(step: DebugStep, report: DebugReport): string {
  const stepStatus = getStepStatus(step, report);
  const tagClass = stepStatus === 'success' ? 'tag-stable' : stepStatus === 'warning' ? 'tag-drift' : 'tag-failed';
  const output = formatStepOutput(step);

  return `
    <div class="element-item">
      <span class="tag ${tagClass}">${stepStatus}</span>
      <span class="element-name">${formatStepName(step.name)}</span>
      <span class="step-meta">
        ${step.duration_ms}ms
        ${output ? `<span class="step-output">${escapeHtml(output)}</span>` : ''}
      </span>
    </div>
  `;
}

function renderCandidates(report: DebugReport): string {
  const candidates = report.healing.top_candidates;
  if (candidates.length === 0) {
    return '<p style="color:var(--text-muted);font-size:0.875rem">No candidates ranked.</p>';
  }

  return `
    <table class="candidates-table">
      <thead><tr><th>#</th><th>Selector</th><th>Confidence</th></tr></thead>
      <tbody>
        ${candidates.map((c, i) => `
          <tr>
            <td>${i + 1}</td>
            <td><code>${escapeHtml(c.selector)}</code></td>
            <td class="${getConfidenceValueClass(c.confidence)}">${c.confidence.toFixed(2)}</td>
          </tr>
        `).join('')}
      </tbody>
    </table>
  `;
}

function getFailureReason(status: DebugStatusType, report: DebugReport): string {
  if (status === 'success') return '';

  const decisionStep = report.steps.find(s => s.name === 'healing_decision');
  if (!decisionStep?.output) return '';

  const output = decisionStep.output as Record<string, unknown>;
  if (!output.reason || output.healed) return '';

  const reasons: Record<string, string> = {
    no_candidates: 'No candidates met minimum confidence threshold (0.5)',
    threshold_not_met: `Best candidate confidence (${output.confidence}) below threshold`,
    ambiguous: 'Top candidates too similar (difference < 0.1)',
  };

  const text = reasons[output.reason as string] || String(output.reason);

  const calloutClass = status === 'warning' ? 'callout-warning' : 'callout-failure';

  return `
    <div class="callout ${calloutClass}" style="margin-top:1rem;margin-bottom:0">
      <svg class="icon icon--md callout-icon" aria-hidden="true"><use href="#icon-warning"/></svg>
      <div><span class="callout-title">Reason</span><span class="callout-sub">${escapeHtml(text)}</span></div>
    </div>
  `;
}

function getTotalElements(report: DebugReport): number {
  const domStep = report.steps.find(s => s.name === 'dom_parsed');
  if (!domStep?.output) return 0;
  const output = domStep.output as Record<string, unknown>;
  return (output.total_elements as number) || (output.element_count as number) || 0;
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
    const top3 = (output.top_3 as Array<{ confidence: number }>) || [];
    return `Top: ${top3[0]?.confidence.toFixed(2) || 'N/A'}`;
  }
  if (step.name === 'healing_decision') {
    return output.healed ? `Healed (${output.confidence})` : `Failed: ${output.reason}`;
  }
  return '';
}

function getConfidenceValueClass(confidence: number): string {
  if (confidence >= 0.85) return 'confidence-value--high';
  if (confidence >= 0.5) return 'confidence-value--medium';
  return 'confidence-value--low';
}
