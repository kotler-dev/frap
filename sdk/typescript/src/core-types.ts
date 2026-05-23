import type { HealingSemantics } from './healing-semantics';

export interface Signature {
  path: DOMToken[];
  prefix: string;
  stable_attrs: Record<string, string>;
  text_content?: string;
  position_in_parent?: number;
  children_hash: number;
  depth: number;
}

export interface DOMToken {
  tag: string;
  role?: string;
  semantic_type?: string;
  structural_class?: string;
  depth: number;
}

export interface Candidate {
  selector: string;
  signature: Signature;
  confidence: number;
}

export interface HealResult {
  healed: boolean;
  selector: string;
  confidence: number;
  diff?: string;
  top_candidates: Candidate[];
  original_signature: Signature;
  semantics?: HealingSemantics;
}

export interface DOMElementInfo {
  selector: string;
  tag: string;
  attributes: Record<string, string>;
  text_content?: string;
  path: string[];
}

export interface DOMSnapshot {
  html: string;
  elements: DOMElementInfo[];
}

export interface WasmHealModule {
  healJson(input: string): string;
}
