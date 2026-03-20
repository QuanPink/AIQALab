package com.framework.api.service;

import com.framework.api.client.PositionStatFilterApiClient;
import com.framework.api.model.request.PositionStatFilterParams;
import io.restassured.response.Response;

import java.util.List;

/**
 * Service layer for the Trader Search business flow.
 *
 * <p>Builds {@link PositionStatFilterParams} from business arguments and delegates
 * to {@link PositionStatFilterApiClient}. Contains no assertions or test logic.
 */
public class SearchTraderService {

    private final PositionStatFilterApiClient apiClient;

    public SearchTraderService() {
        this.apiClient = new PositionStatFilterApiClient();
    }

    /**
     * Search traders by keyword with default sort (lastTradeAtTs desc).
     */
    public Response searchByKeyword(String keyword, int limit, List<String> protocols) {
        PositionStatFilterParams params = PositionStatFilterParams.builder()
                .keyword(keyword)
                .limit(limit)
                .protocols(protocols)
                .build();
        return apiClient.filter(params);
    }

    /**
     * Search traders by keyword with explicit sort field and direction.
     */
    public Response searchByKeyword(String keyword, int limit,
                                    String sortBy, String sortType,
                                    List<String> protocols) {
        PositionStatFilterParams params = PositionStatFilterParams.builder()
                .keyword(keyword)
                .limit(limit)
                .sortBy(sortBy)
                .sortType(sortType)
                .protocols(protocols)
                .build();
        return apiClient.filter(params);
    }

    /**
     * Search traders without auth token (public endpoint).
     */
    public Response searchPublic(String keyword, int limit, List<String> protocols) {
        PositionStatFilterParams params = PositionStatFilterParams.builder()
                .keyword(keyword)
                .limit(limit)
                .protocols(protocols)
                .build();
        return apiClient.filterPublic(params);
    }
}
