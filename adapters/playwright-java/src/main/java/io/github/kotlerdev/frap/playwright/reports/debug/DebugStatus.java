package io.github.kotlerdev.frap.playwright.reports.debug;

import io.github.kotlerdev.frap.core.semantics.HealOutcome;

/**
 * Overall debug report status (mirrors TS {@code debug-status.ts}).
 */
public enum DebugStatus {
    SUCCESS, WARNING, FAILURE;

    public static DebugStatus overall(DebugReportModels.DebugReport report) {
        if (report.semantics() != null && report.semantics().outcome() == HealOutcome.UNEXPECTED_HEAL) {
            return WARNING;
        }
        if (report.healing().healed()) {
            return SUCCESS;
        }
        if (report.healing().topCandidates().size() >= 2) {
            return WARNING;
        }
        return FAILURE;
    }

    public static boolean elementFoundWithoutHealing(DebugReportModels.DebugReport report) {
        if (report.healing().healed()) {
            return false;
        }
        return report.healing().confidence() >= 1.0 && report.healing().topCandidates().isEmpty();
    }
}
