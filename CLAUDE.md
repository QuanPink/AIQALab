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
mvn test -Dtestng.suite=testng-api.xml -Dtest=SearchTraderApiTest

# Switch environment (default: dev)
mvn test -Denv=staging
mvn test -Denv=prod

# Override config at runtime
mvn test -Denv=staging -Dbrowser=firefox -Dapi.token=<token>

# Override a test data property at runtime
mvn test -Denv=dev -Dtest=SearchTraderApiTest -Dtest.trader.id=<known-id>

# Generate/serve Allure report
mvn allure:report
mvn allure:serve
```

## Architecture

Java/Maven test automation framework for UI (Selenium) and API (REST Assured/GraphQL) testing against `https://app.copin.io` (UI) and `https://api.copin.io` (API).

### Layer Hierarchy

```
Test Classes (LoginTest, SearchTraderApiTest, SearchPositionByTransactionApiTest)
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

- `searchPositionStatistic` → `data[].id` is always `null` by design. Use composite key `account|protocol|type` to identify records uniquely.
- `searchPositionStatistic` endpoint is **public** — unauthenticated requests return 200 with data (not 401/403).
- Numeric fields such as `totalTrade` may be `null`; use `getObject(path, Integer.class)` instead of `getInt(path)` to avoid `NullPointerException`.
- `/api/v1/users` endpoint does not exist on the live server — `UserApiTest` failures are expected.

### Test Data

- `src/test/resources/testdata/users.json` — static user fixtures
- `src/test/resources/testdata/login_test_data.json` — invalid email formats, security payloads, boundary values, rate-limit config
- `src/test/resources/testdata/protocols.properties` — protocol lists loaded via `ProtocolTestData` (`getSubsetProtocols()`, `getAllProtocols()`, `getBase58ProtocolFilter()`, `getBech32ProtocolFilter()`)
- `RandomDataUtils.uniqueEmail()` etc. for dynamic data in API tests
- Lookup identifiers use the fallback pattern: `System.getProperty("test.<key>", "fallback-not-found")` — when the fallback fires, the test must assert not-found behavior rather than assume the entity exists

### Reporting

Allure annotations (`@Epic`, `@Feature`, `@Story`, `@Step`) are used throughout. Screenshots are captured automatically on failure. Environment info is attached at suite start via `@BeforeSuite`. Logs go to `target/logs/` (rolling, 10 MB, 5 files max).

---

## QA Command Framework

This repo ships 23 AI-assisted QA commands in `.claude/commands/`. They are invoked as slash commands (e.g. `/qa-generate-api-test`, `/qa-review-test`) and all rules are defined in `.claude/commands/shared-qa-rules.md` — that file is the single source of truth.

### Core Rules (enforced by all commands)

- **Business-driven naming**: class, method, and service names must reflect business behavior — never endpoint paths or backend field names
- **Layered architecture**: `Test → Service → ApiClient → Params` — tests call Service only, never ApiClient directly
- **Domain-agnostic**: no protocol/platform names in test code (EVM, Solana, copin, binance). Format terms (Hex, Base58, Bech32) are acceptable
- **One API per test class**: never mix multiple endpoints in one test class
- **5 scenario groups**: Positive, Negative, Edge Cases, Cross-cutting, Security
- **Test method naming**: `should<Behavior>[When<Condition>]()`

### Command → Artifact Mapping

Every command that produces output saves a manifest to `.claude/artifacts/<name>/latest.md` for downstream workflow chaining. Key commands:

| Command | Purpose |
|---------|---------|
| `/qa-generate-api-test` | Generate complete Java API test class + Service + ApiClient |
| `/qa-generate-ui-test` | Generate complete Java UI test class + Page Object |
| `/qa-review-test` | Audit test file compliance against shared-qa-rules |
| `/qa-fix-test` | Diagnose and fix failing tests |
| `/qa-analyze-failure` | Root cause analysis on surefire report failures |
| `/qa-analyze-regression` | Classify failures as REG/EXP/ENV/FLK/STL/UNK |
| `/qa-health-score` | Calculate 0–100 test health score |
| `/qa-skill-validation` | Validate the command files themselves |

### Naming Conventions (quick reference)

| Artifact | Pattern | Example |
|----------|---------|---------|
| API test class | `<Verb><Entity>[Qualifier]ApiTest` | `SearchTraderApiTest` |
| UI test class | `<BusinessFlow>Test` | `AuthenticateUserTest` |
| Service class | `<Verb><Entity>Service` | `SearchTraderService` |
| Test method | `should<Behavior>[When<Condition>]()` | `shouldReturnEmptyWhenNoDataFound()` |
