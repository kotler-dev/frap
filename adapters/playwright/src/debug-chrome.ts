import type { DebugManifest, DebugManifestEntry } from './debug-manifest';
import { statusTagClass } from './debug-status';

export function escapeHtml(str: string): string {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

export interface ReportHeaderOptions {
  title: string;
  subtitle: string;
  embed?: boolean;
  manifest?: DebugManifest;
  currentEntryId?: string;
  indexHref?: string;
  explorerHref?: string;
  showExplorerLink?: boolean;
  showClassicLink?: boolean;
}

export function renderThemeToggle(): string {
  return `<button class="theme-toggle" type="button" aria-label="Toggle theme">
        <svg class="icon-sun" aria-hidden="true"><use href="#icon-sun"/></svg>
        <svg class="icon-moon" aria-hidden="true"><use href="#icon-moon"/></svg>
      </button>`;
}

function renderViewLinks(opts: ReportHeaderOptions): string {
  const viewLinks: string[] = [];
  if (opts.showExplorerLink && opts.explorerHref) {
    viewLinks.push(
      `<a class="nav-view-link" href="${escapeHtml(opts.explorerHref)}">Explorer view (B)</a>`
    );
  }
  if (opts.showClassicLink && opts.indexHref) {
    viewLinks.push(
      `<a class="nav-view-link" href="${escapeHtml(opts.indexHref)}">Classic view (A)</a>`
    );
  }
  return viewLinks.join('');
}

export function renderReportNav(opts: ReportHeaderOptions): string {
  if (opts.embed) {
    return '';
  }

  const viewLinks = renderViewLinks(opts);

  if (!opts.manifest || opts.manifest.reportCount <= 1) {
    return viewLinks
      ? `<nav class="report-nav" aria-label="Report navigation">${viewLinks}</nav>`
      : '';
  }

  const entries = opts.manifest.entries;
  const currentIdx = opts.currentEntryId
    ? entries.findIndex(e => e.id === opts.currentEntryId)
    : -1;

  const prev = currentIdx > 0 ? entries[currentIdx - 1] : null;
  const next = currentIdx >= 0 && currentIdx < entries.length - 1 ? entries[currentIdx + 1] : null;

  const backHref = opts.indexHref ?? '../frap-debug.html';
  // Detail pages live in debug-reports/ — use basename only (not htmlHref from report root)
  const siblingHref = (entry: DebugManifestEntry) => `${entry.id}.html`;
  const prevHtml = prev
    ? `<a class="nav-btn" href="${escapeHtml(siblingHref(prev))}" title="${escapeHtml(prev.testName)}">‹ Prev</a>`
    : `<span class="nav-btn nav-btn--disabled">‹ Prev</span>`;
  const nextHtml = next
    ? `<a class="nav-btn" href="${escapeHtml(siblingHref(next))}" title="${escapeHtml(next.testName)}">Next ›</a>`
    : `<span class="nav-btn nav-btn--disabled">Next ›</span>`;

  const links = renderViewLinks(opts);

  return `
    <nav class="report-nav" aria-label="Report navigation">
      <a class="nav-btn nav-btn--back" href="${escapeHtml(backHref)}">← All reports</a>
      <span class="report-nav__sep"></span>
      ${prevHtml}
      ${nextHtml}
      ${links ? `<span class="report-nav__sep"></span>${links}` : ''}
    </nav>`;
}

export function renderReportHeader(opts: ReportHeaderOptions): string {
  const nav = renderReportNav(opts);
  return `
    <header class="site-header">
      <div class="site-header__brand">
        <h1>Fletta</h1>
        <p class="subtitle">${escapeHtml(opts.subtitle)}</p>
        ${nav}
      </div>
      <div class="site-header__actions">
        ${renderThemeToggle()}
      </div>
    </header>`;
}

export function renderIndexSummary(manifest: DebugManifest): string {
  const counts = { success: 0, warning: 0, failure: 0 };
  for (const e of manifest.entries) {
    counts[e.status]++;
  }

  return `
    <div class="index-summary">
      <span class="tag tag-stable">${counts.success} success</span>
      <span class="tag tag-drift">${counts.warning} warning</span>
      <span class="tag tag-failed">${counts.failure} failure</span>
    </div>`;
}

export function renderIndexTestRow(entry: DebugManifestEntry): string {
  const tagClass = statusTagClass(entry.status);
  return `
    <li class="index-test-row">
      <a class="index-test-link" href="${escapeHtml(entry.htmlHref)}">
        <span class="index-test-name">${escapeHtml(entry.leafName)}</span>
        <span class="tag ${tagClass}">${entry.status}</span>
        <span class="index-test-conf">${entry.confidence.toFixed(2)}</span>
      </a>
    </li>`;
}

export function renderGroupedIndexList(manifest: DebugManifest): string {
  const groups = new Map<string, DebugManifestEntry[]>();

  for (const entry of manifest.entries) {
    const key = entry.groupPath.join(' > ') || 'Other';
    if (!groups.has(key)) {
      groups.set(key, []);
    }
    groups.get(key)!.push(entry);
  }

  const sortedKeys = [...groups.keys()].sort();

  return sortedKeys
    .map(groupKey => {
      const items = groups.get(groupKey)!;
      const rows = items.map(renderIndexTestRow).join('');
      return `
        <details class="index-group" open>
          <summary class="index-group-title">${escapeHtml(groupKey)} <span class="index-group-count">(${items.length})</span></summary>
          <ul class="index-test-list">${rows}</ul>
        </details>`;
    })
    .join('');
}
