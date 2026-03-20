package com.framework.api.client;

import com.framework.api.model.request.PositionStatFilterParams;
import com.framework.core.config.ConfigManager;
import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Arrays;
import java.util.List;

/**
 * API client for {@code GET /public/position/statistic-v2/filter}.
 *
 * <p>Searches position statistics by account address prefix or txHash fragment.
 * This endpoint is <b>publicly accessible</b> — no token is required, but an
 * authenticated request is also accepted (raw JWT, no "Bearer" prefix).
 */
public class PositionStatFilterApiClient extends BaseApiClient {

    public static final String FILTER_PATH = "/public/position/statistic-v2/filter";

    public static final List<String> ALL_PROTOCOLS = Arrays.asList(
        "GMX", "GMX_AVAX", "KWENTA", "POLYNOMIAL", "GMX_V2", "GMX_V2_AVAX", "GMX_SOL",
        "GNS", "GNS_POLY", "GNS_BASE", "GNS_APE", "LEVEL_BNB", "LEVEL_ARB", "MUX_ARB",
        "EQUATION_ARB", "APOLLOX_BNB", "APOLLOX_BASE", "AVANTIS_BASE", "LOGX_BLAST",
        "LOGX_MODE", "MYX_ARB", "MYX_OPBNB", "MYX_LINEA", "HMX_ARB", "DEXTORO",
        "VELA_ARB", "SYNTHETIX_V3_ARB", "SYNTHETIX_V3", "SYNTHETIX", "KTX_MANTLE",
        "CYBERDEX", "YFX_ARB", "KILOEX_OPBNB", "KILOEX_BNB", "KILOEX_MANTA",
        "KILOEX_BASE", "ROLLIE_SCROLL", "PERENNIAL_ARB", "MUMMY_FANTOM", "MORPHEX_FANTOM",
        "HYPERLIQUID", "SYNFUTURE_BASE", "DYDX", "BSX_BASE", "UNIDEX_ARB", "VERTEX_ARB",
        "LINEHUB_LINEA", "FOXIFY_ARB", "BMX_BASE", "DEPERP_BASE", "HORIZON_BNB",
        "HOLDSTATION_ZKSYNC", "HOLDSTATION_BERA", "POLYNOMIAL_L2", "ZENO_METIS",
        "FULCROM_CRONOS", "ELFI_ARB", "JUPITER", "PINGU_ARB", "OSTIUM_ARB", "DECIBEL"
    );

    // ── Public API ───────────────────────────────────────────────────────

    /** Authenticated search (raw JWT, no "Bearer" prefix). */
    @Step("GET /public/position/statistic-v2/filter — keyword={params.keyword}")
    public Response filter(PositionStatFilterParams params) {
        return applyParams(buildRequest(), params)
                .get(FILTER_PATH).then().extract().response();
    }

    /** Unauthenticated search — endpoint is public, expects 200. */
    @Step("GET /public/position/statistic-v2/filter (no auth) — keyword={params.keyword}")
    public Response filterPublic(PositionStatFilterParams params) {
        RequestSpecification spec = RestAssured.given()
                .baseUri(ConfigManager.get("api.base.url"))
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .filter(new AllureRestAssured());
        return applyParams(spec, params)
                .get(FILTER_PATH).then().extract().response();
    }

    // ── Override: raw JWT (no "Bearer" prefix) ───────────────────────────

    @Override
    protected RequestSpecification buildRequest() {
        return RestAssured.given()
                .baseUri(ConfigManager.get("api.base.url"))
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", getToken())
                .filter(new AllureRestAssured());
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private RequestSpecification applyParams(RequestSpecification spec,
                                             PositionStatFilterParams params) {
        if (params.getKeyword() != null) {
            spec = spec.queryParam("keyword", params.getKeyword());
        }
        spec = spec
                .queryParam("limit",    params.getLimit())
                .queryParam("sortBy",   params.getSortBy())
                .queryParam("sortType", params.getSortType());

        if (params.getProtocols() != null) {
            for (String protocol : params.getProtocols()) {
                spec = spec.queryParam("protocols[]", protocol);
            }
        }
        return spec;
    }
}
