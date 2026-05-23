import * as fs from 'fs';
import * as path from 'path';
import type { DebugReport } from '@fletta/sdk';
import { debugReportSlug } from '@fletta/sdk';
import { getOverallStatus, type DebugStatusType } from './debug-status';

export interface DebugManifestEntry {
  id: string;
  testName: string;
  groupPath: string[];
  leafName: string;
  htmlHref: string;
  embedHref: string;
  status: DebugStatusType;
  healed: boolean;
  confidence: number;
  timestamp: string;
  hasSemantics: boolean;
}

export interface DebugManifest {
  generatedAt: string;
  reportCount: number;
  entries: DebugManifestEntry[];
}

export function parseTestNameParts(testName: string): { groupPath: string[]; leafName: string } {
  const sep = ' > ';
  if (!testName.includes(sep)) {
    return { groupPath: ['Other'], leafName: testName };
  }
  const parts = testName.split(sep);
  const leafName = parts[parts.length - 1] ?? testName;
  const groupPath = parts.slice(0, -1);
  return { groupPath: groupPath.length > 0 ? groupPath : ['Other'], leafName };
}

export function buildManifestEntry(
  report: DebugReport,
  jsonFileName: string
): DebugManifestEntry {
  const id = jsonFileName.replace(/\.json$/, '');
  const htmlName = `${id}.html`;
  const { groupPath, leafName } = parseTestNameParts(report.testName);

  return {
    id,
    testName: report.testName,
    groupPath,
    leafName,
    htmlHref: `debug-reports/${htmlName}`,
    embedHref: `debug-reports/${htmlName}?embed=1`,
    status: getOverallStatus(report),
    healed: report.healing.healed,
    confidence: report.healing.confidence,
    timestamp: report.timestamp,
    hasSemantics: Boolean(report.semantics),
  };
}

export function buildManifest(
  items: Array<{ report: DebugReport; jsonFileName: string }>
): DebugManifest {
  const entries = items.map(({ report, jsonFileName }) =>
    buildManifestEntry(report, jsonFileName)
  );

  return {
    generatedAt: new Date().toISOString(),
    reportCount: entries.length,
    entries,
  };
}

export function writeManifest(reportDir: string, manifest: DebugManifest): string {
  const subDir = path.join(reportDir, 'debug-reports');
  if (!fs.existsSync(subDir)) {
    fs.mkdirSync(subDir, { recursive: true });
  }
  const manifestPath = path.join(subDir, 'manifest.json');
  fs.writeFileSync(manifestPath, JSON.stringify(manifest, null, 2));
  return manifestPath;
}

export function readManifest(reportDir: string): DebugManifest | null {
  const manifestPath = path.join(reportDir, 'debug-reports', 'manifest.json');
  if (!fs.existsSync(manifestPath)) {
    return null;
  }
  return JSON.parse(fs.readFileSync(manifestPath, 'utf-8')) as DebugManifest;
}

/** Slug from JSON filename (without extension) — matches debugReportSlug output */
export function entryIdFromReport(report: DebugReport): string {
  return debugReportSlug(report.testName);
}
