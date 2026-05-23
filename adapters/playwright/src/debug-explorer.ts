import * as fs from 'fs';
import * as path from 'path';
import { formatRunTimestamp, type DebugManifest } from './debug-manifest';
import { escapeHtml, renderThemeToggle } from './debug-chrome';
import {
  REPORT_ICONS_SVG,
  REPORT_STYLES,
  REPORT_EXPLORER_STYLES,
  REPORT_THEME_INIT,
  REPORT_THEME_TOGGLE,
  REPORT_THEME_TOGGLE_EXPLORER,
} from './debug-report-styles';

/** Explorer B: full UI when 2+ reports, stub page when exactly 1 (avoids 404 on direct open). */
export function generateDebugExplorerPage(reportDir: string, manifest: DebugManifest): string {
  if (manifest.reportCount >= 2) {
    return writeFullExplorerHtml(reportDir, manifest);
  }
  if (manifest.reportCount === 1) {
    return writeExplorerStubHtml(reportDir, manifest);
  }
  return '';
}

function writeExplorerStubHtml(reportDir: string, manifest: DebugManifest): string {
  const entry = manifest.entries[0];
  const testLabel = entry ? escapeHtml(entry.testName) : '1 test';

  const html = `<!DOCTYPE html>
<html lang="en" data-theme="light" data-palette="warm-neutrals-semantic">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Fletta Debug Explorer</title>
  <script>${REPORT_THEME_INIT}</script>
  <style>${REPORT_STYLES}</style>
</head>
<body>
  ${REPORT_ICONS_SVG}
  <div class="container">
    <header class="site-header">
      <div class="site-header__brand">
        <h1>Fletta Explorer</h1>
        <p class="subtitle">Not available for this run</p>
      </div>
      <div class="site-header__actions">
        ${renderThemeToggle()}
      </div>
    </header>
    <div class="callout callout-warning">
      <svg class="icon icon--md callout-icon" aria-hidden="true"><use href="#icon-warning"/></svg>
      <div>
        <span class="callout-title">Explorer needs 2+ debug reports</span>
        <span class="callout-sub">This run has only one test with <code>debug: true</code> (${testLabel}). Use <a class="nav-view-link" href="fletta-debug.html">Classic view (A)</a> — it contains the full report. Run more debug-enabled tests in the same suite to enable sidebar navigation here.</span>
      </div>
    </div>
    <section class="section">
      <div class="panel">
        <p style="font-size:0.875rem;color:var(--text-muted);margin:0">
          <a class="nav-view-link" href="fletta-debug.html">← Open fletta-debug.html</a>
        </p>
      </div>
    </section>
  </div>
  <script>${REPORT_THEME_TOGGLE}</script>
</body>
</html>`;

  const outPath = path.join(reportDir, 'fletta-debug-explorer.html');
  fs.writeFileSync(outPath, html);
  console.log(`[fletta:debug] Explorer stub (1 report): ${outPath}`);
  return outPath;
}

function writeFullExplorerHtml(reportDir: string, manifest: DebugManifest): string {

  const manifestJson = JSON.stringify(manifest).replace(/</g, '\\u003c');
  const firstEmbed = manifest.entries[0]?.embedHref ?? '';
  const generatedAt = formatRunTimestamp(manifest.generatedAt);

  const html = `<!DOCTYPE html>
<html lang="en" data-theme="light" data-palette="warm-neutrals-semantic">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Fletta Debug Explorer</title>
  <script>${REPORT_THEME_INIT}</script>
  <style>${REPORT_STYLES}${REPORT_EXPLORER_STYLES}</style>
</head>
<body class="explorer-layout">
  ${REPORT_ICONS_SVG}
  <div class="explorer-shell">
    <header class="explorer-topbar">
      <div>
        <h1>Fletta Explorer</h1>
        <p class="subtitle">${manifest.reportCount} tests with debug enabled · run ${escapeHtml(generatedAt)}</p>
      </div>
      <div class="explorer-topbar__actions">
        <a class="nav-view-link" href="fletta-debug.html">Classic view (A)</a>
        ${renderThemeToggle()}
      </div>
    </header>
    <div class="explorer-body">
      <aside class="explorer-sidebar" aria-label="Test list">
        <div class="explorer-search">
          <input type="search" id="explorer-filter" placeholder="Filter tests…" autocomplete="off" />
        </div>
        <nav class="explorer-nav" id="explorer-nav"></nav>
      </aside>
      <main class="explorer-main">
        <iframe id="report-frame" title="Debug report" src="${escapeHtml(firstEmbed)}"></iframe>
      </main>
    </div>
  </div>
  <script type="application/json" id="fletta-manifest">${manifestJson}</script>
  <script>${EXPLORER_SCRIPT}</script>
  <script>${REPORT_THEME_TOGGLE_EXPLORER}</script>
</body>
</html>`;

  const outPath = path.join(reportDir, 'fletta-debug-explorer.html');
  fs.writeFileSync(outPath, html);
  console.log(`[fletta:debug] Explorer: ${outPath}`);
  return outPath;
}

/** @deprecated Use generateDebugExplorerPage */
export function generateDebugExplorerHtml(reportDir: string, manifest: DebugManifest): string {
  return generateDebugExplorerPage(reportDir, manifest);
}

const EXPLORER_SCRIPT = `(function () {
  var manifestEl = document.getElementById('fletta-manifest');
  var nav = document.getElementById('explorer-nav');
  var frame = document.getElementById('report-frame');
  var filterInput = document.getElementById('explorer-filter');
  if (!manifestEl || !nav || !frame) return;

  var manifest = JSON.parse(manifestEl.textContent || '{}');
  var entries = manifest.entries || [];
  var activeId = entries[0] ? entries[0].id : null;

  function groupKey(entry) {
    return (entry.groupPath && entry.groupPath.length) ? entry.groupPath.join(' > ') : 'Other';
  }

  function buildNav() {
    var groups = {};
    entries.forEach(function (e) {
      var k = groupKey(e);
      if (!groups[k]) groups[k] = [];
      groups[k].push(e);
    });
    var keys = Object.keys(groups).sort();
    nav.innerHTML = keys.map(function (k) {
      var items = groups[k].map(function (e) {
        return '<button type="button" class="explorer-item' + (e.id === activeId ? ' is-active' : '') + '" data-id="' + e.id + '" data-href="' + e.embedHref + '" data-search="' + (e.testName + ' ' + e.leafName).toLowerCase() + '">' + escapeHtml(e.leafName) + '</button>';
      }).join('');
      return '<div class="explorer-group" data-group="' + escapeHtml(k) + '"><div class="explorer-group-label">' + escapeHtml(k) + '</div>' + items + '</div>';
    }).join('');
  }

  function escapeHtml(s) {
    return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }

  function setActive(id, href) {
    activeId = id;
    frame.setAttribute('src', href);
    nav.querySelectorAll('.explorer-item').forEach(function (btn) {
      btn.classList.toggle('is-active', btn.getAttribute('data-id') === id);
    });
  }

  nav.addEventListener('click', function (ev) {
    var btn = ev.target.closest('.explorer-item');
    if (!btn || btn.classList.contains('is-hidden')) return;
    setActive(btn.getAttribute('data-id'), btn.getAttribute('data-href'));
  });

  if (filterInput) {
    filterInput.addEventListener('input', function () {
      var q = filterInput.value.trim().toLowerCase();
      nav.querySelectorAll('.explorer-item').forEach(function (btn) {
        var match = !q || (btn.getAttribute('data-search') || '').indexOf(q) !== -1;
        btn.classList.toggle('is-hidden', !match);
      });
      nav.querySelectorAll('.explorer-group').forEach(function (g) {
        var visible = g.querySelectorAll('.explorer-item:not(.is-hidden)').length > 0;
        g.style.display = visible ? '' : 'none';
      });
    });
  }

  buildNav();
})();`;
