package io.github.kotlerdev.frap.core.client;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON-RPC request for frap-core-rpc.
 */
public record RpcRequest(
    @JsonProperty("id") Object id,
    @JsonProperty("method") String method,
    @JsonProperty("params") Object params
) {
    public RpcRequest {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("method is required");
        }
    }

    public RpcRequest(Object id, String method) {
        this(id, method, null);
    }
}
