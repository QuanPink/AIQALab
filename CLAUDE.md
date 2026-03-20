# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Run all tests (default suite: testng.xml)
mvn test

# Run specific suite
mvn test -Dtestng.suite=testng-api.xml
mvn test -Dtestng.suite=testng-ui.xml
mvn test -Dtestng.suite=testng-smoke.xml

# Run a single test class
mvn test -Dtestng.suite=testng-api.xml -Dtest=PositionStatisticsApiTest

# Switch environment (default: dev)
mvn test -Denv=staging
mvn test -Denv=prod

# Override config at runtime
mvn test -Denv=staging -Dbrowser=firefox -Dapi.token=<token>

# Generate/serve Allure report
mvn allure:report
mvn allure:serve
```

## Architecture

Java/Maven test automation framework for UI (Selenium) and API (REST Assured/GraphQL) testing against `https://app.copin.io` (UI) and `https://api.copin.io` (API).

### Layer Hierarchy

```
Test Classes (LoginTest, UserApiTest, PositionStatisticsApiTest)
    ↓
Base Classes (BaseTest → BaseApiTest)
    ↓
Framework Core: ConfigManager | DriverManager | Page Objects | API Clients
    ↓
Utilities: WaitHelper | JsonUtils | RandomDataUtils | Validators
```

### Key Patterns

**Configuration** (`ConfigManager`): 3-tier resolution — JVM system property (`-Dprop=val`) > OS env var (dots→underscores, uppercase e.g. `api.token` → `API_TOKEN`) > `src/test/resources/config/config-{env}.properties`. Secrets (tokens) must use env vars; `api.token` is absent from all committed properties files.

**Driver Management**: `DriverFactory` creates WebDriver; `DriverManager` holds instances in `ThreadLocal` for thread-safe parallel execution. `BaseTest.setUpDriver()` / `tearDown()` manage the lifecycle. Override `needsDriver()` returning `false` to skip browser setup — `BaseApiTest` does this and is the correct base for all API test classes.

**Page Object Model**: All pages extend `BasePage`. Methods use fluent chaining and return page objects. Explicit waits are in every interaction via `WaitHelper` (15 s standard, 5 s short). Use `clearAndType()` instead of `type()` for React inputs (sends Ctrl+A before typing). `DashboardPage` exists but uses hardcoded CSS selectors; prefer `HomePage` for post-login assertions.

**API Clients**: All extend `BaseApiClient` which handles bearer token injection (`Authorization: Bearer {token}`) and Allure/console logging filters. `PositionStatisticsApiClient` overrides `buildRequest()` to send a **raw JWT without the "Bearer" prefix** — this is required by `api.copin.io/graphql`.

**Validators**: `ApiValidator` and `UiValidator` provide fluent assertion APIs wrapping AssertJ. Use these instead of raw assertions for descriptive failure messages and Allure step integration.

**Retry**: `RetryAnalyzer` + `RetryListener` (wired in all testng XML files). Max retries: 2 (dev/staging), 1 (prod). `RetryListener` applies the analyzer automatically via `IAnnotationTransformer` — no per-test annotation needed.

### Parallel Execution

- UI tests: method-level parallelism, 3 threads
- API tests: class-level parallelism, 6 threads
- Suite level: 4 threads

Never store driver or page objects as static fields — `ThreadLocal` isolation only works per-instance.

### Known API Behaviors (copin.io)

- `searchPositionStatistic` → `data[].id` is always `null` by design. Use composite key `account|protocol|type` to identify records uniquely (see `TC004`).
- `searchPositionStatistic` endpoint is **public** — unauthenticated requests return 200 with data (not 401/403).
- Numeric fields such as `totalTrade` may be `null`; use `getObject(path, Integer.class)` instead of `getInt(path)` to avoid `NullPointerException`.
- `/api/v1/users` endpoint does not exist on the live server — `UserApiTest` failures are expected.

### Test Data

- `src/test/resources/testdata/users.json` — static user fixtures
- `src/test/resources/testdata/login_test_data.json` — invalid email formats, security payloads, boundary values, rate-limit config
- `RandomDataUtils.uniqueEmail()` etc. for dynamic data in API tests

### Reporting

Allure annotations (`@Epic`, `@Feature`, `@Story`, `@Step`) are used throughout. Screenshots are captured automatically on failure. Environment info is attached at suite start via `@BeforeSuite`. Logs go to `target/logs/` (rolling, 10 MB, 5 files max).
