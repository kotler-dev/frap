import * as fs from 'fs';
import * as path from 'path';
import { fallbackHeal } from '../core-fallback';
import type { DOMSnapshot, HealResult, Signature } from '../core-types';

const FIXTURE_DIR = path.resolve(
  __dirname,
  '../../../../fixtures/contract/clustering-id-migration'
);

interface ContractExpected {
  healed: boolean;
  min_confidence: number;
  best_candidate_attribute: string;
  best_candidate_value: string;
}

interface HealRequestFixture {
  primary_selector: string;
  original_signature: Signature;
  dom_snapshot: DOMSnapshot;
  min_confidence: number;
}

function loadFixture<T>(name: string): T {
  return JSON.parse(fs.readFileSync(path.join(FIXTURE_DIR, name), 'utf-8')) as T;
}

function assertContract(result: HealResult, expected: ContractExpected): void {
  expect(result.healed).toBe(expected.healed);
  expect(result.confidence).toBeGreaterThanOrEqual(expected.min_confidence);
  expect(result.top_candidates.length).toBeGreaterThan(0);

  const best = result.top_candidates[0];
  const needle = `${expected.best_candidate_attribute}="${expected.best_candidate_value}"`;
  expect(best.selector).toContain(needle);

  if (result.healed) {
    expect(result.selector).toContain(needle);
  }
}

describe('contract: clustering id → data-id migration', () => {
  it('fallbackHeal matches shared fixture expectations', () => {
    const request = loadFixture<HealRequestFixture>('request.json');
    const expected = loadFixture<ContractExpected>('expected.json');

    const result = fallbackHeal(
      request.primary_selector,
      request.original_signature,
      request.dom_snapshot,
      request.min_confidence
    );

    assertContract(result, expected);
  });
});
