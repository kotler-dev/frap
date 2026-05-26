//! frap-core-rpc: NDJSON-RPC server for Frap Core.
//!
//! Reads newline-delimited JSON requests from stdin, writes responses to stdout.
//!
//! Request format:
//! ```json
//! {"id":1,"method":"heal","params":{"primary_selector":"...","original_signature":{},"dom_snapshot":{},"min_confidence":0.85}}
//! {"id":2,"method":"analyze_rca","params":{"timeline":{},"failure_at_ms":0,"window_ms":30000}}
//! ```
//!
//! Response format:
//! ```json
//! {"id":1,"result":"<HealResult JSON>"}
//! {"id":2,"error":{"code":"...","message":"..."}}
//! ```

use frap_core::{analyze_rca_json, FlettaCore};
use serde::{Deserialize, Serialize};
use std::io::{self, BufRead, Write};

#[derive(Debug, Deserialize, Serialize)]
struct Request {
    id: serde_json::Value,
    method: String,
    #[serde(default)]
    params: serde_json::Value,
}

#[derive(Debug, Serialize)]
struct Response {
    id: serde_json::Value,
    #[serde(skip_serializing_if = "Option::is_none")]
    result: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    error: Option<ErrorDetail>,
}

#[derive(Debug, Serialize)]
struct ErrorDetail {
    code: String,
    message: String,
}

fn main() {
    let stdin = io::stdin();
    let mut stdout = io::stdout();
    let mut core = FlettaCore::new();

    for line in stdin.lock().lines() {
        match line {
            Ok(line) => {
                if line.trim().is_empty() {
                    continue;
                }
                let response = handle_request(&line, &mut core);
                let json = match serde_json::to_string(&response) {
                    Ok(j) => j,
                    Err(e) => {
                        let err_response = serde_json::json!({
                            "id": null,
                            "error": {
                                "code": "SERIALIZE_ERROR",
                                "message": format!("Failed to serialize response: {}", e)
                            }
                        });
                        err_response.to_string()
                    }
                };
                if writeln!(stdout, "{}", json).is_err() {
                    eprintln!("[frap-core-rpc] Failed to write to stdout");
                    break;
                }
                if stdout.flush().is_err() {
                    eprintln!("[frap-core-rpc] Failed to flush stdout");
                    break;
                }
            }
            Err(e) => {
                eprintln!("[frap-core-rpc] Failed to read stdin: {}", e);
                break;
            }
        }
    }
}

fn handle_request(line: &str, core: &mut FlettaCore) -> Response {
    let request: Request = match serde_json::from_str(line) {
        Ok(r) => r,
        Err(e) => {
            return Response {
                id: serde_json::Value::Null,
                result: None,
                error: Some(ErrorDetail {
                    code: "PARSE_ERROR".to_string(),
                    message: format!("Invalid JSON: {}", e),
                }),
            };
        }
    };

    match request.method.as_str() {
        "heal" => handle_heal(request, core),
        "analyze_rca" => handle_analyze_rca(request),
        _ => Response {
            id: request.id,
            result: None,
            error: Some(ErrorDetail {
                code: "METHOD_NOT_FOUND".to_string(),
                message: format!("Unknown method: {}", request.method),
            }),
        },
    }
}

fn handle_heal(request: Request, core: &mut FlettaCore) -> Response {
    // params can be either a HealRequest object directly, or {"request": {...}}
    let params = &request.params;
    let request_json = if params.get("primary_selector").is_some() {
        // params is the HealRequest itself
        match serde_json::to_string(params) {
            Ok(j) => j,
            Err(e) => {
                return Response {
                    id: request.id,
                    result: None,
                    error: Some(ErrorDetail {
                        code: "SERIALIZE_ERROR".to_string(),
                        message: format!("Failed to serialize params: {}", e),
                    }),
                };
            }
        }
    } else if let Some(req) = params.get("request") {
        match serde_json::to_string(req) {
            Ok(j) => j,
            Err(e) => {
                return Response {
                    id: request.id,
                    result: None,
                    error: Some(ErrorDetail {
                        code: "SERIALIZE_ERROR".to_string(),
                        message: format!("Failed to serialize request: {}", e),
                    }),
                };
            }
        }
    } else {
        return Response {
            id: request.id,
            result: None,
            error: Some(ErrorDetail {
                code: "INVALID_PARAMS".to_string(),
                message: "Missing 'primary_selector' in params".to_string(),
            }),
        };
    };

    match core.heal_json(&request_json) {
        Ok(result) => Response {
            id: request.id,
            result: Some(result),
            error: None,
        },
        Err(e) => Response {
            id: request.id,
            result: None,
            error: Some(ErrorDetail {
                code: "HEAL_ERROR".to_string(),
                message: format!("{}", e),
            }),
        },
    }
}

fn handle_analyze_rca(request: Request) -> Response {
    let params = &request.params;

    let timeline = match params.get("timeline") {
        Some(t) => match serde_json::to_string(t) {
            Ok(j) => j,
            Err(e) => {
                return Response {
                    id: request.id,
                    result: None,
                    error: Some(ErrorDetail {
                        code: "SERIALIZE_ERROR".to_string(),
                        message: format!("Failed to serialize timeline: {}", e),
                    }),
                };
            }
        },
        None => {
            return Response {
                id: request.id,
                result: None,
                error: Some(ErrorDetail {
                    code: "INVALID_PARAMS".to_string(),
                    message: "Missing 'timeline' in params".to_string(),
                }),
            };
        }
    };

    let failure_at_ms = params
        .get("failure_at_ms")
        .and_then(|v| v.as_i64())
        .unwrap_or(0);

    let window_ms = params
        .get("window_ms")
        .and_then(|v| v.as_i64())
        .unwrap_or(30000);

    match analyze_rca_json(&timeline, failure_at_ms, window_ms) {
        Ok(result) => Response {
            id: request.id,
            result: Some(result),
            error: None,
        },
        Err(e) => Response {
            id: request.id,
            result: None,
            error: Some(ErrorDetail {
                code: "RCA_ERROR".to_string(),
                message: format!("{}", e),
            }),
        },
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    fn sample_heal_request() -> serde_json::Value {
        json!({
            "primary_selector": "[data-testid='pay-btn']",
            "original_signature": {
                "path": [{"tag": "button", "role": "submit", "depth": 0}],
                "prefix": "button:submit",
                "stable_attrs": {"data-testid": "pay-btn"},
                "text_content": "Pay",
                "children_hash": 0,
                "depth": 1
            },
            "dom_snapshot": {
                "html": "<button data-testid='checkout-pay'>Pay</button>",
                "elements": [{
                    "selector": "[data-testid='checkout-pay']",
                    "tag": "button",
                    "attributes": {"data-testid": "checkout-pay"},
                    "text_content": "Pay",
                    "path": ["button:submit"]
                }]
            },
            "min_confidence": 0.7
        })
    }

    #[test]
    fn test_handle_heal_success() {
        let request = Request {
            id: json!(1),
            method: "heal".to_string(),
            params: sample_heal_request(),
        };
        let mut core = FlettaCore::new();
        let response = handle_heal(request, &mut core);

        assert!(response.error.is_none(), "Unexpected error: {:?}", response.error);
        assert!(response.result.is_some());
        let result: serde_json::Value =
            serde_json::from_str(&response.result.unwrap()).expect("parse result");
        assert!(result.get("healed").unwrap().as_bool().unwrap());
    }

    #[test]
    fn test_handle_heal_wrapped_params() {
        let request = Request {
            id: json!(2),
            method: "heal".to_string(),
            params: json!({"request": sample_heal_request()}),
        };
        let mut core = FlettaCore::new();
        let response = handle_heal(request, &mut core);

        assert!(response.error.is_none());
        assert!(response.result.is_some());
    }

    #[test]
    fn test_handle_analyze_rca_missing_timeline() {
        let request = Request {
            id: json!(3),
            method: "analyze_rca".to_string(),
            params: json!({"failure_at_ms": 1000}),
        };
        let response = handle_analyze_rca(request);

        assert!(response.error.is_some());
        assert_eq!(response.error.unwrap().code, "INVALID_PARAMS");
    }

    #[test]
    fn test_handle_unknown_method() {
        let request = Request {
            id: json!(4),
            method: "unknown".to_string(),
            params: json!({}),
        };
        let mut core = FlettaCore::new();
        let response = handle_request(
            &serde_json::to_string(&request).unwrap(),
            &mut core,
        );

        assert!(response.error.is_some());
        assert_eq!(response.error.unwrap().code, "METHOD_NOT_FOUND");
    }
}
