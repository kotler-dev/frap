use serde::{Deserialize, Serialize};
use std::collections::HashMap;

pub const DEFAULT_MIN_CONFIDENCE: f64 = 0.85;

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct DOMToken {
    pub tag: String,
    pub role: Option<String>,
    pub semantic_type: Option<String>,
    pub structural_class: Option<String>,
    pub depth: u8,
}

impl DOMToken {
    pub fn matches(&self, other: &DOMToken) -> bool {
        self.tag == other.tag && self.role == other.role
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Signature {
    pub path: Vec<DOMToken>,
    pub prefix: String,
    pub stable_attrs: HashMap<String, String>,
    pub text_content: Option<String>,
    pub position_in_parent: Option<usize>,
    pub children_hash: u64,
    pub depth: u8,
}

impl Signature {
    pub fn path_string(&self) -> String {
        self.path
            .iter()
            .map(|t| format!("{}:{}", t.tag, t.role.as_deref().unwrap_or("-")))
            .collect::<Vec<_>>()
            .join(">")
    }

    pub fn tokens(&self) -> &[DOMToken] {
        &self.path
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Candidate {
    pub selector: String,
    pub signature: Signature,
    pub confidence: f64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct HealResult {
    pub healed: bool,
    pub selector: String,
    pub confidence: f64,
    pub diff: Option<String>,
    pub top_candidates: Vec<Candidate>,
    pub original_signature: Signature,
}

impl HealResult {
    pub fn success(selector: String) -> Self {
        Self {
            healed: false,
            selector,
            confidence: 1.0,
            diff: None,
            top_candidates: vec![],
            original_signature: Signature {
                path: vec![],
                prefix: String::new(),
                stable_attrs: HashMap::new(),
                text_content: None,
                position_in_parent: None,
                children_hash: 0,
                depth: 0,
            },
        }
    }

    pub fn healed(selector: String, confidence: f64, candidates: Vec<Candidate>, original: Signature) -> Self {
        Self {
            healed: true,
            selector,
            confidence,
            diff: Some(format!("Healed with confidence {:.2}", confidence)),
            top_candidates: candidates.into_iter().take(3).collect(),
            original_signature: original,
        }
    }

    pub fn failed(candidates: Vec<Candidate>, original: Signature) -> Self {
        Self {
            healed: false,
            selector: String::new(),
            confidence: 0.0,
            diff: Some("No suitable candidate found".to_string()),
            top_candidates: candidates.into_iter().take(3).collect(),
            original_signature: original,
        }
    }
}

pub fn levenshtein_distance(a: &str, b: &str) -> usize {
    let len_a = a.chars().count();
    let len_b = b.chars().count();
    
    if len_a == 0 {
        return len_b;
    }
    if len_b == 0 {
        return len_a;
    }

    let mut matrix = vec![vec![0; len_b + 1]; len_a + 1];

    for i in 0..=len_a {
        matrix[i][0] = i;
    }
    for j in 0..=len_b {
        matrix[0][j] = j;
    }

    for (i, ca) in a.chars().enumerate() {
        for (j, cb) in b.chars().enumerate() {
            let cost = if ca == cb { 0 } else { 1 };
            matrix[i + 1][j + 1] = *[
                matrix[i][j + 1] + 1,
                matrix[i + 1][j] + 1,
                matrix[i][j] + cost,
            ]
            .iter()
            .min()
            .unwrap();
        }
    }

    matrix[len_a][len_b]
}

pub fn longest_common_subsequence_len<T: Eq>(a: &[T], b: &[T], matcher: impl Fn(&T, &T) -> bool) -> usize {
    let mut dp = vec![vec![0; b.len() + 1]; a.len() + 1];

    for i in 1..=a.len() {
        for j in 1..=b.len() {
            if matcher(&a[i - 1], &b[j - 1]) {
                dp[i][j] = dp[i - 1][j - 1] + 1;
            } else {
                dp[i][j] = dp[i - 1][j].max(dp[i][j - 1]);
            }
        }
    }

    dp[a.len()][b.len()]
}

pub fn calculate_path_similarity(a: &str, b: &str) -> f64 {
    let distance = levenshtein_distance(a, b);
    let max_len = a.len().max(b.len());
    if max_len == 0 {
        return 1.0;
    }
    1.0 - (distance as f64 / max_len as f64)
}

pub fn calculate_token_similarity(a: &[DOMToken], b: &[DOMToken]) -> f64 {
    if a.is_empty() && b.is_empty() {
        return 1.0;
    }
    let lcs_len = longest_common_subsequence_len(a, b, |t1, t2| t1.matches(t2));
    let max_len = a.len().max(b.len());
    lcs_len as f64 / max_len as f64
}

pub fn calculate_structural_similarity(a: u64, b: u64) -> f64 {
    if a == b {
        1.0
    } else if a == 0 || b == 0 {
        0.5
    } else {
        0.0
    }
}

pub fn calculate_attribute_bonus(original: &Signature, candidate: &Signature) -> f64 {
    let mut bonus = 0.0;

    if let (Some(orig_text), Some(cand_text)) = (&original.text_content, &candidate.text_content) {
        if orig_text == cand_text && !orig_text.is_empty() {
            bonus += 0.1;
        }
    }

    for (key, value) in &original.stable_attrs {
        if candidate.stable_attrs.get(key) == Some(value) {
            bonus += 0.05;
        }
    }

    if let (Some(orig_tid), Some(cand_tid)) = (
        original.stable_attrs.get("data-testid"),
        candidate.stable_attrs.get("data-testid"),
    ) {
        if orig_tid != cand_tid {
            let orig_parts: Vec<&str> = orig_tid.split(&['-', '_'][..]).filter(|p| p.len() > 2).collect();
            let cand_parts: Vec<&str> = cand_tid.split(&['-', '_'][..]).filter(|p| p.len() > 2).collect();
            if orig_parts.iter().any(|p| cand_parts.iter().any(|c| c.contains(p) || p.contains(c))) {
                bonus += 0.15;
            }
        }
    }

    bonus
}

pub fn calculate_confidence(original: &Signature, candidate: &Signature) -> f64 {
    let path_sim = calculate_path_similarity(&original.path_string(), &candidate.path_string());
    let token_sim = calculate_token_similarity(&original.path, &candidate.path);
    let structural_sim = calculate_structural_similarity(original.children_hash, candidate.children_hash);
    let bonus = calculate_attribute_bonus(original, candidate);

    let confidence = 0.5 * path_sim + 0.3 * token_sim + 0.2 * structural_sim + bonus;
    confidence.min(1.0)
}

pub fn extract_stable_attrs(attrs: &[(String, String)]) -> HashMap<String, String> {
    let stable_keys = ["role", "type", "placeholder", "aria-label", "aria-labelledby", "name"];
    let mut result = HashMap::new();

    for (key, value) in attrs {
        if stable_keys.contains(&key.as_str()) && !value.is_empty() {
            result.insert(key.clone(), value.clone());
        }
    }

    result
}

pub fn looks_like_generated(value: &str) -> bool {
    value.len() > 10 && (value.contains("-") || value.contains("_")) && 
    (value.chars().any(|c| c.is_ascii_hexdigit()) || value.contains("uuid") || value.contains("id"))
}

pub fn looks_like_semantic(value: &str) -> bool {
    let semantic_keywords = ["submit", "search", "login", "logout", "save", "cancel", "confirm", "pay", "checkout"];
    let lower = value.to_lowercase();
    semantic_keywords.iter().any(|kw| lower.contains(kw))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_levenshtein_distance() {
        assert_eq!(levenshtein_distance("kitten", "sitting"), 3);
        assert_eq!(levenshtein_distance("", ""), 0);
        assert_eq!(levenshtein_distance("a", ""), 1);
        assert_eq!(levenshtein_distance("", "a"), 1);
        assert_eq!(levenshtein_distance("abc", "abc"), 0);
    }

    #[test]
    fn test_path_similarity() {
        let sim = calculate_path_similarity("div>button>span", "div>button>span");
        assert_eq!(sim, 1.0);

        let sim = calculate_path_similarity("div>button", "div>input");
        assert!(sim < 1.0);
        assert!(sim > 0.0);
    }

    #[test]
    fn test_token_similarity() {
        let tokens_a = vec![
            DOMToken { tag: "div".to_string(), role: None, semantic_type: None, structural_class: None, depth: 0 },
            DOMToken { tag: "button".to_string(), role: Some("submit".to_string()), semantic_type: None, structural_class: None, depth: 1 },
        ];
        let tokens_b = vec![
            DOMToken { tag: "div".to_string(), role: None, semantic_type: None, structural_class: None, depth: 0 },
            DOMToken { tag: "button".to_string(), role: Some("submit".to_string()), semantic_type: None, structural_class: None, depth: 1 },
        ];
        let sim = calculate_token_similarity(&tokens_a, &tokens_b);
        assert_eq!(sim, 1.0);
    }

    #[test]
    fn test_lcs_similarity() {
        let a = vec![1, 2, 3, 4];
        let b = vec![2, 3, 5];
        let lcs = longest_common_subsequence_len(&a, &b, |x, y| x == y);
        assert_eq!(lcs, 2);
    }

    #[test]
    fn test_confidence_calculation() {
        let original = Signature {
            path: vec![
                DOMToken { tag: "div".to_string(), role: Some("form".to_string()), semantic_type: None, structural_class: None, depth: 0 },
                DOMToken { tag: "button".to_string(), role: Some("submit".to_string()), semantic_type: None, structural_class: None, depth: 1 },
            ],
            prefix: "div:form>button:submit".to_string(),
            stable_attrs: [("role".to_string(), "button".to_string())].into(),
            text_content: Some("Pay".to_string()),
            position_in_parent: Some(0),
            children_hash: 12345,
            depth: 2,
        };

        let candidate = original.clone();
        let confidence = calculate_confidence(&original, &candidate);
        assert!(confidence >= 0.9);
    }
}
