# AIQALab

**AI-Powered QA Platform — End-to-End Quality Engineering with AI**

AIQALab is an end-to-end AI-powered QA platform that transforms how quality engineering is done. It provides 22 intelligent commands that act as a virtual QA team — analyzing requirements, generating test strategies, writing test code with enforced architecture, executing tests, classifying failures, auto-fixing with verification, and reporting health metrics. Whether you're a manual QA engineer or automation specialist, AIQALab gives you AI-powered superpowers across the entire QA lifecycle.

---

## Why AIQALab

**Traditional QA** is repetitive, slow, and inconsistent — writing test cases manually, debugging flaky tests, classifying failures one by one, and producing reports that take longer than the tests themselves.

**Existing AI tools** can generate code, but they don't enforce your architecture, don't validate the quality of what they generate, and don't cover the full testing lifecycle. You get a code snippet, not a quality system.

**AIQALab** takes a different approach: AI acts as your QA team, not just a code generator. It plans, generates, reviews, executes, analyzes, fixes, and reports — covering the full lifecycle with consistent quality standards enforced at every step.

---

## What Makes It Different

- **Full lifecycle coverage** — 22 commands spanning requirement analysis to health reporting, not just test generation
- **Architecture enforcement** — AI generates code following `Test → Service → ApiClient → Params` and reviews it against the same rules
- **Self-validating system** — `/qa-skill-validation` audits the command files themselves for quality and consistency
- **Domain-agnostic** — reusable across any project; no business-domain coupling baked in
- **For everyone** — manual QA engineers use planning and analysis commands; automation engineers use generation and execution commands
- **Health Score** — a single 0–100 metric that translates test suite quality into language stakeholders understand

---

## Platform Overview

```
ANALYZE          PLAN              GENERATE            EXECUTE
─────────────    ──────────────    ─────────────────   ──────────────
analyze-req      brainstorm-test   generate-api-test   run-tests
                 create-test-plan  generate-ui-test    setup-environment
                 generate-testcase generate-page-obj
                                   generate-schema
                                   generate-test-data
                                   generate-ci-config

ANALYZE RESULTS                    REPORT
────────────────────────────────   ──────────────────────────────────
analyze-failure  fix-test           generate-report     skill-validation
analyze-regress  fix-and-verify     health-score        shared-qa-rules
impact-analysis  review-test
```

---

## Command Reference

### Planning & Analysis (4)

| Command | What it does |
|---|---|
| `/qa-analyze-requirement` | Analyze feature requirements from a QA perspective — identify risks, ambiguities, and coverage gaps |
| `/qa-brainstorm-test` | Generate comprehensive test scenarios across 5 groups: happy path, edge case, negative, security, performance |
| `/qa-create-test-plan` | Organize brainstormed scenarios into a structured, prioritized test plan |
| `/qa-generate-testcase` | Write detailed test case specifications with preconditions, steps, and expected results |

### Code Generation (6)

| Command | What it does |
|---|---|
| `/qa-generate-api-test` | Generate a complete API test class following the layered architecture (Test → Service → ApiClient → Params) |
| `/qa-generate-ui-test` | Generate a UI test class using the Page Object pattern with proper base class inheritance |
| `/qa-generate-page-object` | Generate a reusable Page Object with fluent methods and explicit waits via `WaitHelper` |
| `/qa-generate-schema-test` | Generate response schema validation tests for contract testing |
| `/qa-generate-test-data` | Generate the full test data layer: constants, factory classes, and TestNG DataProviders |
| `/qa-generate-ci-config` | Generate CI/CD pipeline configuration (GitHub Actions, Jenkins, or GitLab CI) |

### Execution & Analysis (5)

| Command | What it does |
|---|---|
| `/qa-run-tests` | Execute tests with specified suite/environment and produce a structured summary |
| `/qa-analyze-failure` | Root cause analysis of test failures — distinguishes code bugs, env issues, and flakiness |
| `/qa-analyze-regression` | Classify failures as `REG` / `EXP` / `ENV` / `FLK` / `STL` / `UNK` for triage |
| `/qa-impact-analysis` | Diff-aware targeted test selection — identify which tests to run given recent code changes |
| `/qa-health-score` | Calculate a 0–100 health score across coverage, stability, maintainability, and speed dimensions |

### Fix & Review (3)

| Command | What it does |
|---|---|
| `/qa-fix-test` | Diagnose and fix a specific failing test with a clear explanation of the root cause |
| `/qa-fix-and-verify` | Complete find-fix-verify cycle: fix the failure, run verification, and add a regression guard |
| `/qa-review-test` | Audit test code quality across 5 dimensions: architecture, assertions, readability, coverage, maintainability |

### Reporting & Setup (2)

| Command | What it does |
|---|---|
| `/qa-generate-report` | Generate test metrics report from execution results in Allure or custom format |
| `/qa-setup-environment` | Environment setup guide with dependency checks and configuration diagnostics |

### Platform Meta (2)

| Command | What it does |
|---|---|
| `/qa-skill-validation` | Validate command files themselves for quality and consistency — the system audits itself |
| `/shared-qa-rules` | Single source of truth for all QA architecture rules, naming conventions, and standards |

---

## Architecture

### Test Layer Architecture

```
Test Classes          (LoginTest, SearchTraderApiTest, ...)
      ↓
Service Layer         (SearchTraderService, SearchPositionByTransactionService, ...)
      ↓
API Client Layer      (BaseApiClient → PositionStatisticsApiClient, ...)
      ↓
Params / Models       (GraphQLRequest, PositionStatFilterParams, ...)
```

Each layer has a single responsibility. Test classes express intent. Services compose operations. Clients handle transport. Params hold request structure. AI-generated code follows this hierarchy — `/qa-review-test` audits generated code against it.

### Command Architecture

```
shared-qa-rules.md  ──→  22 command files  ──→  .claude/artifacts/
(single source of truth)   (inherit rules)        (generated output)
```

### Framework Core

```
src/main/java/com/framework/
├── api/
│   ├── client/       BaseApiClient + domain clients
│   ├── model/        request params + response models
│   └── service/      business-level API operations
├── core/
│   ├── config/       ConfigManager (JVM prop > env var > properties file)
│   ├── driver/       DriverFactory + ThreadLocal DriverManager
│   ├── report/       Allure helpers + screenshot capture
│   └── retry/        RetryAnalyzer + RetryListener
├── ui/
│   ├── base/         BasePage with WaitHelper integration
│   └── pages/        LoginPage, HomePage, DashboardPage
├── utils/            JsonUtils, RandomDataUtils, WaitHelper
└── validator/        ApiValidator + UiValidator (fluent AssertJ wrappers)
```

See `CLAUDE.md` for detailed architecture decisions and known API behaviors.

---

## Quick Start

**Prerequisites:** Java 11+, Maven 3.6+, [Claude Code](https://claude.ai/code) installed

```bash
# 1. Clone
git clone https://github.com/QuanPink/AIQALab.git
cd AIQALab

# 2. Build
mvn clean compile

# 3. Run tests
mvn test                                    # full suite (dev env)
mvn test -Dtestng.suite=testng-api.xml      # API tests only
mvn test -Denv=staging                      # against staging

# 4. View report
mvn allure:serve
```

**Example AI-assisted workflow:**

```
/qa-analyze-requirement User login with email and password
/qa-brainstorm-test User login flow
/qa-generate-api-test POST /api/v1/auth/login — returns {token, user}
/qa-review-test LoginApiTest.java
/qa-run-tests smoke dev
/qa-health-score
```

---

## For Manual QA Engineers

You don't need to write a single line of code to get value from AIQALab. These commands work entirely at the planning and analysis level:

| Command | Use case |
|---|---|
| `/qa-analyze-requirement` | Paste a user story or spec — get a risk-based QA analysis |
| `/qa-brainstorm-test` | Describe a feature — get 20+ test scenarios across all test types |
| `/qa-create-test-plan` | Turn scenarios into a structured, prioritized plan ready for a sprint |
| `/qa-generate-testcase` | Expand scenarios into detailed test cases with full steps |
| `/qa-analyze-regression` | Paste a failure list — get each failure classified with recommended action |
| `/qa-generate-report` | Generate a stakeholder-ready metrics report from your test run data |

---

## For Automation QA Engineers

Advanced commands for building, maintaining, and scaling a test automation suite:

| Command | Use case |
|---|---|
| `/qa-generate-api-test` | Generate production-ready API test class with correct layering |
| `/qa-generate-ui-test` | Generate Selenium test class with Page Object wiring |
| `/qa-review-test` | Audit any test file across 5 quality dimensions before merge |
| `/qa-fix-and-verify` | Auto-fix a failing test, verify it passes, and add a regression guard |
| `/qa-impact-analysis` | Given a diff, identify exactly which tests need to run |
| `/qa-generate-schema-test` | Generate contract tests to catch API breaking changes early |

---

## Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Test Framework | TestNG | 7.9.0 |
| UI Automation | Selenium | 4.18.1 |
| Driver Management | WebDriverManager | 5.8.0 |
| API Testing | REST Assured | 5.4.0 |
| Reporting | Allure | 2.25.0 |
| Assertions | AssertJ | 3.25.3 |
| Serialization | Jackson | 2.17.0 |
| Logging | Log4j2 | 2.23.1 |
| Boilerplate | Lombok | 1.18.32 |
| Build | Maven | 3.6+ |
| Language | Java | 11 |

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Run command validation before committing: `/qa-skill-validation all`
4. Submit a pull request with a clear description of your changes

---

## License

MIT

---

## Acknowledgments

- Built with [Claude Code](https://claude.ai/code) by Anthropic
- Inspired by gstack's cognitive mode switching approach to AI-assisted development
- Designed for QA engineers who believe AI should augment, not replace, human judgment
