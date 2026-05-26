//! FFI C API for frap-core.
//!
//! This module provides a C-compatible interface for calling Frap Core
//! from other languages (Java via JNI, Python via ctypes, etc.).
//!
//! # Safety
//!
//! All functions are marked `unsafe` because they deal with raw pointers.
//! Callers must ensure:
//! - Input strings are valid UTF-8 and null-terminated
//! - Returned pointers are freed with the appropriate free function
//! - The library is only initialized once per process

use std::ffi::{CStr, CString};
use std::os::raw::{c_char, c_double, c_int};
use std::ptr::null_mut;

use crate::{analyze_rca_json, FlettaCore, HealRequest};

/// Opaque handle to a FlettaCore instance.
pub struct FrapCoreHandle {
    core: FlettaCore,
}

/// Result structure for healing operations.
#[repr(C)]
pub struct FrapHealResult {
    /// 1 if healed, 0 if not
    pub healed: c_int,
    /// The healed selector (null if not healed)
    pub selector: *mut c_char,
    /// Confidence score (0.0 - 1.0)
    pub confidence: c_double,
    /// Diff description (null if not available)
    pub diff: *mut c_char,
    /// JSON string with full result including candidates (caller must free)
    pub json_result: *mut c_char,
}

/// Error codes for FFI operations.
#[repr(C)]
#[derive(Debug, Clone, Copy)]
pub enum FrapErrorCode {
    /// Success
    Ok = 0,
    /// Null pointer argument
    NullPointer = 1,
    /// Invalid UTF-8 string
    InvalidUtf8 = 2,
    /// JSON parsing error
    JsonError = 3,
    /// Healing failed
    HealingFailed = 4,
    /// Memory allocation failed
    OutOfMemory = 5,
}

/// Error information structure.
#[repr(C)]
pub struct FrapError {
    pub code: FrapErrorCode,
    pub message: *mut c_char,
}

thread_local! {
    static LAST_ERROR: std::cell::RefCell<Option<FrapError>> = std::cell::RefCell::new(None);
}

/// Creates a new FrapCore instance.
///
/// # Safety
/// Returns a valid pointer that must be freed with `frap_core_free`.
#[no_mangle]
pub unsafe extern "C" fn frap_core_new() -> *mut FrapCoreHandle {
    let handle = Box::new(FrapCoreHandle {
        core: FlettaCore::new(),
    });
    Box::into_raw(handle)
}

/// Creates a new FrapCore instance with custom min confidence.
///
/// # Safety
/// Returns a valid pointer that must be freed with `frap_core_free`.
#[no_mangle]
pub unsafe extern "C" fn frap_core_with_confidence(min_confidence: c_double) -> *mut FrapCoreHandle {
    let handle = Box::new(FrapCoreHandle {
        core: FlettaCore::new().with_min_confidence(min_confidence),
    });
    Box::into_raw(handle)
}

/// Frees a FrapCore instance.
///
/// # Safety
/// `handle` must be a valid pointer returned by `frap_core_new` or NULL.
#[no_mangle]
pub unsafe extern "C" fn frap_core_free(handle: *mut FrapCoreHandle) {
    if !handle.is_null() {
        drop(Box::from_raw(handle));
    }
}

/// Performs healing.
///
/// # Arguments
/// * `handle` - FrapCore instance
/// * `request_json` - JSON string with HealRequest
/// * `out_result` - Output pointer for result (caller must free with `frap_heal_result_free`)
///
/// # Returns
/// 0 on success, non-zero on error
///
/// # Safety
/// All pointers must be valid or NULL as documented.
#[no_mangle]
pub unsafe extern "C" fn frap_heal(
    handle: *mut FrapCoreHandle,
    request_json: *const c_char,
    out_result: *mut *mut FrapHealResult,
) -> c_int {
    // Validate inputs
    if handle.is_null() || request_json.is_null() || out_result.is_null() {
        set_last_error(FrapErrorCode::NullPointer, "Null pointer argument");
        return FrapErrorCode::NullPointer as c_int;
    }

    // Parse request JSON
    let request_str = match CStr::from_ptr(request_json).to_str() {
        Ok(s) => s,
        Err(_) => {
            set_last_error(FrapErrorCode::InvalidUtf8, "Invalid UTF-8 in request");
            return FrapErrorCode::InvalidUtf8 as c_int;
        }
    };

    let mut core = &mut (*handle).core;

    // Perform healing
    match core.heal_json(request_str) {
        Ok(result_json) => {
            // Parse result to extract fields
            let result: serde_json::Value = match serde_json::from_str(&result_json) {
                Ok(v) => v,
                Err(e) => {
                    set_last_error(FrapErrorCode::JsonError, &format!("Failed to parse result: {}", e));
                    return FrapErrorCode::JsonError as c_int;
                }
            };

            let healed = result.get("healed").and_then(|v| v.as_bool()).unwrap_or(false);
            let selector = result.get("selector").and_then(|v| v.as_str()).unwrap_or("");
            let confidence = result.get("confidence").and_then(|v| v.as_f64()).unwrap_or(0.0);
            let diff = result.get("diff").and_then(|v| v.as_str());

            // Build result struct
            let result_ptr = Box::into_raw(Box::new(FrapHealResult {
                healed: if healed { 1 } else { 0 },
                selector: string_to_c_ptr(selector),
                confidence,
                diff: diff.map(|d| string_to_c_ptr(d)).unwrap_or(null_mut()),
                json_result: string_to_c_ptr(&result_json),
            }));

            *out_result = result_ptr;
            FrapErrorCode::Ok as c_int
        }
        Err(e) => {
            set_last_error(FrapErrorCode::HealingFailed, &format!("Healing failed: {}", e));
            FrapErrorCode::HealingFailed as c_int
        }
    }
}

/// Frees a heal result.
///
/// # Safety
/// `result` must be a valid pointer returned by `frap_heal` or NULL.
#[no_mangle]
pub unsafe extern "C" fn frap_heal_result_free(result: *mut FrapHealResult) {
    if result.is_null() {
        return;
    }

    let result = Box::from_raw(result);

    if !result.selector.is_null() {
        drop(CString::from_raw(result.selector));
    }
    if !result.diff.is_null() {
        drop(CString::from_raw(result.diff));
    }
    if !result.json_result.is_null() {
        drop(CString::from_raw(result.json_result));
    }
}

/// Performs RCA analysis on a timeline.
///
/// # Arguments
/// * `timeline_json` - JSON string with ContextTimeline
/// * `failure_at_ms` - Failure timestamp (0 for auto-detect)
/// * `window_ms` - Analysis window in milliseconds
/// * `out_result` - Output pointer for JSON result string (caller must free with `frap_string_free`)
///
/// # Returns
/// 0 on success, non-zero on error
///
/// # Safety
/// All pointers must be valid or NULL as documented.
#[no_mangle]
pub unsafe extern "C" fn frap_analyze_rca(
    timeline_json: *const c_char,
    failure_at_ms: i64,
    window_ms: i64,
    out_result: *mut *mut c_char,
) -> c_int {
    if timeline_json.is_null() || out_result.is_null() {
        set_last_error(FrapErrorCode::NullPointer, "Null pointer argument");
        return FrapErrorCode::NullPointer as c_int;
    }

    let timeline_str = match CStr::from_ptr(timeline_json).to_str() {
        Ok(s) => s,
        Err(_) => {
            set_last_error(FrapErrorCode::InvalidUtf8, "Invalid UTF-8 in timeline");
            return FrapErrorCode::InvalidUtf8 as c_int;
        }
    };

    let use_window = if window_ms > 0 { window_ms } else { 30000 };

    match analyze_rca_json(timeline_str, failure_at_ms, use_window) {
        Ok(result) => {
            *out_result = string_to_c_ptr(&result);
            FrapErrorCode::Ok as c_int
        }
        Err(e) => {
            set_last_error(FrapErrorCode::JsonError, &format!("RCA failed: {}", e));
            FrapErrorCode::JsonError as c_int
        }
    }
}

/// Frees a string returned by frap functions.
///
/// # Safety
/// `s` must be a valid pointer returned by frap or NULL.
#[no_mangle]
pub unsafe extern "C" fn frap_string_free(s: *mut c_char) {
    if !s.is_null() {
        drop(CString::from_raw(s));
    }
}

/// Returns the last error message.
///
/// # Safety
/// Returns a pointer to a static string. Do not free.
#[no_mangle]
pub unsafe extern "C" fn frap_last_error() -> *const c_char {
    LAST_ERROR.with(|e| {
        if let Some(ref err) = *e.borrow() {
            if !err.message.is_null() {
                return err.message as *const c_char;
            }
        }
        std::ptr::null()
    })
}

/// Returns the last error code.
#[no_mangle]
pub extern "C" fn frap_last_error_code() -> c_int {
    LAST_ERROR.with(|e| {
        if let Some(ref err) = *e.borrow() {
            err.code as c_int
        } else {
            FrapErrorCode::Ok as c_int
        }
    })
}

/// Clears the last error.
#[no_mangle]
pub extern "C" fn frap_clear_error() {
    LAST_ERROR.with(|e| {
        if let Some(err) = e.borrow_mut().take() {
            unsafe {
                if !err.message.is_null() {
                    drop(CString::from_raw(err.message));
                }
            }
        }
    });
}

// Helper functions

unsafe fn string_to_c_ptr(s: &str) -> *mut c_char {
    match CString::new(s) {
        Ok(cstr) => cstr.into_raw(),
        Err(_) => null_mut(),
    }
}

fn set_last_error(code: FrapErrorCode, message: &str) {
    LAST_ERROR.with(|e| {
        unsafe {
            if let Some(old) = e.borrow_mut().take() {
                if !old.message.is_null() {
                    drop(CString::from_raw(old.message));
                }
            }
        }
        *e.borrow_mut() = Some(FrapError {
            code,
            message: unsafe { string_to_c_ptr(message) },
        });
    });
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_ffi_heal_roundtrip() {
        let request = r#"{
            "primary_selector": "[data-testid='pay-btn']",
            "original_signature": {
                "path": [{"tag": "button", "role": "submit", "depth": 0}],
                "prefix": "button:submit",
                "stable_attrs": {"data-testid": "pay-btn"},
                "children_hash": 0,
                "depth": 1
            },
            "dom_snapshot": {
                "html": "<button data-testid='checkout-pay'>Pay</button>",
                "elements": [{
                    "selector": "[data-testid='checkout-pay']",
                    "tag": "button",
                    "attributes": {"data-testid": "checkout-pay"},
                    "path": ["button:submit"]
                }]
            },
            "min_confidence": 0.7
        }"#;

        unsafe {
            let handle = frap_core_new();
            assert!(!handle.is_null());

            let request_c = CString::new(request).unwrap();
            let mut result: *mut FrapHealResult = null_mut();

            let rc = frap_heal(handle, request_c.as_ptr(), &mut result);
            assert_eq!(rc, 0);
            assert!(!result.is_null());
            assert_eq!((*result).healed, 1);
            assert!((*result).confidence > 0.7);

            frap_heal_result_free(result);
            frap_core_free(handle);
        }
    }
}
