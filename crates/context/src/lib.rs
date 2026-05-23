//! Unified context timeline (UI + network + logs).

pub mod correlation;
pub mod logs;
pub mod network;
pub mod timeline;

pub use correlation::{events_by_trace_id, group_by_trace_id, network_before_ui_failure};
pub use logs::{LogEvent, LogLevel};
pub use network::{MessageDirection, NetworkEvent, NetworkPhase, NetworkProtocol};
pub use timeline::{event_timestamp_ms, Event, Timeline};
