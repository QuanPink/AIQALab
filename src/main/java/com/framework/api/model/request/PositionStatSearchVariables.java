package com.framework.api.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Variables payload for the {@code searchPositionStatistic} GraphQL query.
 *
 * <pre>
 * {
 *   "index": "copin.position_statistics_v2",
 *   "body": {
 *     "filter": { "and": [...] },
 *     "sorts": [...],
 *     "paging": { "size": 20, "from": 0 }
 *   }
 * }
 * </pre>
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PositionStatSearchVariables {

    private String index;
    private SearchBody body;

    // ── Nested models ────────────────────────────────────────────────────

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SearchBody {
        private SearchFilter filter;
        private List<SortField> sorts;
        private Paging paging;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SearchFilter {
        private List<FilterCondition> and;
    }

    /** Supports both {@code in} (multi-value) and {@code match} (exact-value) predicates. */
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FilterCondition {
        private String field;
        private List<String> in;
        private String match;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SortField {
        private String field;
        private String direction;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Paging {
        private Integer size;
        private Integer from;
    }
}
