//! Language-agnostic contract: id -> data-id migration in the same list cluster.

use frap_core::{FlettaCore, HealRequest, HealResult};
use serde::Deserialize;
use std::fs;
use std::path::PathBuf;

#[derive(Debug, Deserialize)]
struct ContractExpected {
    healed: bool,
    min_confidence: f64,
    best_candidate_attribute: String,
    best_candidate_value: String,
}

fn fixture_path(name: &str) -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR"))
        .join("../../fixtures/contract/clustering-id-migration")
        .join(name)
}

#[test]
fn contract_clustering_id_to_data_id_migration() {
    let request_json = fs::read_to_string(fixture_path("request.json")).expect("read request");
    let expected_json = fs::read_to_string(fixture_path("expected.json")).expect("read expected");
    let expected: ContractExpected = serde_json::from_str(&expected_json).expect("parse expected");

    let request: HealRequest = serde_json::from_str(&request_json).expect("parse request");
    let mut core = FlettaCore::new();
    let result: HealResult = core.heal_request(&request);

    assert_eq!(
        result.healed, expected.healed,
        "healed mismatch: {:?}",
        result
    );
    assert!(
        result.confidence >= expected.min_confidence,
        "confidence {} < {}",
        result.confidence,
        expected.min_confidence
    );
    assert!(
        !result.top_candidates.is_empty(),
        "expected ranked candidates"
    );

    let best = &result.top_candidates[0];
    assert!(
        best.selector.contains(&format!(
            "{}=\"{}\"",
            expected.best_candidate_attribute, expected.best_candidate_value
        )),
        "best selector {} should reference {}={}",
        best.selector,
        expected.best_candidate_attribute,
        expected.best_candidate_value
    );

    if result.healed {
        assert!(
            result.selector.contains(&format!(
                "{}=\"{}\"",
                expected.best_candidate_attribute, expected.best_candidate_value
            )),
            "healed selector {} should reference migrated attribute",
            result.selector
        );
    }
}
