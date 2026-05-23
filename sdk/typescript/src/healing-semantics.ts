/** Why healing was attempted */
export type HealTrigger = 'selector_missing' | 'action_failed';

/**
 * Test-level expectation for healing.
 * - allow: healing is OK (default)
 * - deny: healing must not occur (CP001 stable UI)
 * - expect_heal: healing is required (CP002 refactor)
 */
export type HealPolicy = 'allow' | 'deny' | 'expect_heal';

/** Classified result for reports and CI gates */
export type HealOutcome =
  | 'healed'
  | 'rejected'
  | 'unexpected_heal'
  | 'no_heal';

export interface HealingSemantics {
  trigger: HealTrigger;
  policy: HealPolicy;
  outcome: HealOutcome;
}

export function classifyHealOutcome(
  policy: HealPolicy,
  healed: boolean,
  healingAttempted: boolean
): HealOutcome {
  if (!healingAttempted) {
    return 'no_heal';
  }
  if (!healed) {
    return 'rejected';
  }
  if (policy === 'deny') {
    return 'unexpected_heal';
  }
  return 'healed';
}
