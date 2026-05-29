//! WASM bindings for Node.js / bundlers (wasm-pack, target `bundler`).

use wasm_bindgen::prelude::*;

use crate::{analyze_rca_json, FrapCore};
use frap_rca::DEFAULT_WINDOW_MS;

/// Run heal from JSON [`HealRequest`], return JSON [`HealResult`].
#[wasm_bindgen(js_name = healJson)]
pub fn heal_json(input: &str) -> Result<String, JsValue> {
    let mut core = FrapCore::new();
    core.heal_json(input)
        .map_err(|e| JsValue::from_str(&e.to_string()))
}

/// Analyze unified context timeline JSON, return JSON [`RcaReport`].
/// Pass `failure_at_ms` <= 0 to auto-detect from UI failure events.
#[wasm_bindgen(js_name = analyzeRcaJson)]
pub fn analyze_rca_json_wasm(timeline_json: &str, failure_at_ms: i64) -> Result<String, JsValue> {
    analyze_rca_json(timeline_json, failure_at_ms, DEFAULT_WINDOW_MS)
        .map_err(|e| JsValue::from_str(&e.to_string()))
}
