package com.framework.api.service;

import com.framework.api.client.PositionStatisticsApiClient;
import com.framework.api.model.request.PositionStatSearchVariables;
import com.framework.api.model.request.PositionStatSearchVariables.FilterCondition;
import com.framework.api.model.request.PositionStatSearchVariables.Paging;
import com.framework.api.model.request.PositionStatSearchVariables.SearchBody;
import com.framework.api.model.request.PositionStatSearchVariables.SearchFilter;
import com.framework.api.model.request.PositionStatSearchVariables.SortField;
import io.restassured.response.Response;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.framework.api.client.PositionStatisticsApiClient.DEFAULT_INDEX;

/**
 * Service layer for the Trader Explorer search business flow.
 *
 * <p>Builds GraphQL variables and delegates to {@link PositionStatisticsApiClient}.
 * Contains no assertions or test logic.
 */
public class SearchTraderExplorerService {

    private final PositionStatisticsApiClient apiClient;

    public SearchTraderExplorerService() {
        this.apiClient = new PositionStatisticsApiClient();
    }

    /**
     * Search trader statistics by protocol list and period type (authenticated).
     * Uses default sort: realisedPnl desc.
     */
    public Response searchByPeriod(List<String> protocols, String type, int size, int from) {
        PositionStatSearchVariables vars = apiClient.buildVars(protocols, type, size, from);
        return apiClient.search(vars);
    }

    /**
     * Search trader statistics without auth token (public access).
     * Uses default sort: realisedPnl desc.
     */
    public Response searchPublic(List<String> protocols, String type, int size, int from) {
        PositionStatSearchVariables vars = apiClient.buildVars(protocols, type, size, from);
        return apiClient.searchWithoutToken(vars);
    }

    /**
     * Search trader statistics with explicit sort field and direction.
     */
    public Response searchWithSort(List<String> protocols, String type,
                                   String sortField, String direction,
                                   int size, int from) {
        PositionStatSearchVariables vars = PositionStatSearchVariables.builder()
                .index(DEFAULT_INDEX)
                .body(SearchBody.builder()
                        .filter(SearchFilter.builder()
                                .and(Arrays.asList(
                                        FilterCondition.builder()
                                                .field("protocol").in(protocols)
                                                .build(),
                                        FilterCondition.builder()
                                                .field("type").match(type)
                                                .build()))
                                .build())
                        .sorts(Collections.singletonList(
                                SortField.builder()
                                        .field(sortField)
                                        .direction(direction)
                                        .build()))
                        .paging(Paging.builder().size(size).from(from).build())
                        .build())
                .build();
        return apiClient.search(vars);
    }
}
