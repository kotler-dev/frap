/**
 * HTML-AAM–style accessible name helpers for DOM snapshots.
 * Used by wrapper.ts (discover) and selector-engine.ts (healing snapshots).
 */

export function buildLabelMap(doc: Document): Map<string, string> {
  const labelMap = new Map<string, string>();
  doc.querySelectorAll('label[for]').forEach((label) => {
    const forId = label.getAttribute('for');
    const text = normalizeLabelText(label);
    if (forId && text) {
      labelMap.set(forId, text);
    }
  });
  doc.querySelectorAll('label').forEach((label) => {
    if (!label.hasAttribute('for')) {
      const control = label.querySelector(
        'input, button, select, textarea, [role="button"]'
      );
      if (control?.id) {
        const text = normalizeLabelText(label);
        if (text) {
          labelMap.set(control.id, text);
        }
      }
    }
  });
  return labelMap;
}

function normalizeLabelText(label: Element): string {
  return (((label as HTMLElement).innerText ?? label.textContent) || '')
    .replace(/\s+/g, ' ')
    .trim();
}

export function computeAccessibleName(
  el: Element,
  labelMap: Map<string, string>,
  doc: Document = el.ownerDocument ?? document
): string | undefined {
  const ariaLabel = el.getAttribute('aria-label')?.trim();
  if (ariaLabel) {
    return ariaLabel;
  }

  const labelledBy = el.getAttribute('aria-labelledby');
  if (labelledBy) {
    const texts = labelledBy
      .split(/\s+/)
      .map((id) => doc.getElementById(id))
      .filter((ref): ref is HTMLElement => ref != null)
      .map((ref) =>
        (((ref.innerText ?? ref.textContent) || '') as string)
          .replace(/\s+/g, ' ')
          .trim()
      )
      .filter(Boolean);
    if (texts.length) {
      return texts.join(' ');
    }
  }

  if (el.id && labelMap.has(el.id)) {
    return labelMap.get(el.id);
  }

  const parentLabel = el.closest('label');
  if (parentLabel && !parentLabel.hasAttribute('for')) {
    const clone = parentLabel.cloneNode(true) as HTMLElement;
    clone
      .querySelectorAll('input, button, select, textarea, [role="button"]')
      .forEach((node) => node.remove());
    const text = (clone.innerText || '').replace(/\s+/g, ' ').trim();
    if (text) {
      return text;
    }
  }

  const directCaption = el.querySelector(':scope > label');
  if (directCaption) {
    const text = normalizeLabelText(directCaption);
    if (text) {
      return text;
    }
  }

  const parent = el.parentElement;
  if (parent) {
    const siblingLabels = Array.from(parent.children).filter(
      (child) => child.tagName === 'LABEL' && !child.contains(el)
    );
    for (const label of siblingLabels) {
      const text = normalizeLabelText(label);
      if (text) {
        return text;
      }
    }
  }

  return undefined;
}

/** JS source embedded in page.evaluate (Java SnapshotBuilder parity). */
export const ACCESSIBLE_NAME_JS = `
function buildLabelMap() {
  const labelMap = new Map();
  document.querySelectorAll('label[for]').forEach((label) => {
    const forId = label.getAttribute('for');
    const text = (label.innerText || '').replace(/\\s+/g, ' ').trim();
    if (forId && text) labelMap.set(forId, text);
  });
  document.querySelectorAll('label').forEach((label) => {
    if (!label.hasAttribute('for')) {
      const control = label.querySelector('input, button, select, textarea, [role="button"]');
      if (control && control.id) {
        const text = (label.innerText || '').replace(/\\s+/g, ' ').trim();
        if (text) labelMap.set(control.id, text);
      }
    }
  });
  return labelMap;
}
function computeAccessibleName(el, labelMap) {
  const ariaLabel = el.getAttribute('aria-label');
  if (ariaLabel && ariaLabel.trim()) return ariaLabel.trim();
  const labelledBy = el.getAttribute('aria-labelledby');
  if (labelledBy) {
    const texts = labelledBy.split(/\\s+/).map((id) => document.getElementById(id))
      .filter(Boolean)
      .map((ref) => (ref.innerText || '').replace(/\\s+/g, ' ').trim())
      .filter(Boolean);
    if (texts.length) return texts.join(' ');
  }
  if (el.id && labelMap.has(el.id)) return labelMap.get(el.id);
  const parentLabel = el.closest('label');
  if (parentLabel && !parentLabel.hasAttribute('for')) {
    const clone = parentLabel.cloneNode(true);
    clone.querySelectorAll('input, button, select, textarea, [role="button"]').forEach((e) => e.remove());
    const text = (clone.innerText || '').replace(/\\s+/g, ' ').trim();
    if (text) return text;
  }
  const directCaption = el.querySelector(':scope > label');
  if (directCaption) {
    const text = (directCaption.innerText || '').replace(/\\s+/g, ' ').trim();
    if (text) return text;
  }
  const parent = el.parentElement;
  if (parent) {
    const siblingLabels = Array.from(parent.children).filter(
      (child) => child.tagName === 'LABEL' && !child.contains(el)
    );
    for (const label of siblingLabels) {
      const text = (label.innerText || '').replace(/\\s+/g, ' ').trim();
      if (text) return text;
    }
  }
  return undefined;
}
`;
