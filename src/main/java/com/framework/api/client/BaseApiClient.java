package com.framework.api.client;

import com.framework.core.config.ConfigManager;
import com.framework.core.logger.LogManager;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Base API client — all domain-specific clients extend this.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Base URL and default Content-Type / Accept headers</li>
 *   <li>Bearer token injection (override {@link #getToken()} per client)</li>
 *   <li>Allure attachment (always active)</li>
 *   <li>Console request/response logging (controlled by {@code api.logging.enabled})</li>
 *   <li>HTTP verb convenience methods</li>
 * </ul>
 */
public abstract class BaseApiClient {

    // ── Request building ─────────────────────────────────────────────────

    protected RequestSpecification buildRequest() {
        return RestAssured.given()
                .baseUri(ConfigManager.get("api.base.url"))
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Bearer " + getToken())
                .filters(buildFilters());
    }

    /**
     * Builds the filter list for every request.
     * Allure is always active; console logging is toggled by {@code api.logging.enabled}.
     */
    private List<Filter> buildFilters() {
        List<Filter> filters = new ArrayList<>();
        filters.add(new AllureRestAssured());
        if (ConfigManager.getBoolean("api.logging.enabled", true)) {
            filters.add(new RequestLoggingFilter());
            filters.add(new ResponseLoggingFilter());
        }
        return filters;
    }

    // ── HTTP verbs ───────────────────────────────────────────────────────

    protected Response get(String endpoint) {
        LogManager.debug("GET " + endpoint);
        return buildRequest().get(endpoint);
    }

    protected Response get(String endpoint, Map<String, Object> queryParams) {
        LogManager.debug("GET " + endpoint + " params=" + queryParams);
        return buildRequest().queryParams(queryParams).get(endpoint);
    }

    protected Response post(String endpoint, Object body) {
        LogManager.debug("POST " + endpoint);
        return buildRequest().body(body).post(endpoint);
    }

    protected Response put(String endpoint, Object body) {
        LogManager.debug("PUT " + endpoint);
        return buildRequest().body(body).put(endpoint);
    }

    protected Response patch(String endpoint, Object body) {
        LogManager.debug("PATCH " + endpoint);
        return buildRequest().body(body).patch(endpoint);
    }

    protected Response delete(String endpoint) {
        LogManager.debug("DELETE " + endpoint);
        return buildRequest().delete(endpoint);
    }

    // ── Extension point ───────────────────────────────────────────────────

    /**
     * Returns the Bearer token for this client.
     * Override to implement per-client auth (e.g. different roles, OAuth2 refresh).
     */
    protected String getToken() {
        return ConfigManager.get("api.token", "");
    }
}
