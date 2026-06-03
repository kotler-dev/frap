//! Contract: dom-benchmark fixtures (F019 / C012).

use frap_core::{
    build_element_map, validate_dom_benchmark, DomBenchmarkExpected, DOMSnapshot, MapOptions,
};
use serde::Deserialize;
use std::fs;
use std::path::PathBuf;

const PAGES: &[&str] = &[
    "01-catalog-list",
    "02-no-testid-buttons",
    "03-duplicate-labels",
    "04-aria-only",
    "05-shadow-open",
    "06-contenteditable",
    "07-role-button",
    "08-generated-id",
    "09-sibling-label",
];

fn bench_root() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("../../fixtures/dom-benchmark")
}

#[derive(Debug, Deserialize)]
struct SnapshotFixture {
    dom_snapshot: DOMSnapshot,
    #[serde(default)]
    options: MapOptions,
}

#[test]
fn contract_dom_benchmark_all_pages() {
    for page in PAGES {
        contract_one_page(page);
    }
}

fn contract_one_page(page_id: &str) {
    let snapshot_path = bench_root()
        .join("snapshots")
        .join(format!("{page_id}.snapshot.json"));
    let expected_path = bench_root()
        .join("ground-truth")
        .join(format!("{page_id}.expected.json"));

    let snapshot_json = fs::read_to_string(&snapshot_path)
        .unwrap_or_else(|e| panic!("read {}: {e}", snapshot_path.display()));
    let expected_json = fs::read_to_string(&expected_path)
        .unwrap_or_else(|e| panic!("read {}: {e}", expected_path.display()));

    let fixture: SnapshotFixture =
        serde_json::from_str(&snapshot_json).expect("parse snapshot fixture");
    let expected: DomBenchmarkExpected =
        serde_json::from_str(&expected_json).expect("parse expected");

    let map = build_element_map(&fixture.dom_snapshot, &fixture.options);
    validate_dom_benchmark(&map, &expected)
        .unwrap_or_else(|e| panic!("page {page_id}: {e}"));
}
