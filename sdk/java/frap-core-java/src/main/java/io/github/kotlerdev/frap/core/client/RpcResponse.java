package io.github.kotlerdev.frap.core.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON-RPC response from frap-core-rpc.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RpcResponse(
    @JsonProperty("id") Object id,
    @JsonProperty("result") String result,
    @JsonProperty("error") RpcError error
) {
    public boolean isSuccess() {
        return error == null;
    }

    public boolean isError() {
        return error != null;
    }

    /**
     * RPC error detail.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RpcError(
        @JsonProperty("code") String code,
        @JsonProperty("message") String message
    ) {
    }
}
