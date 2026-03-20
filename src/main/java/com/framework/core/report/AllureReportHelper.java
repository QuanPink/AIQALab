package com.framework.core.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.framework.core.config.ConfigManager;
import io.qameta.allure.Allure;
import io.restassured.response.Response;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility for manually attaching data to the Allure report from test code.
 *
 * <p>REST Assured responses are captured automatically via the
 * {@code AllureRestAssured} filter in {@code BaseApiClient}. Use this class
 * for additional attachments (JSON payloads, CSV data, environment metadata).
 *
 * <p>Usage:
 * <pre>{@code
 * AllureReportHelper.attachJson("Request body", myRequestObject);
 * AllureReportHelper.attachText("Correlation ID", correlationId);
 * }</pre>
 */
public class AllureReportHelper {

    /** Shared, thread-safe, pretty-printing Jackson mapper. */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private AllureReportHelper() {}

    // ── Text attachments ──────────────────────────────────────────────────

    public static void attachText(String label, String content) {
        Allure.addAttachment(label, "text/plain",
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), ".txt");
    }

    public static void attachJson(String label, String json) {
        Allure.addAttachment(label, "application/json",
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), ".json");
    }

    /**
     * Serialises {@code object} to pretty-printed JSON and attaches it.
     * Uses Jackson — works correctly with Lombok {@code @Data} / {@code @Builder} models.
     *
     * @throws RuntimeException if Jackson cannot serialise the object
     */
    public static void attachJson(String label, Object object) {
        try {
            String json = MAPPER.writeValueAsString(object);
            attachJson(label, json);
        } catch (Exception e) {
            attachText(label + " (serialisation failed)", object.toString());
        }
    }

    // ── API response attachments ──────────────────────────────────────────

    /**
     * Attaches the raw response body to the report.
     * Useful for debugging when the {@code AllureRestAssured} filter is disabled.
     */
    public static void attachResponse(String label, Response response) {
        String body = response.asPrettyString();
        String mediaType = response.contentType() != null
                ? response.contentType().split(";")[0].trim()
                : "text/plain";
        Allure.addAttachment(label, mediaType,
                new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)), ".json");
    }

    // ── Environment info ──────────────────────────────────────────────────

    /**
     * Writes the current environment name and URLs to the Allure report.
     * Call once from a {@code @BeforeSuite} hook in {@code BaseTest}.
     */
    public static void attachEnvironmentInfo() {
        String env = System.getProperty("env", "dev");
        String info = "Environment : " + env + "\n"
                + "Base URL    : " + ConfigManager.get("base.url", "N/A") + "\n"
                + "API URL     : " + ConfigManager.get("api.base.url", "N/A") + "\n"
                + "Browser     : " + ConfigManager.get("browser", "chrome") + "\n"
                + "Headless    : " + ConfigManager.get("headless", "false");
        attachText("Test Environment", info);
    }
}
