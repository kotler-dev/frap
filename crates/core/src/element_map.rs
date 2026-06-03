//! Element map discovery: cluster DOM snapshot elements for Page Object generation.

use crate::CoreError;
use clustering::DOMElementClusterer;
use healing::{DOMElementInfo, DOMSnapshot, HealingEngine};
use serde::{Deserialize, Deserializer, Serialize};
use signature::Signature;
use std::collections::{HashMap, HashSet};

const INTERACTIVE_TAGS: &[&str] = &["button", "a", "input", "select", "textarea"];

fn deserialize_alternatives<'de, D>(deserializer: D) -> Result<Vec<LocatorRecommendation>, D::Error>
where
    D: Deserializer<'de>,
{
    let opt = Option::<Vec<LocatorRecommendation>>::deserialize(deserializer)?;
    Ok(opt.unwrap_or_default())
}

#[derive(Debug, Clone, Serialize, Deserialize, Default, PartialEq, Eq)]
#[serde(rename_all = "snake_case")]
pub enum CoverageMode {
    #[default]
    Actionable,
    Semantic,
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct MapOptions {
    #[serde(default)]
    pub url: Option<String>,
    #[serde(default)]
    pub include_non_interactive: bool,
    #[serde(default)]
    pub max_elements: Option<usize>,
    #[serde(default)]
    pub coverage_mode: CoverageMode,
}

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct FilterSpec {
    #[serde(default)]
    pub interactive_only: bool,
    #[serde(default)]
    pub min_cluster_size: Option<usize>,
    #[serde(default)]
    pub tags: Option<Vec<String>>,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct LocatorRecommendation {
    pub selector: String,
    pub strategy: String,
    /// Reliability of the recommended **selector** — how trustworthy the chosen
    /// locator is, driven by `strategy` (see [`strategy_score`]).
    /// Distinct from [`ElementNode::confidence`]; both rank a `data-testid`
    /// highest but live on different scales.
    pub confidence: f64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ElementNode {
    pub id: String,
    pub selector: String,
    pub recommended_selector: String,
    pub tag: String,
    pub signature: Signature,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cluster_id: Option<String>,
    /// Structural confidence of the element **signature** (used by healing and
    /// clustering), from [`stability_confidence`]. This is NOT the selector
    /// reliability — that is [`LocatorRecommendation::confidence`]. Both are
    /// monotonic by quality (`data-testid` ≥ `id` ≥ `data-id` ≥ text ≥ none).
    pub confidence: f64,
    pub locator: LocatorRecommendation,
    /// True when the recommended locator is weak (structural or low selector confidence).
    #[serde(default)]
    pub fragile: bool,
    #[serde(
        default,
        skip_serializing_if = "Vec::is_empty",
        deserialize_with = "deserialize_alternatives"
    )]
    pub alternatives: Vec<LocatorRecommendation>,
    /// Accessible name from snapshot (label[for], aria-labelledby, sibling label, …).
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub accessible_name: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "snake_case")]
pub enum ClusterType {
    Single,
    List,
    Unknown,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Cluster {
    pub id: String,
    pub cluster_type: ClusterType,
    pub element_ids: Vec<String>,
    pub prefix_signature: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MapMetadata {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
    pub element_count: usize,
    pub cluster_count: usize,
    pub timestamp_ms: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ElementMap {
    pub elements: Vec<ElementNode>,
    pub clusters: Vec<Cluster>,
    pub metadata: MapMetadata,
}

pub fn recommend_locator(element: &DOMElementInfo) -> LocatorRecommendation {
    locator_from_pick(pick_locator(element))
}

fn recommend_locator_with_click_root(
    element: &DOMElementInfo,
    snapshot: &[DOMElementInfo],
) -> LocatorRecommendation {
    let own = recommend_locator(element);
    let Some(root) = find_click_root_ancestor(snapshot, element) else {
        return own;
    };
    let root_loc = recommend_locator(root);
    if click_area_score(&root_loc) > click_area_score(&own) {
        root_loc
    } else {
        own
    }
}

fn locator_from_pick((strategy, selector): (&'static str, String)) -> LocatorRecommendation {
    LocatorRecommendation {
        selector,
        strategy: strategy.to_string(),
        confidence: strategy_score(strategy),
    }
}

/// Prefer locators that hit the full clickable region (container id/href over inner role/text).
fn click_area_score(loc: &LocatorRecommendation) -> i32 {
    match loc.strategy.as_str() {
        "data-testid" => 50,
        "id" => 45,
        "data-id" => 40,
        "name" => 35,
        "role" => 25,
        "aria-label" => 20,
        "text" => 15,
        _ => 0,
    }
}

fn path_is_strict_ancestor_prefix(ancestor: &[String], descendant: &[String]) -> bool {
    descendant.len() > ancestor.len() && descendant.starts_with(ancestor)
}

/// Groups nested controls that share the same href or explicit accessible name.
fn click_target_key(element: &DOMElementInfo) -> Option<String> {
    if let Some(href) = element.attributes.get("href") {
        let h = href.trim();
        if !h.is_empty() && h != "#" && !h.starts_with("javascript:") {
            return Some(format!("href:{h}"));
        }
    }
    let name = element.accessible_name.as_deref()?.trim();
    if name.is_empty() {
        return None;
    }
    // Same caption on wrapper div and inner role=button must share one click target.
    Some(format!("name:{name}"))
}

fn is_clickable_surface(element: &DOMElementInfo) -> bool {
    is_interactive(element)
        || (element.attributes.contains_key("id")
            && element
                .accessible_name
                .as_ref()
                .is_some_and(|n| !n.trim().is_empty()))
}

fn find_click_root_ancestor<'a>(
    snapshot: &'a [DOMElementInfo],
    element: &DOMElementInfo,
) -> Option<&'a DOMElementInfo> {
    let key = click_target_key(element)?;
    let mut best: Option<(&'a DOMElementInfo, usize)> = None;
    for candidate in snapshot {
        if click_target_key(candidate).as_deref() != Some(key.as_str()) {
            continue;
        }
        if !path_is_strict_ancestor_prefix(&candidate.path, &element.path) {
            continue;
        }
        if !is_clickable_surface(candidate) {
            continue;
        }
        let depth = candidate.path.len();
        if best.map(|(_, d)| depth < d).unwrap_or(true) {
            best = Some((candidate, depth));
        }
    }
    best.map(|(c, _)| c)
}

/// After per-element recommendations, align nested duplicates to the shallowest click root.
fn promote_click_roots(snapshot: &[DOMElementInfo], nodes: &mut [ElementNode]) {
    for node in nodes.iter_mut() {
        let Some(el) = snapshot.iter().find(|e| e.selector == node.selector) else {
            continue;
        };
        let Some(root) = find_click_root_ancestor(snapshot, el) else {
            continue;
        };
        if root.selector == el.selector {
            continue;
        }
        let root_loc = recommend_locator(root);
        if click_area_score(&root_loc) <= click_area_score(&node.locator) {
            continue;
        }
        let prev = node.locator.clone();
        if !node
            .alternatives
            .iter()
            .any(|a| a.selector == prev.selector)
        {
            node.alternatives.insert(0, prev);
            node.alternatives.truncate(3);
        }
        node.locator = root_loc.clone();
        node.recommended_selector = root_loc.selector.clone();
        node.fragile = is_fragile_locator(&root_loc.strategy, root_loc.confidence);
    }
}

fn is_fragile_locator(strategy: &str, confidence: f64) -> bool {
    strategy == "structural" || confidence < 0.55
}

fn collect_alternatives(
    element: &DOMElementInfo,
    primary: &LocatorRecommendation,
) -> Vec<LocatorRecommendation> {
    let mut alts = Vec::new();
    let candidates = [
        pick_locator(element),
        pick_with_role_name(element),
        pick_text_only(element),
    ];
    for (strategy, selector) in candidates {
        let rec = LocatorRecommendation {
            selector: selector.clone(),
            strategy: strategy.to_string(),
            confidence: strategy_score(strategy),
        };
        if rec.selector != primary.selector
            && !alts
                .iter()
                .any(|a: &LocatorRecommendation| a.selector == rec.selector)
        {
            alts.push(rec);
        }
    }
    alts.truncate(3);
    alts
}

fn pick_with_role_name(element: &DOMElementInfo) -> (&'static str, String) {
    if let Some(role) = element.attributes.get("role") {
        if let Some(name) = accessible_name(element) {
            return (
                "role",
                format!("role={}[name=\"{}\"]", role, escape_css_attr(&name)),
            );
        }
    }
    ("structural", structural_selector(element))
}

fn pick_text_only(element: &DOMElementInfo) -> (&'static str, String) {
    if let Some(text) = element.text_content.as_deref().and_then(usable_text) {
        return (
            "text",
            format!("{}:has-text(\"{}\")", element.tag, escape_css_attr(&text)),
        );
    }
    ("structural", structural_selector(element))
}

fn accessible_name(element: &DOMElementInfo) -> Option<String> {
    if let Some(name) = element.accessible_name.as_ref() {
        let t = name.trim();
        if !t.is_empty() {
            return Some(t.to_string());
        }
    }
    if let Some(label) = element.attributes.get("aria-label") {
        let t = label.trim();
        if !t.is_empty() {
            return Some(t.to_string());
        }
    }
    element.text_content.as_deref().and_then(usable_text)
}

pub fn build_element_map(snapshot: &DOMSnapshot, options: &MapOptions) -> ElementMap {
    let mut clusterer = DOMElementClusterer::new();
    let mut cluster_members: HashMap<String, Vec<String>> = HashMap::new();
    let mut cluster_prefix: HashMap<String, String> = HashMap::new();
    let mut elements = Vec::new();

    let limit = options.max_elements.unwrap_or(snapshot.elements.len());

    for (idx, element) in snapshot.elements.iter().take(limit).enumerate() {
        if !should_include_element(element, options) {
            continue;
        }

        let signature = HealingEngine::signature_from_element(element);
        let cluster_id = clusterer.add_element(element.selector.clone(), signature.clone());
        let element_id = format!("el-{idx}");
        cluster_prefix
            .entry(cluster_id.clone())
            .or_insert_with(|| signature.prefix.clone());
        cluster_members
            .entry(cluster_id.clone())
            .or_default()
            .push(element_id.clone());

        let locator = recommend_locator_with_click_root(element, snapshot.elements.as_slice());
        let confidence = stability_confidence(&signature);
        let fragile = is_fragile_locator(&locator.strategy, locator.confidence);
        let alternatives = collect_alternatives(element, &locator);

        elements.push(ElementNode {
            id: element_id,
            selector: element.selector.clone(),
            recommended_selector: locator.selector.clone(),
            tag: element.tag.clone(),
            signature,
            cluster_id: Some(cluster_id),
            confidence,
            locator: locator.clone(),
            fragile,
            alternatives,
            accessible_name: element.accessible_name.clone(),
        });
    }

    promote_click_roots(snapshot.elements.as_slice(), &mut elements);

    let mut clusters: Vec<Cluster> = cluster_members
        .into_iter()
        .map(|(id, element_ids)| {
            let cluster_type = if element_ids.len() >= 2 {
                ClusterType::List
            } else {
                ClusterType::Single
            };
            Cluster {
                id: id.clone(),
                cluster_type,
                element_ids,
                prefix_signature: cluster_prefix.get(&id).cloned().unwrap_or_default(),
            }
        })
        .collect();

    demote_heterogeneous_lists(&mut clusters, &elements);

    let timestamp_ms = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .map(|d| d.as_millis() as u64)
        .unwrap_or(0);

    ElementMap {
        elements,
        clusters,
        metadata: MapMetadata {
            url: options.url.clone(),
            element_count: 0,
            cluster_count: 0,
            timestamp_ms,
        },
    }
    .with_metadata_counts()
}

/// Minimum LIST size before demoting unique-id clusters (issue #9 mega-LIST).
const MEGA_LIST_DEMOTE_THRESHOLD: usize = 8;

/// Downgrades large LIST clusters that are not repeated components (issue #9):
/// many unique id-based locators under the same path prefix are unrelated controls.
fn demote_heterogeneous_lists(clusters: &mut [Cluster], elements: &[ElementNode]) {
    for cluster in clusters.iter_mut() {
        if cluster.cluster_type != ClusterType::List || cluster.element_ids.len() < 2 {
            continue;
        }
        let members: Vec<&ElementNode> = cluster
            .element_ids
            .iter()
            .filter_map(|id| elements.iter().find(|e| &e.id == id))
            .collect();
        if members.len() < 2 {
            continue;
        }
        if is_repeated_component_cluster(&members) {
            continue;
        }
        let unique_selectors: std::collections::HashSet<_> = members
            .iter()
            .map(|e| e.recommended_selector.as_str())
            .collect();
        if unique_selectors.len() == members.len() && members.len() >= MEGA_LIST_DEMOTE_THRESHOLD {
            cluster.cluster_type = ClusterType::Single;
        }
    }
}

fn is_repeated_component_cluster(members: &[&ElementNode]) -> bool {
    if members
        .iter()
        .all(|e| e.locator.strategy == "data-testid" || e.locator.strategy == "data-id")
    {
        return true;
    }
    let first = members[0].recommended_selector.as_str();
    members.iter().all(|e| e.recommended_selector == first)
}

impl ElementMap {
    fn with_metadata_counts(mut self) -> Self {
        self.metadata.element_count = self.elements.len();
        self.metadata.cluster_count = self.clusters.len();
        self
    }
}

pub fn filter_element_map(map: &ElementMap, spec: &FilterSpec) -> ElementMap {
    let allowed_tags: Option<HashSet<&str>> = spec
        .tags
        .as_ref()
        .map(|t| t.iter().map(|s| s.as_str()).collect());

    let mut kept_ids: HashSet<String> = HashSet::new();

    for element in &map.elements {
        if spec.interactive_only && !is_interactive_tag(&element.tag) {
            continue;
        }
        if let Some(tags) = &allowed_tags {
            if !tags.contains(element.tag.as_str()) {
                continue;
            }
        }
        if let Some(min_size) = spec.min_cluster_size {
            if let Some(cluster_id) = &element.cluster_id {
                let size = map
                    .clusters
                    .iter()
                    .find(|c| &c.id == cluster_id)
                    .map(|c| c.element_ids.len())
                    .unwrap_or(0);
                if size < min_size {
                    continue;
                }
            } else if min_size > 1 {
                continue;
            }
        }
        kept_ids.insert(element.id.clone());
    }

    let elements: Vec<ElementNode> = map
        .elements
        .iter()
        .filter(|e| kept_ids.contains(&e.id))
        .cloned()
        .collect();

    let clusters: Vec<Cluster> = map
        .clusters
        .iter()
        .filter_map(|c| {
            let element_ids: Vec<String> = c
                .element_ids
                .iter()
                .filter(|id| kept_ids.contains(*id))
                .cloned()
                .collect();
            if element_ids.is_empty() {
                None
            } else {
                Some(Cluster {
                    id: c.id.clone(),
                    cluster_type: if element_ids.len() >= 2 {
                        ClusterType::List
                    } else {
                        ClusterType::Single
                    },
                    element_ids,
                    prefix_signature: c.prefix_signature.clone(),
                })
            }
        })
        .collect();

    ElementMap {
        metadata: MapMetadata {
            url: map.metadata.url.clone(),
            element_count: elements.len(),
            cluster_count: clusters.len(),
            timestamp_ms: map.metadata.timestamp_ms,
        },
        elements,
        clusters,
    }
}

pub fn build_element_map_json(
    snapshot_json: &str,
    options_json: &str,
) -> Result<String, CoreError> {
    let snapshot: DOMSnapshot = serde_json::from_str(snapshot_json)?;
    let options: MapOptions = if options_json.trim().is_empty() {
        MapOptions::default()
    } else {
        serde_json::from_str(options_json)?
    };
    let map = build_element_map(&snapshot, &options);
    Ok(serde_json::to_string(&map)?)
}

pub fn filter_element_map_json(map_json: &str, spec_json: &str) -> Result<String, CoreError> {
    let map: ElementMap = serde_json::from_str(map_json)?;
    let spec: FilterSpec = serde_json::from_str(spec_json)?;
    let filtered = filter_element_map(&map, &spec);
    Ok(serde_json::to_string(&filtered)?)
}

fn should_include_element(element: &DOMElementInfo, options: &MapOptions) -> bool {
    if options.include_non_interactive {
        return true;
    }
    match options.coverage_mode {
        CoverageMode::Actionable => is_interactive(element),
        CoverageMode::Semantic => {
            is_interactive(element)
                || element.attributes.contains_key("data-testid")
                || element.attributes.contains_key("data-id")
                || element.attributes.contains_key("role")
                || element
                    .attributes
                    .get("contenteditable")
                    .is_some_and(|v| v == "true" || v.is_empty())
        }
    }
}

fn is_interactive(element: &DOMElementInfo) -> bool {
    is_interactive_tag(&element.tag)
        || element.attributes.get("role").is_some_and(|r| {
            matches!(
                r.as_str(),
                "button" | "link" | "textbox" | "checkbox" | "radio" | "combobox"
            )
        })
}

fn is_interactive_tag(tag: &str) -> bool {
    INTERACTIVE_TAGS.contains(&tag.to_lowercase().as_str())
}

/// Picks the most stable locator for an element and returns `(strategy, selector)`.
///
/// The returned selector is always a valid Playwright selector. In particular it
/// never produces the jQuery/Sizzle `:contains()` pseudo-class, which Playwright
/// does not support. When no stable attribute is available it falls back to
/// `aria-label`, then a valid `:has-text(...)` text selector (only for clean,
/// static labels), and finally a structural selector.
fn pick_locator(element: &DOMElementInfo) -> (&'static str, String) {
    if let Some(testid) = element.attributes.get("data-testid") {
        return (
            "data-testid",
            format!("[data-testid=\"{}\"]", escape_css_attr(testid)),
        );
    }
    if let Some(id) = element.attributes.get("id") {
        if !looks_generated_id(id) {
            return ("id", format!("#{}", escape_css_attr(id)));
        }
    }
    if let Some(data_id) = element.attributes.get("data-id") {
        return (
            "data-id",
            format!("[data-id=\"{}\"]", escape_css_attr(data_id)),
        );
    }
    if let Some(name) = element.attributes.get("name") {
        return ("name", format!("[name=\"{}\"]", escape_css_attr(name)));
    }
    let (role_strategy, role_sel) = pick_with_role_name(element);
    if role_strategy == "role" {
        return (role_strategy, role_sel);
    }
    if let Some(label) = element.attributes.get("aria-label") {
        let label = label.trim();
        if !label.is_empty() {
            return (
                "aria-label",
                format!("{}[aria-label=\"{}\"]", element.tag, escape_css_attr(label)),
            );
        }
    }
    if let Some(text) = element.text_content.as_deref().and_then(usable_text) {
        // Valid Playwright text pseudo-class — NOT the jQuery `:contains`.
        return (
            "text",
            format!("{}:has-text(\"{}\")", element.tag, escape_css_attr(&text)),
        );
    }
    ("structural", structural_selector(element))
}

/// Returns a cleaned label if the visible text can be safely used inside a
/// locator, otherwise `None`. Rejects empty text, CSS/JS leaked from
/// `<style>`/`<script>`, and dynamic/personal values such as amounts.
fn usable_text(raw: &str) -> Option<String> {
    let text = raw.split_whitespace().collect::<Vec<_>>().join(" ");
    if text.is_empty() || text.chars().count() > 40 {
        return None;
    }
    if looks_like_css(&text) || looks_dynamic(&text) {
        return None;
    }
    Some(text)
}

/// Heuristic for CSS/JS text that leaked from inline `<style>`/`<script>`.
fn looks_like_css(text: &str) -> bool {
    (text.contains('{') && text.contains(':'))
        || text.contains("clip-path")
        || text.contains("fill-opacity")
}

/// Heuristic for dynamic/per-user values (amounts, currency, number-dominant
/// strings) that should not be hard-coded into a locator.
fn looks_dynamic(text: &str) -> bool {
    if text.chars().any(|c| matches!(c, '₽' | '$' | '€' | '%')) {
        return true;
    }
    let digits = text.chars().filter(|c| c.is_ascii_digit()).count();
    let letters = text.chars().filter(|c| c.is_alphabetic()).count();
    digits > 0 && digits >= letters
}

/// Last-resort valid CSS selector. Not necessarily unique; the low confidence
/// and the `structural` strategy tell the consumer it is weak.
fn structural_selector(element: &DOMElementInfo) -> String {
    match element.position_in_parent {
        Some(pos) => format!("{}:nth-of-type({})", element.tag, pos + 1),
        None => element.tag.clone(),
    }
}

fn looks_generated_id(id: &str) -> bool {
    signature::looks_like_generated(id)
}

fn escape_css_attr(value: &str) -> String {
    value.replace('\\', "\\\\").replace('"', "\\\"")
}

/// Reliability of the recommended **selector**, by locator strategy.
/// Monotonic: `data-testid` > `id` > `data-id` > `name` > `aria-label` > `text` > structural.
fn strategy_score(strategy: &str) -> f64 {
    match strategy {
        "data-testid" => 0.95,
        "id" => 0.85,
        "data-id" => 0.8,
        "name" => 0.7,
        "role" => 0.72,
        "aria-label" => 0.65,
        "text" => 0.55,
        _ => 0.4,
    }
}

/// Structural confidence of the element **signature** (for healing/clustering).
/// Monotonic by quality so it never inverts against [`strategy_score`]:
/// `data-testid` > `id` > `data-id` > text-only > none.
fn stability_confidence(signature: &Signature) -> f64 {
    let mut score: f64 = 0.5;
    if signature.stable_attrs.contains_key("data-testid") {
        score += 0.3;
    }
    if signature.stable_attrs.contains_key("id") {
        score += 0.15;
    }
    // Previously omitted, which made a data-id element tie with a no-signal one.
    if signature.stable_attrs.contains_key("data-id") {
        score += 0.1;
    }
    if signature
        .text_content
        .as_ref()
        .is_some_and(|t| !t.is_empty())
    {
        score += 0.05;
    }
    score.min(1.0)
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::collections::HashMap;

    fn card_snapshot() -> DOMSnapshot {
        DOMSnapshot {
            html: "<div class=\"grid\">...</div>".to_string(),
            elements: vec![
                DOMElementInfo {
                    selector: "[data-testid=\"card-1\"]".to_string(),
                    tag: "article".to_string(),
                    attributes: [("data-testid".to_string(), "card-1".to_string())].into(),
                    text_content: Some("Card 1".to_string()),
                    accessible_name: None,
                    path: vec!["div:-".to_string(), "article:-".to_string()],
                    position_in_parent: Some(0),
                },
                DOMElementInfo {
                    selector: "[data-testid=\"card-2\"]".to_string(),
                    tag: "article".to_string(),
                    attributes: [("data-testid".to_string(), "card-2".to_string())].into(),
                    text_content: Some("Card 2".to_string()),
                    accessible_name: None,
                    path: vec!["div:-".to_string(), "article:-".to_string()],
                    position_in_parent: Some(1),
                },
                DOMElementInfo {
                    selector: "button.buy".to_string(),
                    tag: "button".to_string(),
                    attributes: HashMap::new(),
                    text_content: Some("Buy".to_string()),
                    accessible_name: None,
                    path: vec!["button:-".to_string()],
                    position_in_parent: None,
                },
            ],
        }
    }

    #[test]
    fn build_element_map_groups_similar_paths() {
        let map = build_element_map(
            &card_snapshot(),
            &MapOptions {
                include_non_interactive: true,
                ..Default::default()
            },
        );
        assert_eq!(map.elements.len(), 3);
        let list_clusters: Vec<_> = map
            .clusters
            .iter()
            .filter(|c| c.cluster_type == ClusterType::List)
            .collect();
        assert!(
            !list_clusters.is_empty(),
            "expected at least one list cluster for similar articles"
        );
    }

    #[test]
    fn filter_interactive_only() {
        let map = build_element_map(&card_snapshot(), &MapOptions::default());
        let filtered = filter_element_map(
            &map,
            &FilterSpec {
                interactive_only: true,
                ..Default::default()
            },
        );
        assert_eq!(filtered.elements.len(), 1);
        assert_eq!(filtered.elements[0].tag, "button");
    }

    fn el(tag: &str, attrs: &[(&str, &str)], text: Option<&str>) -> DOMElementInfo {
        DOMElementInfo {
            selector: format!("{tag}:contains(\"junk\")"), // simulate adapter passthrough
            tag: tag.to_string(),
            attributes: attrs
                .iter()
                .map(|(k, v)| (k.to_string(), v.to_string()))
                .collect(),
            text_content: text.map(|t| t.to_string()),
            accessible_name: None,
            path: vec![format!("{tag}:-")],
            position_in_parent: Some(0),
        }
    }

    // issue #01: recommended selector must never contain the jQuery `:contains`.
    #[test]
    fn never_emits_contains_pseudo_class() {
        let cases = [
            el("button", &[], None),                       // no signal
            el("a", &[], Some("Search")),                  // text only
            el("button", &[], Some("")),                   // empty text
            el("a", &[("aria-label", "Open menu")], None), // aria-label
        ];
        for e in cases {
            let rec = recommend_locator(&e);
            assert!(
                !rec.selector.contains(":contains("),
                "selector still uses :contains -> {}",
                rec.selector
            );
        }
    }

    // issue #01: plain text falls back to a valid Playwright `:has-text`.
    #[test]
    fn text_fallback_uses_has_text() {
        let rec = recommend_locator(&el("a", &[], Some("Pay by QR")));
        assert_eq!(rec.strategy, "text");
        assert_eq!(rec.selector, "a:has-text(\"Pay by QR\")");
    }

    #[test]
    fn accessible_name_from_label_for() {
        let el = DOMElementInfo {
            selector: "[id=\"calc-btn\"]".to_string(),
            tag: "div".to_string(),
            attributes: [("role".to_string(), "button".to_string())].into(),
            text_content: Some("".to_string()),
            accessible_name: Some("Калькулятор процентов".to_string()),
            path: vec![],
            position_in_parent: None,
        };
        let rec = recommend_locator(&el);
        assert_eq!(rec.strategy, "role");
        assert!(rec.selector.contains("Калькулятор процентов"));
    }

    #[test]
    fn nested_click_target_promotes_container_id() {
        let snapshot = DOMSnapshot {
            html: String::new(),
            elements: vec![
                DOMElementInfo {
                    selector: "div[id=\"calc-btn\"]".to_string(),
                    tag: "div".to_string(),
                    attributes: [("id".to_string(), "calc-btn".to_string())].into(),
                    text_content: None,
                    accessible_name: Some("Калькулятор процентов".to_string()),
                    path: vec!["section:-".into(), "div:-".into()],
                    position_in_parent: Some(0),
                },
                DOMElementInfo {
                    selector: "div".to_string(),
                    tag: "div".to_string(),
                    attributes: [("role".to_string(), "button".to_string())].into(),
                    text_content: None,
                    accessible_name: Some("Калькулятор процентов".to_string()),
                    path: vec!["section:-".into(), "div:-".into(), "div:-".into()],
                    position_in_parent: Some(0),
                },
            ],
        };
        let map = build_element_map(
            &snapshot,
            &MapOptions {
                coverage_mode: CoverageMode::Semantic,
                ..Default::default()
            },
        );
        let inner = map
            .elements
            .iter()
            .find(|e| e.selector == "div")
            .expect("inner role button");
        assert_eq!(inner.recommended_selector, "#calc-btn");
        assert_eq!(inner.locator.strategy, "id");
    }

    #[test]
    fn accessible_name_from_sibling_label() {
        let el = DOMElementInfo {
            selector: "div[role=\"button\"]".to_string(),
            tag: "div".to_string(),
            attributes: [("role".to_string(), "button".to_string())].into(),
            text_content: None,
            accessible_name: Some("Калькулятор процентов".to_string()),
            path: vec![],
            position_in_parent: None,
        };
        let rec = recommend_locator(&el);
        assert_eq!(rec.strategy, "role");
        assert!(rec
            .selector
            .contains("role=button[name=\"Калькулятор процентов\"]"));
    }

    // issue #01/#07: aria-label is preferred over visible text.
    #[test]
    fn aria_label_preferred_over_text() {
        let rec = recommend_locator(&el("button", &[("aria-label", "Log out")], Some("X")));
        assert_eq!(rec.strategy, "aria-label");
        assert_eq!(rec.selector, "button[aria-label=\"Log out\"]");
    }

    // issue #06: empty text must not become `:has-text("")`; it goes structural.
    #[test]
    fn empty_text_is_not_locatable_by_text() {
        let rec = recommend_locator(&el("button", &[], Some("   ")));
        assert_eq!(rec.strategy, "structural");
        assert!(!rec.selector.contains("has-text"));
        assert!(rec.confidence <= 0.5);
    }

    // issue #08: CSS leaked from <style> must not be used as a text locator.
    #[test]
    fn css_text_is_rejected() {
        let rec = recommend_locator(&el("a", &[], Some(".B{clip-path:url(#C)}.C{fill:#000}")));
        assert_eq!(rec.strategy, "structural");
        assert!(!rec.selector.contains("has-text"));
    }

    // issue #07: dynamic/amount-like text must not be hard-coded into a locator.
    #[test]
    fn dynamic_amount_text_is_rejected() {
        for amount in ["12 345 ₽", "1000", "−500 руб"] {
            let rec = recommend_locator(&el("span", &[], Some(amount)));
            assert_eq!(rec.strategy, "structural", "value leaked: {amount}");
        }
    }

    // stable attributes keep their priority and exact selector form.
    #[test]
    fn stable_attributes_keep_priority() {
        let rec = recommend_locator(&el("button", &[("data-testid", "pay")], Some("Pay")));
        assert_eq!(rec.strategy, "data-testid");
        assert_eq!(rec.selector, "[data-testid=\"pay\"]");
        assert_eq!(rec.confidence, 0.95);
    }

    // issue #15: the node confidence and the locator confidence are two distinct
    // scales, but neither may invert against element quality, and a data-id must
    // not tie with a no-signal element.
    #[test]
    fn confidence_scales_are_monotonic_and_consistent() {
        let mk = |attrs: &[(&str, &str)], text: Option<&str>| {
            let snap = DOMSnapshot {
                html: String::new(),
                elements: vec![DOMElementInfo {
                    selector: "x".into(),
                    tag: "button".into(),
                    attributes: attrs
                        .iter()
                        .map(|(k, v)| (k.to_string(), v.to_string()))
                        .collect(),
                    text_content: text.map(|t| t.to_string()),
                    accessible_name: None,
                    path: vec!["button:-".into()],
                    position_in_parent: None,
                }],
            };
            let m = build_element_map(&snap, &MapOptions::default());
            let e = &m.elements[0];
            (e.confidence, e.locator.confidence)
        };

        let testid = mk(&[("data-testid", "x")], None);
        let id = mk(&[("id", "x")], None);
        let data_id = mk(&[("data-id", "x")], None);
        let text = mk(&[], Some("Buy"));
        let none = mk(&[], None);

        assert!(testid.0 > id.0 && id.0 > data_id.0 && data_id.0 > text.0 && text.0 > none.0);
        assert!(data_id.0 > none.0);
        assert!(testid.1 >= id.1 && id.1 >= data_id.1 && data_id.1 >= none.1);
        assert!(testid.0 >= id.0 && testid.1 >= id.1);
        assert!(id.0 >= data_id.0 && id.1 >= data_id.1);
    }

    // issue #09: unrelated controls sharing a path prefix must not stay one mega-LIST.
    #[test]
    fn heterogeneous_unique_id_links_are_not_one_list() {
        let deep_path = vec![
            "div:-".into(),
            "div:-".into(),
            "main:-".into(),
            "div:-".into(),
            "div:-".into(),
            "a:-".into(),
        ];
        let mut elements = Vec::new();
        for i in 0..10 {
            elements.push(DOMElementInfo {
                selector: format!("a#nav-{i}"),
                tag: "a".into(),
                attributes: [("id".to_string(), format!("nav-{i}"))].into(),
                text_content: Some(format!("Link {i}")),
                accessible_name: None,
                path: deep_path.clone(),
                position_in_parent: Some(i),
            });
        }
        let map = build_element_map(
            &DOMSnapshot {
                html: String::new(),
                elements,
            },
            &MapOptions::default(),
        );
        let mega_lists: Vec<_> = map
            .clusters
            .iter()
            .filter(|c| {
                c.cluster_type == ClusterType::List
                    && c.element_ids.len() >= MEGA_LIST_DEMOTE_THRESHOLD
            })
            .collect();
        assert!(
            mega_lists.is_empty(),
            "expected no mega-LIST for unique id links, got {:?}",
            map.clusters
        );
    }

    #[test]
    fn repeated_data_testid_cards_stay_list_cluster() {
        let map = build_element_map(
            &card_snapshot(),
            &MapOptions {
                include_non_interactive: true,
                ..Default::default()
            },
        );
        let list_clusters: Vec<_> = map
            .clusters
            .iter()
            .filter(|c| c.cluster_type == ClusterType::List)
            .collect();
        assert!(
            !list_clusters.is_empty(),
            "article cards with data-testid should remain LIST clusters"
        );
    }
}
