package com.framework.api.service;

import com.framework.api.client.PositionByTxHashApiClient;
import com.framework.api.model.request.PositionTxHashParams;
import io.restassured.response.Response;

/**
 * Service layer for the Position Search by transaction hash business flow.
 *
 * <p>Builds {@link PositionTxHashParams} from business arguments and delegates
 * to {@link PositionByTxHashApiClient}. Contains no assertions or test logic.
 */
public class SearchPositionByTransactionService {

    private final PositionByTxHashApiClient apiClient;

    public SearchPositionByTransactionService() {
        this.apiClient = new PositionByTxHashApiClient();
    }

    /**
     * Search positions by transaction hash with explicit limit.
     * Pass {@code null} to omit the txHash param (triggers 400 — for negative tests).
     */
    public Response searchByTransactionHash(String txHash, int limit) {
        PositionTxHashParams params = PositionTxHashParams.builder()
                .txHash(txHash)
                .limit(limit)
                .build();
        return apiClient.findByTxHash(params);
    }

    /**
     * Search positions by transaction hash with default limit.
     * Pass {@code null} to omit the txHash param (triggers 400 — for negative tests).
     */
    public Response searchByTransactionHash(String txHash) {
        PositionTxHashParams params = PositionTxHashParams.builder()
                .txHash(txHash)
                .build();
        return apiClient.findByTxHash(params);
    }

    /**
     * Search positions by transaction hash without auth token (public endpoint).
     */
    public Response searchByTransactionHashPublic(String txHash) {
        PositionTxHashParams params = PositionTxHashParams.builder()
                .txHash(txHash)
                .build();
        return apiClient.findByTxHashPublic(params);
    }
}
