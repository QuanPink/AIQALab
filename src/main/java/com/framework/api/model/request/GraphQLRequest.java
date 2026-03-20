package com.framework.api.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * Generic GraphQL request envelope.
 * Wraps any operationName + variables + query string.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GraphQLRequest {

    private String operationName;
    private Object variables;
    private String query;
}
