package com.framework.api;

import com.framework.api.service.SearchTraderExplorerService;
import com.framework.base.BaseApiTest;
import com.framework.data.constants.ProtocolTestData;
import com.framework.validator.ApiValidator;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.framework.api.client.PositionStatisticsApiClient.ALL_PROTOCOLS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API tests for the Trader Explorer page — {@code searchPositionStatistic} GraphQL operation.
 *
 * <p>This endpoint powers the Trader Explorer page, returning a paginated list of trader statistics
 * filtered by period type and protocol. Each record represents aggregated trading performance
 * for a single trader over the requested period.
 *
 * <p>Endpoint: {@code POST {api.base.url}/graphql}
 * <br>Auth: Raw JWT in {@code authorization} header (no "Bearer" prefix).
 */
@Epic("Trading Platform API")
@Feature("Trader Explorer")
public class SearchTraderExplorerApiTest extends BaseApiTest {

    private SearchTraderExplorerService searchTraderExplorerService;

    // ─────────────────────────────────────────────────────────────────────

    @BeforeClass
    public void setUpClient() {
        searchTraderExplorerService = new SearchTraderExplorerService();
    }

    // ── Smoke — default D30 filter ────────────────────────────────────────

    @Test(groups = {"smoke", "regression"},
          description = "Search all protocols D30 — returns 200 with non-empty data list")
    @Story("Search Trader Explorer")
    @Severity(SeverityLevel.BLOCKER)
    public void shouldReturnDataWhenSearchingAllProtocolsWithDefaultFilter() {
        Response response = searchTraderExplorerService.searchByPeriod(ALL_PROTOCOLS, "D30", 20, 0);

        ApiValidator.assertThat(response)
                .statusCodeIs(200)
                .bodyFieldNotNull("data.searchPositionStatistic")
                .bodyFieldNotNull("data.searchPositionStatistic.data")
                .bodyFieldNotNull("data.searchPositionStatistic.meta")
                .responseTimeUnder(3000);

        // Verify GraphQL returned no errors
        assertThat((Object) response.jsonPath().get("errors")).as("GraphQL errors").isNull();

        // Verify data is non-empty
        List<?> data = response.jsonPath().getList("data.searchPositionStatistic.data");
        assertThat(data).as("trader statistics list").isNotEmpty();

        // Verify meta total > 0
        Integer total = response.jsonPath().getInt("data.searchPositionStatistic.meta.total");
        assertThat(total).as("meta.total").isGreaterThan(0);
    }

    // ── D7 period type ────────────────────────────────────────────────────

    @Test(groups = {"regression"},
          description = "Search all protocols D7 — returns 200 with correct type filter applied")
    @Story("Search Trader Explorer")
    @Severity(SeverityLevel.CRITICAL)
    public void shouldReturnDataForD7PeriodType() {
        Response response = searchTraderExplorerService.searchByPeriod(ALL_PROTOCOLS, "D7", 10, 0);

        ApiValidator.assertThat(response)
                .statusCodeIs(200)
                .bodyFieldNotNull("data.searchPositionStatistic.data")
                .responseTimeUnder(3000);

        assertThat((Object) response.jsonPath().get("errors")).as("GraphQL errors").isNull();
        assertThat(response.jsonPath().getString("data.searchPositionStatistic.data[0].type"))
                .as("returned record type must match requested period D7")
                .isEqualTo("D7");
    }

    // ── Pagination — size=5 ───────────────────────────────────────────────

    @Test(groups = {"regression"},
          description = "Pagination size=5 — returns at most 5 records in data array")
    @Story("Search Trader Explorer — Pagination")
    @Severity(SeverityLevel.CRITICAL)
    public void shouldReturnAtMostRequestedPageSize() {
        Response response = searchTraderExplorerService.searchByPeriod(ALL_PROTOCOLS, "D30", 5, 0);

        ApiValidator.assertThat(response)
                .statusCodeIs(200)
                .responseTimeUnder(3000);

        List<?> data = response.jsonPath().getList("data.searchPositionStatistic.data");
        assertThat(data).as("data size").hasSizeLessThanOrEqualTo(5);
    }

    // ── Pagination — page 2 (offset) ──────────────────────────────────────

    @Test(groups = {"regression"},
          description = "Pagination offset=20 — second page returns distinct records")
    @Story("Search Trader Explorer — Pagination")
    @Severity(SeverityLevel.NORMAL)
    public void shouldReturnDistinctRecordsOnSecondPage() {
        // id is null by API design — use composite key (account|protocol|type) to identify records
        List<Map<String, String>> rawPage1 = searchTraderExplorerService
                .searchByPeriod(ALL_PROTOCOLS, "D30", 20, 0)
                .jsonPath().getList("data.searchPositionStatistic.data");
        List<Map<String, String>> rawPage2 = searchTraderExplorerService
                .searchByPeriod(ALL_PROTOCOLS, "D30", 20, 20)
                .jsonPath().getList("data.searchPositionStatistic.data");

        List<String> keysPage1 = rawPage1.stream()
                .map(r -> r.get("account") + "|" + r.get("protocol") + "|" + r.get("type"))
                .collect(java.util.stream.Collectors.toList());
        List<String> keysPage2 = rawPage2.stream()
                .map(r -> r.get("account") + "|" + r.get("protocol") + "|" + r.get("type"))
                .collect(java.util.stream.Collectors.toList());

        // Pages must not overlap
        assertThat(keysPage1).as("page1 composite keys must be non-empty").isNotEmpty();
        assertThat(keysPage2)
                .as("page2 must be non-empty and not overlap with page1")
                .isNotEmpty()
                .doesNotContainAnyElementsOf(keysPage1);
    }

    // ── Sort ascending ────────────────────────────────────────────────────

    @Test(groups = {"regression"},
          description = "Sort realisedPnl asc — returns 200 with valid response")
    @Story("Search Trader Explorer — Sorting")
    @Severity(SeverityLevel.NORMAL)
    public void shouldReturnResultsWhenSortedAscending() {
        Response response = searchTraderExplorerService
                .searchWithSort(ALL_PROTOCOLS, "D30", "realisedPnl", "asc", 20, 0);

        ApiValidator.assertThat(response)
                .statusCodeIs(200)
                .bodyFieldNotNull("data.searchPositionStatistic.data")
                .responseTimeUnder(3000);

        List<Double> pnlValues = response.jsonPath()
                .getList("data.searchPositionStatistic.data.realisedPnl", Double.class);
        assertThat(pnlValues)
                .as("realisedPnl values must be sorted in ascending order")
                .isNotEmpty()
                .isSortedAccordingTo(Comparator.naturalOrder());
    }

    // ── No auth token ──────────────────────────────────────────────────────

    @Test(groups = {"regression"},
          description = "Request without auth token — API is public, returns 200 with valid data")
    @Story("Search Trader Explorer — Authentication")
    @Severity(SeverityLevel.NORMAL)
    public void shouldAllowPublicAccessWithoutToken() {
        Response response = searchTraderExplorerService.searchPublic(ALL_PROTOCOLS, "D30", 5, 0);

        // This GraphQL endpoint is publicly accessible — no auth token required
        ApiValidator.assertThat(response)
                .statusCodeIs(200)
                .bodyFieldNotNull("data.searchPositionStatistic")
                .responseTimeUnder(3000);
    }

    // ── Parametrized — multiple period types ──────────────────────────────

    @Test(groups = {"regression"}, dataProvider = "allSupportedPeriodTypes",
          description = "Search with different period types — all return 200")
    @Story("Search Trader Explorer — Period Types")
    @Severity(SeverityLevel.NORMAL)
    public void shouldReturnDataForAllSupportedPeriodTypes(String type) {
        ApiValidator.assertThat(searchTraderExplorerService.searchByPeriod(ALL_PROTOCOLS, type, 10, 0))
                .statusCodeIs(200)
                .bodyFieldNotNull("data.searchPositionStatistic")
                .responseTimeUnder(3000);
    }

    @DataProvider(name = "allSupportedPeriodTypes", parallel = true)
    public Object[][] allSupportedPeriodTypes() {
        return new Object[][]{
            {"L24H"},
            {"D1"},
            {"D7"},
            {"D14"},
            {"D30"},
            {"D60"},
            {"FULL"}
        };
    }

    // ── Negative — invalid inputs ─────────────────────────────────────────

    @Test(groups = {"regression"},
          description = "Invalid period type — GraphQL returns error node in response")
    @Story("Search Trader Explorer — Validation")
    @Severity(SeverityLevel.NORMAL)
    public void shouldReturnGraphQlErrorForInvalidPeriodType() {
        Response response = searchTraderExplorerService.searchByPeriod(ALL_PROTOCOLS, "INVALID", 10, 0);

        ApiValidator.assertThat(response)
                .statusCodeIs(200)
                .responseTimeUnder(3000);

        assertThat((Object) response.jsonPath().get("errors"))
                .as("GraphQL errors must be present for invalid period type")
                .isNotNull();
    }

    @Test(groups = {"regression"},
          description = "Empty protocol list — API handles gracefully without server error")
    @Story("Search Trader Explorer — Validation")
    @Severity(SeverityLevel.NORMAL)
    public void shouldReturnGraphQlErrorForEmptyProtocolList() {
        Response response = searchTraderExplorerService.searchByPeriod(List.of(), "D30", 10, 0);

        ApiValidator.assertThat(response)
                .statusCodeIs(200)
                .responseTimeUnder(3000);

        assertThat((Object) response.jsonPath().get("errors"))
                .as("GraphQL errors must be present for empty protocol list")
                .isNotNull();
    }

    // ── Subset-protocol validation ─────────────────────────────────────────

    @Test(groups = {"regression"},
          description = "Filter with configured protocol subset — returns only records from that subset")
    @Story("Search Trader Explorer — Protocol Filter")
    @Severity(SeverityLevel.NORMAL)
    public void shouldReturnOnlyRecordsMatchingProtocolSubset() {
        Response response = searchTraderExplorerService.searchByPeriod(ProtocolTestData.getSubsetProtocols(), "D30", 20, 0);

        ApiValidator.assertThat(response)
                .statusCodeIs(200)
                .bodyFieldNotNull("data.searchPositionStatistic.data")
                .responseTimeUnder(3000);

        List<String> returnedProtocols = response.jsonPath()
                .getList("data.searchPositionStatistic.data.protocol");
        assertThat(returnedProtocols)
                .as("subset filter must return at least one record and every protocol must be in the requested subset")
                .isNotEmpty()
                .allMatch(ProtocolTestData.getSubsetProtocols()::contains);
    }

    // ── Response field integrity ───────────────────────────────────────────

    @Test(groups = {"regression"},
          description = "First record contains all required fields (account, protocol, type)")
    @Story("Search Trader Explorer — Response Schema")
    @Severity(SeverityLevel.CRITICAL)
    public void shouldReturnRecordWithRequiredFields() {
        Response response = searchTraderExplorerService.searchByPeriod(ALL_PROTOCOLS, "D30", 1, 0);

        ApiValidator.assertThat(response)
                .statusCodeIs(200)
                .responseTimeUnder(3000);

        // Note: `id` is null by API design for this index — not validated here
        String basePath = "data.searchPositionStatistic.data[0]";
        assertThat(response.jsonPath().getString(basePath + ".account"))
                .as("account").isNotBlank();
        assertThat(response.jsonPath().getString(basePath + ".protocol"))
                .as("protocol").isIn(ALL_PROTOCOLS);
        assertThat(response.jsonPath().getString(basePath + ".type"))
                .as("type").isEqualTo("D30");
    }
}
