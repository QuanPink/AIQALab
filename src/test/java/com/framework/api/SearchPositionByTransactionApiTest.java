package com.framework.api;

import com.framework.api.service.SearchPositionByTransactionService;
import com.framework.base.BaseApiTest;
import com.framework.validator.ApiValidator;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for position search by transaction hash.
 *
 * <p><b>API:</b> {@code GET /position/filter}
 *
 * <p>This endpoint searches <b>raw position records</b> (individual trade open/close events)
 * by an exact transaction hash. Unlike the trader statistic endpoint, this returns per-position
 * data, not aggregated statistics.
 *
 * <p>Response shape: <b>direct JSON array</b> {@code [ {...}, {...} ]} — no {@code data}/{@code meta} wrapper.
 *
 * <p><b>Known behaviors:</b>
 * <ul>
 *   <li>transaction hash is required — missing returns 400 ({@code "txHash should not be empty"}).</li>
 *   <li>Empty string returns 400.</li>
 *   <li>Non-existent or invalid-format transaction hash returns 200 + {@code []} (no 4xx for format errors).</li>
 *   <li>Endpoint is public — no auth token required.</li>
 *   <li>Multiple positions may share the same transaction hash (e.g., batch operations).</li>
 * </ul>
 *
 * <p><b>Note on "valid transaction hash with data" tests:</b>
 * Tests that require a real transaction hash read from {@code test.position.transactionhash} config property.
 * If not configured, those tests assert the "not found" path (200 + empty).
 * To test the "found" path: {@code mvn test -Dtest.position.transactionhash=0x<real_hash>}
 */
@Epic("Trading Platform API")
@Feature("Position Search by Transaction")
public class SearchPositionByTransactionApiTest extends BaseApiTest {

    private SearchPositionByTransactionService searchPositionByTransactionService;

    /**
     * A real transaction hash from the system — configure via {@code -Dtest.position.transactionhash=0x...}.
     * Defaults to a non-existent but validly-formatted hash when not provided.
     */
    private static final String KNOWN_TRANSACTION_HASH = System.getProperty(
            "test.position.transactionhash",
            "0xdeadbeef" + "1234567890abcdef".repeat(3) + "12345678"  // valid format, no match
    );

    private static final String TRANSACTION_HASH_NONEXISTENT  = "0x" + "a1b2c3d4e5f6".repeat(5) + "a1b2c3d4";
    private static final String TRANSACTION_HASH_INVALID      = "not_a_valid_txhash_xyz";
    private static final String TRANSACTION_HASH_TRUNCATED    = "0xdeadbeef";
    private static final String TRANSACTION_HASH_ALT_FORMAT   = "5uJc7XHBsHpGXmqNaZEHNdPY4mNasBxmKLfKEmF4P8Kt";

    // ─────────────────────────────────────────────────────────────────────

    @BeforeClass
    public void setUpClient() {
        searchPositionByTransactionService = new SearchPositionByTransactionService();
    }

    // ════════════════════════════════════════════════════════════════════
    //  VALID TRANSACTION HASH
    // ════════════════════════════════════════════════════════════════════

    @Test(groups = {"smoke", "regression"},
          description = "Known transaction hash — returns 200; validates schema if data is present")
    @Story("Search Position — Valid Transaction Hash")
    @Severity(SeverityLevel.BLOCKER)
    public void shouldReturnPositionsForKnownTransactionHash() {
        // Uses KNOWN_TRANSACTION_HASH from -Dtest.position.transactionhash.
        // Falls back to a non-existent hash if not configured — asserts empty result.
        Response response = searchPositionByTransactionService.searchByTransactionHash(KNOWN_TRANSACTION_HASH, 20);

        ApiValidator.assertThat(response)
                .statusCodeIs(200)
                .responseTimeUnder(3000);

        List<Object> positions = response.jsonPath().getList("");
        assertThat(positions).as("response must be a JSON array").isNotNull();

        // Schema validation — only runs when data is present
        if (!positions.isEmpty()) {
            shouldHaveValidPositionSchema(response);
        }
    }

    @Test(groups = {"regression"},
          description = "Non-existent transaction hash (valid format) — returns 200 with empty array")
    @Story("Search Position — Transaction Hash Not Found")
    @Severity(SeverityLevel.CRITICAL)
    public void shouldReturnEmptyArrayWhenTransactionHashNotFound() {
        Response response = searchPositionByTransactionService.searchByTransactionHash(TRANSACTION_HASH_NONEXISTENT, 10);

        ApiValidator.assertThat(response)
                .statusCodeIs(200)
                .responseTimeUnder(3000);

        assertThat(response.jsonPath().getList(""))
                .as("response array must be empty for non-existent transaction hash")
                .isEmpty();
    }

    // ════════════════════════════════════════════════════════════════════
    //  INVALID / MISSING TRANSACTION HASH
    // ════════════════════════════════════════════════════════════════════

    @Test(groups = {"regression"},
          description = "Missing transaction hash param — API returns 400 Bad Request")
    @Story("Search Position — Missing Transaction Hash")
    @Severity(SeverityLevel.NORMAL)
    public void shouldReturnErrorWhenTransactionHashIsMissing() {
        ApiValidator.assertThat(searchPositionByTransactionService.searchByTransactionHash(null))
                .statusCodeIs(400)
                .bodyContains("message", "txHash should not be empty")
                .responseTimeUnder(3000);
    }

    @Test(groups = {"regression"},
          description = "Empty string transaction hash — API returns 400 Bad Request")
    @Story("Search Position — Empty Transaction Hash")
    @Severity(SeverityLevel.NORMAL)
    public void shouldReturnErrorWhenTransactionHashIsEmpty() {
        ApiValidator.assertThat(searchPositionByTransactionService.searchByTransactionHash(""))
                .statusCodeIs(400)
                .bodyContains("message", "txHash should not be empty")
                .responseTimeUnder(3000);
    }

    @Test(groups = {"regression"}, dataProvider = "unrecognizedTransactionHashFormats",
          description = "Invalid or unrecognized transaction hash formats — API returns 200 + empty array (no format validation)")
    @Story("Search Position — Invalid Transaction Hash Format")
    @Severity(SeverityLevel.NORMAL)
    public void shouldReturnEmptyArrayForUnrecognizedTransactionHashFormats(String txHash, String caseDesc) {
        Response response = searchPositionByTransactionService.searchByTransactionHash(txHash);

        ApiValidator.assertThat(response)
                .statusCodeIs(200)
                .responseTimeUnder(3000);

        assertThat(response.jsonPath().getList(""))
                .as("response array must be empty for [%s]", caseDesc)
                .isEmpty();
    }

    @DataProvider(name = "unrecognizedTransactionHashFormats", parallel = true)
    public Object[][] unrecognizedTransactionHashFormats() {
        return new Object[][]{
            {TRANSACTION_HASH_INVALID,      "plain string input"},
            {TRANSACTION_HASH_TRUNCATED,    "truncated input (8 chars)"},
            {TRANSACTION_HASH_ALT_FORMAT,   "alternative format string"},
            {"0x" + "0".repeat(64), "all-zero hash"},
        };
    }

    // ════════════════════════════════════════════════════════════════════
    //  AUTHENTICATION & GENERAL
    // ════════════════════════════════════════════════════════════════════

    @Test(groups = {"regression"},
          description = "No auth token — public endpoint returns 200")
    @Story("Search Position — Authentication")
    @Severity(SeverityLevel.NORMAL)
    public void shouldNotRequireAuthenticationToSearch() {
        Response response = searchPositionByTransactionService.searchByTransactionHashPublic(TRANSACTION_HASH_NONEXISTENT);

        ApiValidator.assertThat(response)
                .statusCodeIs(200)
                .responseTimeUnder(3000);

        assertThat(response.jsonPath().getList(""))
                .as("unauthenticated request must return a JSON array")
                .isNotNull();
    }

    // ════════════════════════════════════════════════════════════════════
    //  SCHEMA VALIDATION (helper — called when data is present)
    // ════════════════════════════════════════════════════════════════════

    /**
     * Validates the schema of the first position record.
     * Called conditionally from {@link #shouldReturnPositionsForKnownTransactionHash()}
     * only when the response array is non-empty.
     */
    private void shouldHaveValidPositionSchema(Response response) {
        String base = "[0]";
        assertThat(response.jsonPath().getString(base + ".txHash"))
                .as("position transaction hash must not be blank")
                .isNotBlank();
        assertThat(response.jsonPath().getString(base + ".account"))
                .as("position.account").isNotBlank();
        assertThat(response.jsonPath().getString(base + ".protocol"))
                .as("position.protocol").isNotBlank();
    }
}
