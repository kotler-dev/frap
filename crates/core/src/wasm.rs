//! WASM bindings for Node.js / bundlers (wasm-pack, target `bundler`).

use wasm_bindgen::prelude::*;

use crate::FlettaCore;

/// Run heal from JSON [`HealRequest`], return JSON [`HealResult`].
#[wasm_bindgen(js_name = healJson)]
pub fn heal_json(input: &str) -> Result<String, JsValue> {
    let mut core = FlettaCore::new();
    core.heal_json(input)
        .map_err(|e| JsValue::from_str(&e.to_string()))
}
