/**
 * TypeScript fallback for healing (dev only). Production path uses Rust WASM via healJson.
 * Enable with FRAP_TS_FALLBACK=1.
 */

import type {
  Candidate,
  DOMElementInfo,
  DOMSnapshot,
  DOMToken,
  HealResult,
  Signature,
} from './core-types';

export function fallbackExtractSignature(dom: string, selector: string): Signature {
  const element = parseDOMElement(dom, selector);
  return fallbackExtractSignatureFromElement(element);
}

export function fallbackHeal(
  primarySelector: string,
  originalSignature: Signature,
  domSnapshot: DOMSnapshot,
  minConfidence: number
): HealResult {
  const element = domSnapshot.elements.find((e) => e.selector === primarySelector);

  if (element) {
    return {
      healed: false,
      selector: primarySelector,
      confidence: 1.0,
      top_candidates: [],
      original_signature: originalSignature,
    };
  }

  const candidates = findCandidates(originalSignature, domSnapshot);

  if (candidates.length === 0) {
    return {
      healed: false,
      selector: '',
      confidence: 0.0,
      diff: 'No suitable candidate found',
      top_candidates: [],
      original_signature: originalSignature,
    };
  }

  const sorted = candidates.sort((a, b) => b.confidence - a.confidence);
  const best = sorted[0];
  const secondBest = sorted[1];

  if (secondBest && best.confidence - secondBest.confidence < 0.1) {
    return {
      healed: false,
      selector: '',
      confidence: 0.0,
      diff: 'Ambiguous: multiple similar candidates found',
      top_candidates: candidates.slice(0, 3),
      original_signature: originalSignature,
    };
  }

  if (best.confidence >= minConfidence) {
    return {
      healed: true,
      selector: best.selector,
      confidence: best.confidence,
      diff: `Healed with confidence ${best.confidence.toFixed(2)}`,
      top_candidates: candidates.slice(0, 3),
      original_signature: originalSignature,
    };
  }

  return {
    healed: false,
    selector: '',
    confidence: 0.0,
    diff: 'No candidate met minConfidence threshold',
    top_candidates: candidates.slice(0, 3),
    original_signature: originalSignature,
  };
}

function findCandidates(original: Signature, snapshot: DOMSnapshot): Candidate[] {
  const candidates: Candidate[] = [];

  for (const element of snapshot.elements) {
    const elementSig = fallbackExtractSignatureFromElement(element);
    const confidence = calculateConfidence(original, elementSig);

    if (confidence >= 0.5) {
      candidates.push({
        selector: element.selector,
        signature: elementSig,
        confidence,
      });
    }
  }

  return candidates.sort((a, b) => b.confidence - a.confidence);
}

function calculateConfidence(original: Signature, candidate: Signature): number {
  const pathSim = calculatePathSimilarity(original.prefix, candidate.prefix);
  const tokenSim = calculateTokenSimilarity(original.path, candidate.path);
  const structuralSim =
    original.children_hash === candidate.children_hash
      ? 1.0
      : original.children_hash === 0 || candidate.children_hash === 0
        ? 0.5
        : 0.0;

  const bonus = calculateAttributeBonus(original, candidate);
  return Math.min(1.0, 0.5 * pathSim + 0.3 * tokenSim + 0.2 * structuralSim + bonus);
}

function calculatePathSimilarity(a: string, b: string): number {
  const distance = levenshteinDistance(a, b);
  const maxLen = Math.max(a.length, b.length);
  return maxLen === 0 ? 1.0 : 1.0 - distance / maxLen;
}

function calculateTokenSimilarity(a: DOMToken[], b: DOMToken[]): number {
  if (a.length === 0 && b.length === 0) return 1.0;

  const lcsLen = longestCommonSubsequenceLen(a, b, (t1, t2) => t1.tag === t2.tag && t1.role === t2.role);
  return lcsLen / Math.max(a.length, b.length);
}

function calculateAttributeBonus(original: Signature, candidate: Signature): number {
  let bonus = 0.0;

  if (
    original.text_content &&
    candidate.text_content &&
    original.text_content === candidate.text_content &&
    original.text_content.length > 0
  ) {
    bonus += 0.1;
  }

  for (const [key, value] of Object.entries(original.stable_attrs)) {
    if (candidate.stable_attrs[key] === value) {
      bonus += 0.05;
    }
  }

  return bonus;
}

function levenshteinDistance(a: string, b: string): number {
  const matrix: number[][] = [];

  for (let i = 0; i <= a.length; i++) {
    matrix[i] = [i];
  }

  for (let j = 0; j <= b.length; j++) {
    matrix[0][j] = j;
  }

  for (let i = 1; i <= a.length; i++) {
    for (let j = 1; j <= b.length; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1;
      matrix[i][j] = Math.min(matrix[i - 1][j] + 1, matrix[i][j - 1] + 1, matrix[i - 1][j - 1] + cost);
    }
  }

  return matrix[a.length][b.length];
}

function longestCommonSubsequenceLen<T>(a: T[], b: T[], matcher: (x: T, y: T) => boolean): number {
  const dp: number[][] = Array(a.length + 1)
    .fill(0)
    .map(() => Array(b.length + 1).fill(0));

  for (let i = 1; i <= a.length; i++) {
    for (let j = 1; j <= b.length; j++) {
      if (matcher(a[i - 1], b[j - 1])) {
        dp[i][j] = dp[i - 1][j - 1] + 1;
      } else {
        dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
      }
    }
  }

  return dp[a.length][b.length];
}

function fallbackExtractSignatureFromElement(element: DOMElementInfo): Signature {
  const tokens: DOMToken[] = element.path.map((pathToken, i) => {
    const parts = pathToken.split(':');
    return {
      tag: parts[0] || 'unknown',
      role: parts[1] && parts[1] !== '-' ? parts[1] : undefined,
      depth: i,
    };
  });

  const stableKeys = ['role', 'type', 'placeholder', 'aria-label', 'name'];
  const stable_attrs: Record<string, string> = {};

  for (const key of stableKeys) {
    if (element.attributes[key]) {
      stable_attrs[key] = element.attributes[key];
    }
  }

  return {
    path: tokens,
    prefix: tokens
      .slice(0, 5)
      .map((t) => `${t.tag}:${t.role || '-'}`)
      .join('>'),
    stable_attrs,
    text_content: element.text_content,
    position_in_parent: undefined,
    children_hash: 0,
    depth: tokens.length,
  };
}

function parseDOMElement(dom: string, selector: string): DOMElementInfo {
  return {
    selector,
    tag: 'div',
    attributes: {},
    text_content: undefined,
    path: ['div:-'],
  };
}
