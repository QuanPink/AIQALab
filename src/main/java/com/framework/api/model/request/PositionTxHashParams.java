package com.framework.api.model.request;

import lombok.Builder;
import lombok.Data;

/**
 * Query parameters for {@code GET /position/filter}.
 *
 * <p>{@code txHash} is <b>required</b> by the API. Set to {@code null} to omit the param
 * entirely (the server returns 400 Bad Request) — used for negative tests.
 * An empty string also returns 400.
 */
@Data
@Builder
public class PositionTxHashParams {

    /** Transaction hash to look up. Required. Null → param omitted → 400. */
    private String txHash;

    @Builder.Default private int limit  = 20;
    @Builder.Default private int offset = 0;
}
