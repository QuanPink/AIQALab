package com.framework.api.model.request;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Query parameters for GET /public/position/statistic-v2/filter.
 *
 * <p>{@code keyword} is <b>required</b> by the API (address prefix or txHash fragment).
 * Set to {@code null} to omit the param entirely — the server will return 400 Bad Request,
 * which is the intended behaviour for negative tests.
 */
@Data
@Builder
public class PositionStatFilterParams {

    /** Account address prefix or txHash fragment to search. Required by the API. */
    private String keyword;

    @Builder.Default private int    limit    = 20;
    @Builder.Default private String sortBy   = "lastTradeAtTs";
    @Builder.Default private String sortType = "desc";

    /** Protocol whitelist. Pass {@code null} to omit the filter entirely. */
    private List<String> protocols;
}
