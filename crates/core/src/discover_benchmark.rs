//! Ground-truth validation for dom-benchmark fixtures (F019).

use crate::element_map::{ClusterType, ElementMap, ElementNode};
use serde::Deserialize;

fn is_fragile(element: &ElementNode) -> bool {
    element.locator.strategy == "css"
        && (element.recommended_selector.contains("nth-of-type")
            || element.recommended_selector.contains("nth-child")
            || element.locator.selector.contains("nth-of-type")
            || element.locator.selector.contains("nth-child"))
}

#[derive(Debug, Clone, Deserialize)]
pub struct DomBenchmarkExpected {
    pub page_id: String,
    pub min_elements: usize,
    #[serde(default)]
    pub min_list_clusters: usize,
    #[serde(default = "default_min_cluster_size")]
    pub min_cluster_size: usize,
    #[serde(default)]
    pub max_fragile_ratio: Option<f64>,
    #[serde(default)]
    pub min_primary_match_ratio: Option<f64>,
    #[serde(default)]
    pub expected_primary_selectors: Vec<String>,
}

fn default_min_cluster_size() -> usize {
    2
}

#[derive(Debug, Clone, Default)]
pub struct DomBenchmarkReport {
    pub element_count: usize,
    pub list_cluster_count: usize,
    pub fragile_count: usize,
    pub fragile_ratio: f64,
    pub primary_match_ratio: Option<f64>,
    pub missing_selectors: Vec<String>,
}

pub fn validate_dom_benchmark(
    map: &ElementMap,
    expected: &DomBenchmarkExpected,
) -> Result<DomBenchmarkReport, String> {
    let element_count = map.elements.len();
    if element_count < expected.min_elements {
        return Err(format!(
            "page {}: elements {} < min {}",
            expected.page_id, element_count, expected.min_elements
        ));
    }

    let list_clusters: Vec<_> = map
        .clusters
        .iter()
        .filter(|c| c.cluster_type == ClusterType::List)
        .filter(|c| c.element_ids.len() >= expected.min_cluster_size)
        .collect();

    if list_clusters.len() < expected.min_list_clusters {
        return Err(format!(
            "page {}: list clusters {} < min {}",
            expected.page_id,
            list_clusters.len(),
            expected.min_list_clusters
        ));
    }

    let fragile_count = map.elements.iter().filter(|e| is_fragile(e)).count();
    let fragile_ratio = if element_count == 0 {
        0.0
    } else {
        fragile_count as f64 / element_count as f64
    };

    if let Some(max_ratio) = expected.max_fragile_ratio {
        if fragile_ratio > max_ratio + f64::EPSILON {
            return Err(format!(
                "page {}: fragile ratio {:.2} > max {:.2}",
                expected.page_id, fragile_ratio, max_ratio
            ));
        }
    }

    let mut missing_selectors = Vec::new();
    let mut primary_match_ratio = None;

    if !expected.expected_primary_selectors.is_empty() {
        let mut matched = 0usize;
        for want in &expected.expected_primary_selectors {
            let found = map.elements.iter().any(|e| {
                e.recommended_selector == *want
                    || e.locator.selector == *want
                    || e.locator.value.as_deref() == Some(want.as_str())
            });
            if found {
                matched += 1;
            } else {
                missing_selectors.push(want.clone());
            }
        }
        let ratio = matched as f64 / expected.expected_primary_selectors.len() as f64;
        primary_match_ratio = Some(ratio);
        if let Some(min_ratio) = expected.min_primary_match_ratio {
            if ratio + f64::EPSILON < min_ratio {
                return Err(format!(
                    "page {}: primary match ratio {:.2} < min {:.2}; missing {:?}",
                    expected.page_id, ratio, min_ratio, missing_selectors
                ));
            }
        }
    }

    Ok(DomBenchmarkReport {
        element_count,
        list_cluster_count: list_clusters.len(),
        fragile_count,
        fragile_ratio,
        primary_match_ratio,
        missing_selectors,
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::element_map::{build_element_map, MapOptions};
    use crate::DOMElementInfo;
    #[test]
    fn validate_min_elements() {
        let snap = crate::DOMSnapshot {
            html: String::new(),
            elements: vec![DOMElementInfo {
                selector: "[data-testid=x]".into(),
                tag: "button".into(),
                attributes: [("data-testid".to_string(), "x".to_string())].into(),
                text_content: None,
                accessible_name: None,
                path: vec!["button:-".to_string()],
                position_in_parent: None,
                visible: None,
                computed_role: None,
                scope_hint: None,
            }],
        };
        let map = build_element_map(&snap, &MapOptions::default());
        let expected = DomBenchmarkExpected {
            page_id: "t".into(),
            min_elements: 2,
            min_list_clusters: 0,
            min_cluster_size: 2,
            max_fragile_ratio: None,
            min_primary_match_ratio: None,
            expected_primary_selectors: vec![],
        };
        assert!(validate_dom_benchmark(&map, &expected).is_err());
    }
}
