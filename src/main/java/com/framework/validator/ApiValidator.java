package com.framework.validator;

import io.restassured.response.Response;
import org.assertj.core.api.Assertions;

import java.util.List;

/**
 * Fluent validator for REST API responses.
 *
 * <p>Usage:
 * <pre>{@code
 * ApiValidator.assertThat(response)
 *     .statusCodeIs(200)
 *     .bodyContains("data.name", "Alice")
 *     .responseTimeUnder(2000);
 * }</pre>
 */
public class ApiValidator {

    private final Response response;

    private ApiValidator(Response response) {
        this.response = response;
    }

    public static ApiValidator assertThat(Response response) {
        return new ApiValidator(response);
    }

    // ── Status ────────────────────────────────────────────────────────────

    public ApiValidator statusCodeIs(int expected) {
        Assertions.assertThat(response.statusCode())
                .as("Expected HTTP %d but got %d.\nBody: %s",
                        expected, response.statusCode(), response.asString())
                .isEqualTo(expected);
        return this;
    }

    public ApiValidator statusCodeInRange(int from, int to) {
        Assertions.assertThat(response.statusCode())
                .as("Status code %d not in range [%d, %d]",
                        response.statusCode(), from, to)
                .isBetween(from, to);
        return this;
    }

    // ── Body ──────────────────────────────────────────────────────────────

    public ApiValidator bodyContains(String jsonPath, Object expectedValue) {
        Object actual = response.jsonPath().get(jsonPath);
        Assertions.assertThat(actual)
                .as("JsonPath [%s] — expected <%s> but was <%s>",
                        jsonPath, expectedValue, actual)
                .isEqualTo(expectedValue);
        return this;
    }

    public ApiValidator bodyFieldNotNull(String jsonPath) {
        Object actual = response.jsonPath().get(jsonPath);
        Assertions.assertThat(actual)
                .as("Expected field [%s] to be non-null", jsonPath)
                .isNotNull();
        return this;
    }

    public ApiValidator bodyFieldIsNull(String jsonPath) {
        Object actual = response.jsonPath().get(jsonPath);
        Assertions.assertThat(actual)
                .as("Expected field [%s] to be null but was <%s>", jsonPath, actual)
                .isNull();
        return this;
    }

    public ApiValidator listNotEmpty(String jsonPath) {
        List<?> list = response.jsonPath().getList(jsonPath);
        Assertions.assertThat(list)
                .as("Expected list at [%s] to be non-empty", jsonPath)
                .isNotEmpty();
        return this;
    }

    public ApiValidator listSizeIs(String jsonPath, int expectedSize) {
        List<?> list = response.jsonPath().getList(jsonPath);
        Assertions.assertThat(list)
                .as("Expected list at [%s] to have %d items but had %d",
                        jsonPath, expectedSize, list == null ? 0 : list.size())
                .hasSize(expectedSize);
        return this;
    }

    public ApiValidator bodyContainsString(String substring) {
        Assertions.assertThat(response.asString())
                .as("Response body does not contain: " + substring)
                .contains(substring);
        return this;
    }

    // ── Performance ───────────────────────────────────────────────────────

    public ApiValidator responseTimeUnder(long maxMilliseconds) {
        Assertions.assertThat(response.time())
                .as("Response time %dms exceeded SLA of %dms",
                        response.time(), maxMilliseconds)
                .isLessThan(maxMilliseconds);
        return this;
    }

    // ── Headers ───────────────────────────────────────────────────────────

    public ApiValidator headerExists(String headerName) {
        Assertions.assertThat(response.header(headerName))
                .as("Expected header [%s] to be present", headerName)
                .isNotNull();
        return this;
    }

    public ApiValidator headerEquals(String headerName, String expectedValue) {
        Assertions.assertThat(response.header(headerName))
                .as("Header [%s] mismatch", headerName)
                .isEqualToIgnoringCase(expectedValue);
        return this;
    }

    // ── Extraction ────────────────────────────────────────────────────────

    public <T> T extractAs(Class<T> clazz) {
        return response.as(clazz);
    }

    public <T> T extractField(String jsonPath, Class<T> type) {
        return response.jsonPath().getObject(jsonPath, type);
    }

    public String extractString(String jsonPath) {
        return response.jsonPath().getString(jsonPath);
    }

    public int extractInt(String jsonPath) {
        return response.jsonPath().getInt(jsonPath);
    }

    public Response getResponse() {
        return response;
    }
}
