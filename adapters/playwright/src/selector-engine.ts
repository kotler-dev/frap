// SelectorEngine interface from Playwright
type SelectorEngine = {
  query(root: Element | Document, selector: string): Element | null | Promise<Element | null>;
  queryAll?(root: Element | Document, selector: string): Element[] | Promise<Element[]>;
};
import { HealingEngine, FlettaConfig, createHealingEngine, DOMSnapshot, DOMElementInfo } from '@frap/sdk';

interface FlettaSelectorEngine extends SelectorEngine {
  _healingEngine?: HealingEngine;
  _config: FlettaConfig;
  _signatures: Map<string, any>;
}

let globalEngine: HealingEngine | null = null;
let globalConfig: FlettaConfig | null = null;
let recordedSignatures: Map<string, any> = new Map();

export async function initFlettaEngine(config: FlettaConfig): Promise<void> {
  globalEngine = await createHealingEngine(config);
  globalConfig = config;
}

export function recordSignature(selector: string, signature: any): void {
  recordedSignatures.set(selector, signature);
}

export function createFlettaSelectorEngine(config: FlettaConfig): FlettaSelectorEngine {
  const engine: FlettaSelectorEngine = {
    _config: config,
    _signatures: recordedSignatures,

    async query(root: Element | Document, selector: string): Promise<Element | null> {
      if (!globalEngine) {
        await initFlettaEngine(config);
      }

      const doc = root instanceof Document ? root : root.ownerDocument;
      if (!doc) return null;

      let element = doc.querySelector(selector);
      
      if (element) {
        return element;
      }

      if (!config.enableHealing) {
        return null;
      }

      const snapshot = buildDOMSnapshot(doc);
      const originalSig = recordedSignatures.get(selector);
      
      if (!originalSig || !globalEngine) {
        return null;
      }

      const result = globalEngine.heal(selector, originalSig, snapshot);
      
      if (result.healed && result.selector) {
        const healedElement = doc.querySelector(result.selector);
        
        if (healedElement && typeof window !== 'undefined') {
          (healedElement as any).__frapHealed = true;
          (healedElement as any).__frapConfidence = result.confidence;
          (healedElement as any).__frapOriginalSelector = selector;
        }
        
        return healedElement;
      }

      return null;
    },

    queryAll(root: Element | Document, selector: string): Element[] {
      const doc = root instanceof Document ? root : root.ownerDocument;
      if (!doc) return [];
      
      const elements = Array.from(doc.querySelectorAll(selector));
      return elements;
    },
  };

  return engine;
}

function buildDOMSnapshot(doc: Document): DOMSnapshot {
  const elements: DOMElementInfo[] = [];
  const allElements = doc.querySelectorAll('*');
  
  allElements.forEach((el, index) => {
    const attributes: Record<string, string> = {};
    const attrs = Array.from(el.attributes);
    for (const attr of attrs) {
      attributes[attr.name] = attr.value;
    }

    const path: string[] = [];
    let current: Element | null = el;
    while (current && current !== doc.body) {
      const role = current.getAttribute('role');
      path.unshift(`${current.tagName.toLowerCase()}:${role || '-'}`);
      current = current.parentElement;
    }

    const selector = buildSelector(el, index);
    
    elements.push({
      selector,
      tag: el.tagName.toLowerCase(),
      attributes,
      text_content: el.textContent || undefined,
      path,
    });
  });

  return {
    html: doc.documentElement.outerHTML,
    elements,
  };
}

function buildSelector(el: Element, index: number): string {
  const tag = el.tagName.toLowerCase();
  
  if (el.id) {
    return `#${el.id}`;
  }
  
  const testId = el.getAttribute('data-testid');
  if (testId) {
    return `[data-testid="${testId}"]`;
  }

  if (el.className) {
    const classes = el.className.toString().split(' ').filter(c => c.length > 0);
    if (classes.length > 0) {
      return `${tag}.${classes.join('.')}`;
    }
  }

  return `${tag}:nth-of-type(${index + 1})`;
}
