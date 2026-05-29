//! Contract: element map discovers list clusters for repeated cards.

use frap_core::{build_element_map, ClusterType, DOMSnapshot, MapOptions};
use serde::Deserialize;
use std::fs;
use std::path::PathBuf;

#[derive(Debug, Deserialize)]
struct ContractExpected {
    min_elements: usize,
    min_list_clusters: usize,
    min_cluster_size: usize,
}

fn fixture_path(name: &str) -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR"))
        .join("../../fixtures/contract/element-map-list")
        .join(name)
}

#[derive(Debug, Deserialize)]
struct FixtureRequest {
    dom_snapshot: DOMSnapshot,
    options: MapOptions,
}

#[test]
fn contract_element_map_list_clustering() {
    let request_json = fs::read_to_string(fixture_path("request.json")).expect("read request");
    let expected_json = fs::read_to_string(fixture_path("expected.json")).expect("read expected");
    let expected: ContractExpected = serde_json::from_str(&expected_json).expect("parse expected");
    let request: FixtureRequest = serde_json::from_str(&request_json).expect("parse request");

    let map = build_element_map(&request.dom_snapshot, &request.options);

    assert!(
        map.elements.len() >= expected.min_elements,
        "elements {}",
        map.elements.len()
    );

    let list_clusters: Vec<_> = map
        .clusters
        .iter()
        .filter(|c| c.cluster_type == ClusterType::List)
        .filter(|c| c.element_ids.len() >= expected.min_cluster_size)
        .collect();

    assert!(
        list_clusters.len() >= expected.min_list_clusters,
        "list clusters: {:?}",
        map.clusters
    );
}
