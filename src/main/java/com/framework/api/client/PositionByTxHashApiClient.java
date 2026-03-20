package com.framework.api.client;

import com.framework.api.model.request.PositionTxHashParams;
import com.framework.core.config.ConfigManager;
import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * API client for {@code GET /position/filter}.
 *
 * <p>Looks up raw position records by {@code txHash}.
 * Returns a <b>direct JSON array</b> (no {@code data}/{@code meta} wrapper).
 *
 * <p><b>Auth:</b> Endpoint is public — no token required.
 * Token is still sent when present (raw JWT, no "Bearer" prefix, same as other copin.io clients).
 *
 * <p><b>Required parameter:</b> {@code txHash}.
 * Missing or empty string → 400 Bad Request.
 */
public class PositionByTxHashApiClient extends BaseApiClient {

    public static final String FILTER_PATH = "/position/filter";

    // ── Public API ───────────────────────────────────────────────────────

    /** Lookup positions by txHash (authenticated). */
    @Step("GET /position/filter — txHash={params.txHash}")
    public Response findByTxHash(PositionTxHashParams params) {
        return applyParams(buildRequest(), params)
                .get(FILTER_PATH).then().extract().response();
    }

    /** Lookup positions by txHash without auth token. */
    @Step("GET /position/filter (no auth) — txHash={params.txHash}")
    public Response findByTxHashPublic(PositionTxHashParams params) {
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
                                             PositionTxHashParams params) {
        if (params.getTxHash() != null) {
            spec = spec.queryParam("txHash", params.getTxHash());
        }
        spec = spec
                .queryParam("limit",  params.getLimit())
                .queryParam("offset", params.getOffset());
        return spec;
    }
}
