import { test, expect } from '@playwright/test';
import { withFrap, getLastHealResult } from '@frap/playwright';
import { confFrap } from './helpers';
import * as fs from 'fs';
import * as path from 'path';

const FIXTURE_DIR = path.resolve(
  __dirname,
  '../../fixtures/contract/clustering-id-migration'
);

test.describe('Conference 2026 Spring', () => {
  test.describe('Clustering', () => {
    test('CONF-CL-REG-PASS: same cluster when li id migrates to data-id', async ({ page }, testInfo) => {
      const html = fs.readFileSync(path.join(FIXTURE_DIR, 'page-after.html'), 'utf-8');
      const expected = JSON.parse(
        fs.readFileSync(path.join(FIXTURE_DIR, 'expected.json'), 'utf-8')
      ) as {
        healed: boolean;
        min_confidence: number;
        best_candidate_attribute: string;
        best_candidate_value: string;
      };

      await page.setContent(html);

      const migrated = await withFrap(
        page.locator('ul[data-testid="participants"] > li[id="2"]'),
        page,
        confFrap({ minConfidence: expected.min_confidence, healPolicy: 'expect_heal', debug: true, testInfo })
      );

      await migrated.click();

      const healResult = getLastHealResult(migrated);
      expect(healResult).toBeDefined();
      expect(healResult!.healed).toBe(expected.healed);

      const needle = `${expected.best_candidate_attribute}="${expected.best_candidate_value}"`;
      expect(healResult!.top_candidates[0]?.selector).toContain(needle);
    });
  });
});
