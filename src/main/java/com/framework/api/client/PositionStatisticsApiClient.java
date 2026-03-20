package com.framework.api.client;

import com.framework.api.model.request.GraphQLRequest;
import com.framework.api.model.request.PositionStatSearchVariables;
import com.framework.api.model.request.PositionStatSearchVariables.FilterCondition;
import com.framework.api.model.request.PositionStatSearchVariables.Paging;
import com.framework.api.model.request.PositionStatSearchVariables.SearchBody;
import com.framework.api.model.request.PositionStatSearchVariables.SearchFilter;
import com.framework.api.model.request.PositionStatSearchVariables.SortField;
import com.framework.core.config.ConfigManager;
import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * API client for the {@code searchPositionStatistic} GraphQL operation.
 *
 * <p><b>Auth note:</b> copin.io GraphQL sends the raw JWT (no "Bearer" prefix).
 * This client overrides {@link #buildRequest()} accordingly.
 */
public class PositionStatisticsApiClient extends BaseApiClient {

    public static final String GRAPHQL_PATH = "/graphql";
    public static final String DEFAULT_INDEX = "copin.position_statistics_v2";

    public static final List<String> ALL_PROTOCOLS = Arrays.asList(
        "GMX_V2","GMX_V2_AVAX","GMX_SOL","GNS","GNS_POLY","GNS_BASE","GNS_APE","LEVEL_BNB",
        "APOLLOX_BNB","AVANTIS_BASE","HMX_ARB","KILOEX_OPBNB","KILOEX_BNB","KILOEX_MANTA",
        "KILOEX_BASE","DYDX","FULCROM_CRONOS","JUPITER","OSTIUM_ARB","DECIBEL"
    );

    // ── GraphQL query ────────────────────────────────────────────────────

    static final String SEARCH_QUERY =
            "query Search($index: String!, $body: SearchPayload!) {\n" +
            "  searchPositionStatistic(index: $index, body: $body) {\n" +
            "    data {\n" +
            "      id account protocol type\n" +
            "      maxDuration minDuration avgDuration\n" +
            "      realisedTotalGain totalLose totalLoss totalWin totalTrade\n" +
            "      totalLiquidation totalLiquidationAmount totalFee\n" +
            "      avgVolume totalVolume gainLossRatio orderPositionRatio profitLossRatio\n" +
            "      winRate longRate profitRate\n" +
            "      pnl unrealisedPnl avgRoi maxRoi maxPnl longPnl shortPnl\n" +
            "      avgLeverage maxLeverage minLeverage\n" +
            "      totalLongVolume totalShortVolume\n" +
            "      sharpeRatio sortinoRatio\n" +
            "      winStreak loseStreak maxWinStreak maxLoseStreak\n" +
            "      runTimeDays indexTokens lastTradeAt lastTradeAtTs\n" +
            "      statisticAt createdAt updatedAt\n" +
            "      realisedTotalLoss realisedPnl realisedAvgRoi realisedMaxRoi\n" +
            "      realisedMaxPnl realisedLongPnl realisedShortPnl\n" +
            "      realisedMaxDrawdown realisedMaxDrawdownPnl\n" +
            "      realisedProfitRate realisedGainLossRatio realisedProfitLossRatio\n" +
            "      realisedSharpeRatio realisedSortinoRatio\n" +
            "      totalGain pairs maxDrawdown maxDrawdownPnl\n" +
            "      ifLabels ifGoodMarkets ifBadMarkets\n" +
            "      statisticLabels aggregatedLabels\n" +
            "      realisedStatisticLabels realisedAggregatedLabels\n" +
            "    }\n" +
            "    meta {\n" +
            "      total\n" +
            "      limit\n" +
            "      offset\n" +
            "      totalPages\n" +
            "    }\n" +
            "  }\n" +
            "}";

    // ── Public API ───────────────────────────────────────────────────────

    /**
     * Execute a position-statistics search with the given variables (authenticated).
     */
    @Step("POST /graphql — searchPositionStatistic {variables.body.paging.size} results")
    public Response search(PositionStatSearchVariables variables) {
        return buildRequest()
                .body(wrapQuery(variables))
                .post(GRAPHQL_PATH)
                .then().extract().response();
    }

    /**
     * Execute search WITHOUT the Authorization header (for 401/403 tests).
     */
    @Step("POST /graphql — searchPositionStatistic (no auth)")
    public Response searchWithoutToken(PositionStatSearchVariables variables) {
        return buildUnauthenticatedRequest()
                .body(wrapQuery(variables))
                .post(GRAPHQL_PATH)
                .then().extract().response();
    }

    // ── Convenience builders ─────────────────────────────────────────────

    /**
     * Build a search request for a list of protocols + period type with default sorting.
     *
     * @param protocols list of protocol names, e.g. {@link #ALL_PROTOCOLS}
     * @param type      e.g. "D30", "D7", "D14", "D60", "D90"
     * @param size      page size (max records to return)
     * @param from      offset (0-based)
     */
    public PositionStatSearchVariables buildVars(
            List<String> protocols, String type, int size, int from) {

        return PositionStatSearchVariables.builder()
                .index(DEFAULT_INDEX)
                .body(SearchBody.builder()
                        .filter(SearchFilter.builder()
                                .and(Arrays.asList(
                                        FilterCondition.builder()
                                                .field("protocol")
                                                .in(protocols)
                                                .build(),
                                        FilterCondition.builder()
                                                .field("type")
                                                .match(type)
                                                .build()))
                                .build())
                        .sorts(Collections.singletonList(
                                SortField.builder()
                                        .field("realisedPnl")
                                        .direction("desc")
                                        .build()))
                        .paging(Paging.builder()
                                .size(size)
                                .from(from)
                                .build())
                        .build())
                .build();
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private GraphQLRequest wrapQuery(PositionStatSearchVariables variables) {
        return GraphQLRequest.builder()
                .operationName("Search")
                .variables(variables)
                .query(SEARCH_QUERY)
                .build();
    }

    /**
     * copin.io GraphQL expects a raw JWT in the {@code authorization} header
     * (no "Bearer" prefix), so we override the base request builder.
     */
    @Override
    protected RequestSpecification buildRequest() {
        return RestAssured.given()
                .baseUri(ConfigManager.get("api.base.url"))
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", getToken())
                .filter(new AllureRestAssured());
    }

    private RequestSpecification buildUnauthenticatedRequest() {
        return RestAssured.given()
                .baseUri(ConfigManager.get("api.base.url"))
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .filter(new AllureRestAssured());
    }
}
