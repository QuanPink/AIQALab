package com.framework.api;

import com.framework.api.service.SearchTraderService;
import com.framework.base.BaseApiTest;
import com.framework.validator.ApiValidator;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.*;

import com.framework.data.constants.ProtocolTestData;

import java.util.List;

import static com.framework.api.client.PositionStatFilterApiClient.ALL_PROTOCOLS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for trader search by address keyword.
 *
 * <p><b>API:</b> {@code GET /public/position/statistic-v2/filter}
 *
 * <p>This endpoint searches <b>traders</b> (aggregated statistics per account) using
 * a keyword that is prefix-matched against the {@code account} field.
 * Supports hex ({@code 0x...}), base58, and bech32 address formats.
 *
 * <p>Response shape: {@code { data: [...], meta: { limit, offset, total, totalPages } }}
 *
 * <p><b>Known behaviors:</b>
 * <ul>
 *   <li>{@code keyword} is required — missing returns 400.</li>
 *   <li>Matching is case-insensitive prefix search on {@code account}.</li>
 *   <li>Non-matching keyword returns 200 + empty data (no 4xx).</li>
 *   <li>{@code type} is always {@code "FULL"} for every returned record.</li>
 *   <li>Endpoint is public — no auth token required.</li>
 *   <li>Hash-formatted keywords are accepted but never match (not indexed here).</li>
 * </ul>
 */
@Epic("Trading Platform API")
@Feature("Trader Search")
public class SearchTraderApiTest extends BaseApiTest {

    private SearchTraderService searchTraderService;

    // ── Test data ─────────────────────────────────────────────────────────

    private static final String HEX_ACCOUNT_PREFIX    = "0xa";
    private static final String BASE58_ACCOUNT_PREFIX = "AZvd";
    private static final String BECH32_ACCOUNT_PREFIX   = ProtocolTestData.getBech32Prefix();
    private static final String NONEXISTENT   = "ZZZZNOTEXISTXXX";

    private static final String       HASH_FORMATTED_KEYWORD     =
            "0x" + "a1b2c3d4e5f6".repeat(5) + "a1b2c3d4";

    // ─────────────────────────────────────────────────────────────────────

    @BeforeClass
    public void setUpClient() {
        searchTraderService = new SearchTraderService();
    }

    // ════════════════════════════════════════════════════════════════════
    //  SEARCH BY HEX ADDRESS
    // ════════════════════════════════════════════════════════════════════

    @Test(groups = {"smoke", "regression"},
          description = "hex address prefix — returns matching traders with data and pagination")
    @Story("Search Trader — Hex Account Address")
    @Severity(SeverityLevel.BLOCKER)
    public void shouldFindTradersWithHexAddressPrefix() {
        ApiValidator.assertThat(searchTraderService.searchByKeyword(HEX_ACCOUNT_PREFIX, 20, ALL_PROTOCOLS))
                .statusCodeIs(200)
                .listNotEmpty("data")
                .bodyFieldNotNull("meta")
                .responseTimeUnder(3000);
    }

    @Test(groups = {"regression"},
          description = "Every returned trader account must start with the hex keyword (case-insensitive)")
    @Story("Search Trader — Hex Account Address")
    @Severity(SeverityLevel.CRITICAL)
    public void shouldReturnOnlyAccountsMatchingHexAddressKeyword() {
        Response response = searchTraderService.searchByKeyword(HEX_ACCOUNT_PREFIX, 20, ALL_PROTOCOLS);

        ApiValidator.assertThat(response)
                .statusCodeIs(200)
                .listNotEmpty("data");

        List<String> accounts = response.jsonPath().getList("data.account");
        assertThat(accounts)
                .as("every account must start with keyword '%s' (case-insensitive)", HEX_ACCOUNT_PREFIX)
                .isNotEmpty()
                .allMatch(a -> a != null && a.toLowerCase().startsWith(HEX_ACCOUNT_PREFIX.toLowerCase()));
    }

    // ════════════════════════════════════════════════════════════════════
    //  SEARCH BY NON-HEX ADDRESS
    // ════════════════════════════════════════════════════════════════════

    @Test(groups = {"regression"},
          description = "base58 address prefix with configured protocol filter — returns matching traders")
    @Story("Search Trader — Base58 Account Address")
    @Severity(SeverityLevel.NORMAL)
    public void shouldFindTradersWithBase58AddressPrefix() {
        Response response = searchTraderService.searchByKeyword(BASE58_ACCOUNT_PREFIX, 10, ProtocolTestData.getBase58ProtocolFilter());

        ApiValidator.assertThat(response)
                .statusCodeIs(200)
                .listNotEmpty("data")
                .responseTimeUnder(3000);

        List<String> accounts = response.jsonPath().getList("data.account");
        assertThat(accounts)
                .as("Base58 accounts must start with prefix '%s' (case-insensitive)", BASE58_ACCOUNT_PREFIX)
                .isNotEmpty()
                .allMatch(a -> a != null && a.toLowerCase().startsWith(BASE58_ACCOUNT_PREFIX.toLowerCase()));
    }

    @Test(groups = {"regression"},
          description = "bech32 address prefix with configured protocol filter — returns matching traders")
    @Story("Search Trader — Bech32 Account Address")
    @Severity(SeverityLevel.NORMAL)
    public void shouldFindTradersWithBech32AddressPrefix() {
        Response response = searchTraderService.searchByKeyword(BECH32_ACCOUNT_PREFIX, 10, ProtocolTestData.getBech32ProtocolFilter());

        ApiValidator.assertThat(response)
                .statusCodeIs(200)
                .listNotEmpty("data")
                .responseTimeUnder(3000);

        List<String> accounts = response.jsonPath().getList("data.account");
        assertThat(accounts)
                .as("Bech32 accounts must start with prefix '%s' (case-insensitive)", BECH32_ACCOUNT_PREFIX)
                .isNotEmpty()
                .allMatch(a -> a != null && a.toLowerCase().startsWith(BECH32_ACCOUNT_PREFIX.toLowerCase()));
    }

    @Test(groups = {"regression"}, dataProvider = "supportedAddressFormats",
          description = "Multiple address formats (hex/base58/bech32) — all return 200 without error")
    @Story("Search Trader — Multiple Address Formats")
    @Severity(SeverityLevel.NORMAL)
    public void shouldSupportMultipleAddressFormats(String keyword, String format) {
        ApiValidator.assertThat(searchTraderService.searchByKeyword(keyword, 5, ALL_PROTOCOLS))
                .statusCodeIs(200)
                .responseTimeUnder(3000);

        assertThat(keyword)
                .as("keyword for format [%s] must not be blank", format)
                .isNotBlank();
    }

    @DataProvider(name = "supportedAddressFormats", parallel = true)
    public Object[][] supportedAddressFormats() {
        return new Object[][]{
            {"0xa",   "hex lowercase"},
            {"0xA",   "hex uppercase"},
            {"0x00",  "hex zero prefix"},
            {"AZvd",  "base58"},
            {ProtocolTestData.getBech32Prefix(), "bech32"},
        };
    }

    // ════════════════════════════════════════════════════════════════════
    //  EDGE CASES
    // ════════════════════════════════════════════════════════════════════

    @Test(groups = {"regression"},
          description = "Non-existent address keyword — returns 200 with empty data, not an error")
    @Story("Search Trader — Not Found")
    @Severity(SeverityLevel.NORMAL)
    public void shouldReturnEmptyWhenTraderNotFound() {
        Response response = searchTraderService.searchByKeyword(NONEXISTENT, 10, ALL_PROTOCOLS);

        ApiValidator.assertThat(response)
                .statusCodeIs(200)
                .responseTimeUnder(3000);

        assertThat(response.jsonPath().getList("data")).as("data must be empty").isEmpty();
        assertThat(response.jsonPath().getInt("meta.total")).as("meta.total must be 0").isEqualTo(0);
    }

    @Test(groups = {"regression"},
          description = "hash-formatted keyword is accepted — API does not crash, returns valid response")
    @Story("Search Trader — Hash-Formatted Keyword")
    @Severity(SeverityLevel.NORMAL)
    public void shouldAcceptHashFormattedKeywordWithoutError() {
        // This endpoint indexes by account, not transaction hash.
        // Validates only: 200 + valid JSON response (data may be empty).
        ApiValidator.assertThat(searchTraderService.searchByKeyword(HASH_FORMATTED_KEYWORD, 5, ALL_PROTOCOLS))
                .statusCodeIs(200)
                .bodyFieldNotNull("data")  // data array is present (may be empty)
                .responseTimeUnder(3000);
    }

    // ════════════════════════════════════════════════════════════════════
    //  FILTER & SORT
    // ════════════════════════════════════════════════════════════════════

    @Test(groups = {"regression"},
          description = "Protocol subset filter — every returned trader belongs to one of the requested protocols")
    @Story("Search Trader — Protocol Filter")
    @Severity(SeverityLevel.NORMAL)
    public void shouldFilterTradersByProtocolCorrectly() {
        Response response = searchTraderService.searchByKeyword(HEX_ACCOUNT_PREFIX, 20, ProtocolTestData.getSubsetProtocols());

        ApiValidator.assertThat(response)
                .statusCodeIs(200)
                .responseTimeUnder(3000);

        List<String> returnedProtocols = response.jsonPath().getList("data.protocol");
        assertThat(returnedProtocols)
                .as("all returned protocols must be in the requested subset")
                .isNotEmpty()
                .allMatch(ProtocolTestData.getSubsetProtocols()::contains);
    }

    @Test(groups = {"regression"},
          description = "sortType=asc — traders sorted from oldest to most recent last trade")
    @Story("Search Trader — Sort")
    @Severity(SeverityLevel.NORMAL)
    public void shouldSortTradersByLastTradeTimeAscending() {
        Response response = searchTraderService.searchByKeyword(
                HEX_ACCOUNT_PREFIX, 10, "lastTradeAtTs", "asc", ALL_PROTOCOLS);

        ApiValidator.assertThat(response)
                .statusCodeIs(200)
                .responseTimeUnder(3000);

        List<Long> timestamps = response.jsonPath().getList("data.lastTradeAtTs", Long.class);
        assertThat(timestamps).as("timestamps must be non-empty for sort validation").isNotEmpty();

        for (int i = 0; i < timestamps.size() - 1; i++) {
            assertThat(timestamps.get(i))
                    .as("record[%d].lastTradeAtTs must be ≤ record[%d].lastTradeAtTs", i, i + 1)
                    .isLessThanOrEqualTo(timestamps.get(i + 1));
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  PAGINATION, AUTH, SCHEMA
    // ════════════════════════════════════════════════════════════════════

    @Test(groups = {"regression"},
          description = "limit=5 — response contains at most 5 traders; meta.limit echoes the request")
    @Story("Search Trader — Pagination")
    @Severity(SeverityLevel.NORMAL)
    public void shouldRespectPaginationLimit() {
        Response response = searchTraderService.searchByKeyword(HEX_ACCOUNT_PREFIX, 5, ALL_PROTOCOLS);

        ApiValidator.assertThat(response)
                .statusCodeIs(200)
                .responseTimeUnder(3000);

        assertThat(response.jsonPath().getList("data"))
                .as("data.size must be ≤ limit").hasSizeLessThanOrEqualTo(5);
        assertThat(response.jsonPath().getInt("meta.limit"))
                .as("meta.limit must echo the requested limit").isEqualTo(5);
    }

    @Test(groups = {"regression"},
          description = "No auth token — endpoint is public, returns 200 with data")
    @Story("Search Trader — Authentication")
    @Severity(SeverityLevel.NORMAL)
    public void shouldNotRequireAuthenticationToSearch() {
        ApiValidator.assertThat(searchTraderService.searchPublic(HEX_ACCOUNT_PREFIX, 5, ALL_PROTOCOLS))
                .statusCodeIs(200)
                .listNotEmpty("data")
                .responseTimeUnder(3000);
    }

    @Test(groups = {"regression"},
          description = "Missing keyword — API returns 400 Bad Request with error message")
    @Story("Search Trader — Validation")
    @Severity(SeverityLevel.NORMAL)
    public void shouldReturnErrorWhenKeywordIsMissing() {
        ApiValidator.assertThat(searchTraderService.searchByKeyword(null, 5, ALL_PROTOCOLS))
                .statusCodeIs(400)
                .bodyFieldNotNull("error")
                .responseTimeUnder(3000);
    }

    @Test(groups = {"regression"},
          description = "First trader record contains required fields: account, protocol, type=FULL, lastTradeAtTs")
    @Story("Search Trader — Response Schema")
    @Severity(SeverityLevel.CRITICAL)
    public void shouldReturnValidTraderRecordSchema() {
        Response response = searchTraderService.searchByKeyword(HEX_ACCOUNT_PREFIX, 1, ALL_PROTOCOLS);

        ApiValidator.assertThat(response)
                .statusCodeIs(200)
                .responseTimeUnder(3000);

        String base = "data[0]";
        assertThat(response.jsonPath().getString(base + ".account"))
                .as("account").isNotBlank();
        assertThat(response.jsonPath().getString(base + ".protocol"))
                .as("protocol").isIn(ALL_PROTOCOLS);
        assertThat(response.jsonPath().getString(base + ".type"))
                .as("type").isEqualTo("FULL");
        assertThat(response.jsonPath().<Long>getObject(base + ".lastTradeAtTs", Long.class))
                .as("lastTradeAtTs").isNotNull().isPositive();
    }
}
