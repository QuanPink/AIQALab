package com.framework.api.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Pagination metadata returned in paged GraphQL / REST responses.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageMeta {

    private Integer total;
    private Integer limit;
    private Integer offset;
    private Integer totalPages;
}
