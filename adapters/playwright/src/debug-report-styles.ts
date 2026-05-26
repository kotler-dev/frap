/** Self-contained styles for fletta-debug.html (warm neutrals + status accents). */

export const REPORT_THEME_INIT = `(function () {
  var stored = localStorage.getItem('fletta-theme');
  if (!stored && window.matchMedia('(prefers-color-scheme: dark)').matches) {
    stored = 'dark';
  }
  document.documentElement.setAttribute('data-theme', stored || 'light');
})();`;

export const REPORT_THEME_TOGGLE = `(function () {
  var root = document.documentElement;
  var toggle = document.querySelector('.theme-toggle');
  if (!toggle) return;
  function applyTheme(theme) {
    root.setAttribute('data-theme', theme);
    localStorage.setItem('fletta-theme', theme);
  }
  toggle.addEventListener('click', function () {
    applyTheme(root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark');
  });
})();`;

export const REPORT_THEME_TOGGLE_EXPLORER = `(function () {
  var root = document.documentElement;
  var toggle = document.querySelector('.theme-toggle');
  var frame = document.getElementById('report-frame');
  if (!toggle) return;
  function applyTheme(theme) {
    root.setAttribute('data-theme', theme);
    localStorage.setItem('fletta-theme', theme);
    if (frame && frame.contentWindow) {
      try {
        frame.contentWindow.postMessage({ type: 'fletta-theme', theme: theme }, '*');
      } catch (e) {}
      var src = frame.getAttribute('src');
      if (src) {
        frame.setAttribute('src', src.split('#')[0]);
      }
    }
  }
  toggle.addEventListener('click', function () {
    applyTheme(root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark');
  });
})();`;

export const REPORT_EMBED_DETECT = `(function () {
  if (/[?&]embed=1(?:&|$)/.test(location.search)) {
    document.documentElement.classList.add('embed-mode');
  }
})();`;

export const REPORT_EMBED_THEME_LISTENER = `(function () {
  window.addEventListener('message', function (ev) {
    if (!ev.data || ev.data.type !== 'fletta-theme') return;
    document.documentElement.setAttribute('data-theme', ev.data.theme);
    localStorage.setItem('fletta-theme', ev.data.theme);
  });
})();`;

export const REPORT_ICONS_SVG = `<svg xmlns="http://www.w3.org/2000/svg" aria-hidden="true" style="display:none">
  <symbol id="icon-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
    <path d="M20 6L9 17l-5-5"/>
  </symbol>
  <symbol id="icon-warning" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
    <path d="M12 9v4M12 17h.01"/><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
  </symbol>
  <symbol id="icon-sun" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
    <circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41"/>
  </symbol>
  <symbol id="icon-moon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
    <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
  </symbol>
</svg>`;

export const REPORT_STYLES = `
*, *::before, *::after { margin: 0; padding: 0; box-sizing: border-box; }

:root {
  --primary-default: #FF4080;
  --secondary-default: #5EC8E8;
  --accent-flamingo: #FF4080;
  --accent-cyan: #5EC8E8;
  --accent-mint: #6EDDB8;
  --accent-peach: #FCA55D;
  --accent-border-flamingo: rgba(255, 64, 128, 0.2);
  --accent-border-cyan: rgba(94, 200, 232, 0.2);
  --accent-border-mint: rgba(110, 221, 184, 0.2);
  --accent-border-peach: rgba(252, 165, 93, 0.2);
}

[data-theme="light"] {
  --bg-default: #FFFFFF;
  --bg-elevated: #F7F6F3;
  --bg-overlay: #F1F1EF;
  --bg-highlight: #F7F6F3;
  --bg-hover: #EFEFEC;
  --text-default: #37352F;
  --text-muted: #6B6B67;
  --text-subtle: #9F9E99;
  --text-inverse: #FFFFFF;
  --accent-flamingo-bg: rgba(255, 64, 128, 0.08);
  --accent-cyan-bg: rgba(94, 200, 232, 0.08);
  --accent-mint-bg: rgba(110, 221, 184, 0.1);
  --accent-peach-bg: rgba(252, 165, 93, 0.1);
  --border-default: #E3E2DE;
  --border-subtle: #EFEFEC;
}

[data-theme="dark"] {
  --bg-default: #2C2C38;
  --bg-elevated: #383848;
  --bg-overlay: #424252;
  --bg-highlight: #343444;
  --bg-hover: #484858;
  --text-default: #ECECF2;
  --text-muted: #A8A8B8;
  --text-subtle: #787888;
  --text-inverse: #2C2C38;
  --accent-flamingo-bg: rgba(255, 64, 128, 0.12);
  --accent-cyan-bg: rgba(94, 200, 232, 0.12);
  --accent-mint-bg: rgba(110, 221, 184, 0.12);
  --accent-peach-bg: rgba(252, 165, 93, 0.12);
  --border-default: #505060;
  --border-subtle: #404050;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;
  background-color: var(--bg-default);
  color: var(--text-default);
  line-height: 1.5;
  padding: 2rem 1.5rem;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.container { max-width: 960px; margin: 0 auto; }

.site-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 2rem;
  padding-bottom: 1.25rem;
  border-bottom: 1px solid var(--border-subtle);
}

.site-header h1 {
  font-size: 1.75rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--text-default);
}

.subtitle {
  color: var(--text-muted);
  font-size: 0.9375rem;
  margin-top: 0.25rem;
}

.theme-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 8px;
  border: 1px solid var(--border-default);
  background: var(--bg-elevated);
  color: var(--text-muted);
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s ease, color 0.15s ease;
}

.theme-toggle:hover {
  background: var(--bg-hover);
  color: var(--text-default);
}

.theme-toggle svg { width: 1.25rem; height: 1.25rem; }

[data-theme="light"] .theme-toggle .icon-moon,
[data-theme="dark"] .theme-toggle .icon-sun { display: none; }

.section { margin-bottom: 2.5rem; }

h2 {
  font-size: 1.125rem;
  font-weight: 600;
  margin-bottom: 0.75rem;
  color: var(--text-default);
}

h3 {
  font-size: 0.875rem;
  font-weight: 600;
  margin: 1.25rem 0 0.75rem;
  color: var(--text-default);
}

.meta-list {
  display: grid;
  gap: 0.35rem;
  font-size: 0.875rem;
  color: var(--text-muted);
}

.meta-list strong { color: var(--text-default); font-weight: 500; }

.meta-list code {
  font-family: 'SF Mono', Monaco, Consolas, monospace;
  font-size: 0.8125rem;
  color: var(--text-default);
  background: var(--bg-overlay);
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
  border: 1px solid var(--border-subtle);
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
  margin-top: 1rem;
}

@media (max-width: 640px) {
  .metrics-grid { grid-template-columns: 1fr; }
}

.metric-card {
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);
  border-radius: 10px;
  padding: 1.25rem;
  text-align: center;
}

.metric-value {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text-default);
  letter-spacing: -0.02em;
}

.metric-label {
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--text-muted);
  margin-top: 0.35rem;
}

.panel {
  background: var(--bg-elevated);
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  padding: 1.25rem;
}

.summary-panel { margin-bottom: 0; }

.icon { width: 1em; height: 1em; flex-shrink: 0; display: inline-block; vertical-align: -0.125em; }
.icon--md { width: 1.125rem; height: 1.125rem; }
.icon--sm { width: 0.875rem; height: 0.875rem; }

.callout {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 1rem 1.125rem;
  border-radius: 10px;
  margin-bottom: 1.5rem;
  border: 1px solid var(--border-subtle);
  font-size: 0.875rem;
}

.callout-title { font-weight: 600; color: var(--text-default); display: block; margin-bottom: 0.2rem; }
.callout-sub { color: var(--text-muted); font-size: 0.8125rem; }

.callout-success {
  background: var(--accent-mint-bg);
  border-color: var(--accent-border-mint);
}
.callout-success .callout-icon { color: var(--accent-mint); }

.callout-warning {
  background: var(--accent-peach-bg);
  border-color: var(--accent-border-peach);
}
.callout-warning .callout-icon { color: var(--accent-peach); }

.callout-failure {
  background: var(--accent-flamingo-bg);
  border-color: var(--accent-border-flamingo);
}
.callout-failure .callout-icon { color: var(--accent-flamingo); }

.tag {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.2rem 0.45rem;
  border-radius: 4px;
  font-size: 0.6875rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border: 1px solid transparent;
  flex-shrink: 0;
}

.tag-stable {
  background: var(--accent-mint-bg);
  color: var(--accent-mint);
  border-color: var(--accent-border-mint);
}

.tag-drift {
  background: var(--accent-peach-bg);
  color: var(--accent-peach);
  border-color: var(--accent-border-peach);
}

.tag-failed {
  background: var(--accent-flamingo-bg);
  color: var(--accent-flamingo);
  border-color: var(--accent-border-flamingo);
}

.tag-info {
  background: var(--accent-cyan-bg);
  color: var(--accent-cyan);
  border-color: var(--accent-border-cyan);
}

.element-list { font-size: 0.875rem; }

.element-item {
  display: flex;
  align-items: center;
  padding: 0.625rem 0;
  border-bottom: 1px solid var(--border-subtle);
  gap: 0.75rem;
}

.element-item:last-child { border-bottom: none; }

.element-name {
  font-weight: 500;
  color: var(--text-default);
  min-width: 10rem;
}

.step-meta {
  margin-left: auto;
  text-align: right;
  font-size: 0.8125rem;
  color: var(--text-muted);
  font-family: 'SF Mono', Monaco, Consolas, monospace;
}

.step-output { display: block; font-size: 0.75rem; color: var(--text-subtle); margin-top: 0.15rem; }

.cluster-block {
  padding: 0.875rem 0;
  border-bottom: 1px solid var(--border-subtle);
}

.cluster-block:last-child { border-bottom: none; padding-bottom: 0; }
.cluster-block:first-child { padding-top: 0; }

.cluster-prefix {
  font-weight: 600;
  font-size: 0.875rem;
  color: var(--text-default);
  margin-bottom: 0.35rem;
}

.cluster-elements {
  font-size: 0.8125rem;
  color: var(--text-muted);
  font-family: 'SF Mono', Monaco, Consolas, monospace;
  line-height: 1.5;
  word-break: break-all;
}

.confidence-value--high { color: var(--accent-mint); }
.confidence-value--medium { color: var(--accent-peach); }
.confidence-value--low { color: var(--accent-flamingo); }

.candidates-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 0.5rem;
  font-size: 0.8125rem;
  table-layout: fixed;
}

.candidates-table th,
.candidates-table td {
  padding: 0.5rem 0.625rem;
  text-align: left;
  border-bottom: 1px solid var(--border-subtle);
  vertical-align: top;
}

.candidates-table th:first-child,
.candidates-table td:first-child {
  width: 2.5rem;
  text-align: center;
}

.candidates-table th:last-child,
.candidates-table td:last-child {
  width: 5.5rem;
  text-align: right;
  font-family: 'SF Mono', Monaco, Consolas, monospace;
}

.candidates-table td:nth-child(2) code {
  word-break: break-all;
}

.candidates-table th {
  color: var(--text-muted);
  font-weight: 600;
  font-size: 0.6875rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.candidates-table td code {
  font-family: 'SF Mono', Monaco, Consolas, monospace;
  font-size: 0.75rem;
  color: var(--text-default);
}

.decision-line {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-bottom: 0.75rem;
}

.decision-line .tag { font-size: 0.75rem; padding: 0.35rem 0.6rem; }

.report-footer {
  margin-top: 2.5rem;
  padding-top: 1rem;
  border-top: 1px solid var(--border-subtle);
  font-size: 0.75rem;
  color: var(--text-subtle);
}

body.embed-mode { padding: 1rem 1.25rem; }

.site-header__actions {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  flex-shrink: 0;
}

.report-nav {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.35rem 0.5rem;
  margin-top: 0.75rem;
}

.report-nav__sep {
  width: 1px;
  height: 1rem;
  background: var(--border-subtle);
  margin: 0 0.15rem;
}

.nav-btn {
  display: inline-flex;
  align-items: center;
  padding: 0.3rem 0.55rem;
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--text-muted);
  text-decoration: none;
  border: 1px solid var(--border-subtle);
  border-radius: 6px;
  background: var(--bg-overlay);
  transition: background 0.15s ease, color 0.15s ease;
}

.nav-btn:hover:not(.nav-btn--disabled) {
  background: var(--bg-hover);
  color: var(--text-default);
}

.nav-btn--back { color: var(--accent-mint); border-color: var(--accent-border-mint); }
.nav-btn--disabled { opacity: 0.4; cursor: default; }

.nav-view-link {
  font-size: 0.75rem;
  color: var(--accent-cyan);
  text-decoration: none;
  padding: 0.3rem 0.5rem;
}

.nav-view-link:hover { text-decoration: underline; }

.index-hero {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.index-summary { display: flex; flex-wrap: wrap; gap: 0.5rem; }

.index-hint {
  font-size: 0.8125rem;
  color: var(--text-muted);
  margin-bottom: 1rem;
}

.index-group {
  border: 1px solid var(--border-subtle);
  border-radius: 10px;
  margin-bottom: 0.75rem;
  background: var(--bg-default);
  overflow: hidden;
}

.index-group-title {
  padding: 0.75rem 1rem;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  list-style: none;
  color: var(--text-default);
}

.index-group-title::-webkit-details-marker { display: none; }

.index-group-count {
  color: var(--text-muted);
  font-weight: 400;
}

.index-test-list {
  list-style: none;
  border-top: 1px solid var(--border-subtle);
}

.index-test-row { border-bottom: 1px solid var(--border-subtle); }
.index-test-row:last-child { border-bottom: none; }

.index-test-link {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.625rem 1rem;
  text-decoration: none;
  color: inherit;
  transition: background 0.15s ease;
}

.index-test-link:hover { background: var(--bg-hover); }

.index-test-name {
  flex: 1;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-default);
}

.index-test-conf {
  font-size: 0.75rem;
  font-family: 'SF Mono', Monaco, Consolas, monospace;
  color: var(--text-muted);
}
`;

export const REPORT_EXPLORER_STYLES = `
body.explorer-layout {
  padding: 0;
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.explorer-shell {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.explorer-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.75rem 1.25rem;
  border-bottom: 1px solid var(--border-subtle);
  background: var(--bg-elevated);
  flex-shrink: 0;
}

.explorer-topbar h1 {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--text-default);
}

.explorer-topbar .subtitle {
  font-size: 0.8125rem;
  margin-top: 0.1rem;
}

.explorer-topbar__actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.explorer-body {
  display: flex;
  flex: 1;
  min-height: 0;
}

.explorer-sidebar {
  width: 280px;
  flex-shrink: 0;
  border-right: 1px solid var(--border-subtle);
  background: var(--bg-elevated);
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.explorer-search {
  padding: 0.75rem;
  border-bottom: 1px solid var(--border-subtle);
}

.explorer-search input {
  width: 100%;
  padding: 0.5rem 0.65rem;
  font-size: 0.8125rem;
  border: 1px solid var(--border-default);
  border-radius: 8px;
  background: var(--bg-default);
  color: var(--text-default);
}

.explorer-search input:focus {
  outline: 2px solid var(--accent-border-cyan);
  border-color: var(--accent-cyan);
}

.explorer-nav {
  flex: 1;
  overflow-y: auto;
  padding: 0.5rem 0;
}

.explorer-group-label {
  padding: 0.35rem 0.75rem;
  font-size: 0.6875rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-subtle);
}

.explorer-item {
  display: block;
  width: 100%;
  text-align: left;
  padding: 0.45rem 0.75rem 0.45rem 1rem;
  font-size: 0.8125rem;
  color: var(--text-muted);
  background: none;
  border: none;
  border-left: 3px solid transparent;
  cursor: pointer;
  font-family: inherit;
}

.explorer-item:hover {
  background: var(--bg-hover);
  color: var(--text-default);
}

.explorer-item.is-active {
  background: var(--accent-cyan-bg);
  color: var(--text-default);
  border-left-color: var(--accent-cyan);
}

.explorer-item.is-hidden { display: none; }

.explorer-main {
  flex: 1;
  min-width: 0;
  background: var(--bg-default);
}

.explorer-main iframe {
  width: 100%;
  height: 100%;
  border: none;
  display: block;
}

@media (max-width: 768px) {
  .explorer-body { flex-direction: column; }
  .explorer-sidebar { width: 100%; max-height: 40vh; border-right: none; border-bottom: 1px solid var(--border-subtle); }
}
`;
