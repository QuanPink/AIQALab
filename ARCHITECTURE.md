# ARCHITECTURE.md

AIQALab has two architectural layers that enforce the same principles: **separation of concerns, business-driven naming, and domain-agnostic design**.

- **Test Architecture** — how Java test code is structured and layered
- **Command Architecture** — how 22 AI commands work together as a system

---

## 1. API Test Architecture

```
┌─────────────────────────────────────────────────────────┐
│  Test Class                                              │
│  - Assertions and test logic ONLY                        │
│  - Calls Service — NEVER ApiClient directly              │
│  - @Epic, @Feature, @Story, @Severity annotations        │
│  - 5 groups: Positive, Negative, Edge,                   │
│    Cross-cutting, Security                               │
├─────────────────────────────────────────────────────────┤
│  Service Class                                           │
│  - Builds request params from business arguments         │
│  - Calls ApiClient, returns Response                     │
│  - NO assertions, NO test logic                          │
├─────────────────────────────────────────────────────────┤
│  ApiClient                                               │
│  - Maps to one HTTP endpoint                             │
│  - Handles auth, headers, transport                      │
│  - Endpoint-based naming acceptable here                 │
├─────────────────────────────────────────────────────────┤
│  Params / Models                                         │
│  - Request structure and response shape                  │
│  - Endpoint-based naming acceptable here                 │
└─────────────────────────────────────────────────────────┘
```

### Layer responsibilities

| Layer | Must contain | Must NOT contain | Naming rule |
|---|---|---|---|
| Test class | Assertions, `@Test` methods, Allure annotations | HTTP calls, param building | Business-driven (MUST) |
| Service class | Param construction, `ApiClient` call, return `Response` | Assertions, `@Test` | Business-driven (MUST) |
| ApiClient | HTTP transport, auth headers, filters | Assertions, business logic | Endpoint-based (OK) |
| Params / Models | Request fields, response fields | Logic of any kind | Endpoint-based (OK) |

### Naming examples

| Layer | Example |
|---|---|
| Test class | `SearchTraderExplorerApiTest` |
| Service class | `SearchTraderExplorerService` |
| ApiClient | `PositionStatisticsApiClient` |
| Params | `PositionStatSearchVariables` |

### File locations (from `shared-qa-rules`)

```
src/main/java/com/framework/api/client/    ← ApiClients
src/main/java/com/framework/api/model/     ← Request/response models
src/main/java/com/framework/api/service/   ← Services
src/test/java/com/framework/api/           ← API test classes
```

---

## 2. UI Test Architecture

```
┌─────────────────────────────────────────────────────────┐
│  Test Class                                              │
│  - Assertions ONLY                                       │
│  - Calls Page Object methods — NEVER driver directly     │
├─────────────────────────────────────────────────────────┤
│  Page Object (extends BasePage)                          │
│  - Private locators                                      │
│  - Business-driven method names                          │
│  - Fluent: return this (same page) or DestPage (nav)     │
│  - NO assertions                                         │
├─────────────────────────────────────────────────────────┤
│  WebDriver (via WaitHelper)                              │
│  - All interactions through explicit waits               │
│  - Never Thread.sleep()                                  │
└─────────────────────────────────────────────────────────┘
```

Use `clearAndType()` instead of `type()` for React inputs (sends Ctrl+A first). Standard wait: 15 s. Short wait: 5 s.

```
src/main/java/com/framework/ui/pages/    ← Page Objects
src/test/java/com/framework/ui/          ← UI test classes
```

---

## 3. Command System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  shared-qa-rules.md  (single source of truth)               │
│  - 6 global rules (naming, separation, consistency…)        │
│  - 5 standard scenario groups                               │
│  - Framework paths reference                                │
│  - Anti-patterns table                                      │
│  - Artifact output convention                               │
├─────────────────────────────────────────────────────────────┤
│  22 Command Files  (.claude/commands/qa-*.md)               │
│  - Each references shared-qa-rules                          │
│  - Each defines: Purpose → Phases → Output → Anti-patterns  │
│  - Produces artifacts to .claude/artifacts/                 │
├─────────────────────────────────────────────────────────────┤
│  Artifact Chaining  (.claude/artifacts/)                    │
│  - brainstorm/ → test-plan/ → testcase/                     │
│  - failure-analysis/ → fix-test/ → review-test/             │
│  - Each command reads upstream artifacts, writes downstream  │
│  - Artifacts are optional — every command works standalone  │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. Full QA Workflow Pipeline

```
  Requirement
      │
      ▼
  /qa-analyze-requirement
      │
      ▼
  /qa-brainstorm-test  ──→  /qa-create-test-plan  ──→  /qa-generate-testcase
                                                              │
                                              ┌───────────────┘
                                              ▼
                              /qa-generate-api-test  /qa-generate-ui-test
                              /qa-generate-schema-test  /qa-generate-test-data
                                              │
                                              ▼
                                      /qa-review-test
                                              │
                                              ▼
                                        /qa-run-tests
                                              │
                               ┌─────────────┴─────────────┐
                               ▼                           ▼
                    /qa-analyze-failure           /qa-impact-analysis
                               │
                               ▼
                    /qa-analyze-regression
                    (REG / EXP / ENV / FLK / STL / UNK)
                               │
                    ┌──────────┴──────────┐
                    ▼                     ▼
              /qa-fix-test        (mark as EXP/ENV, skip)
                    │
                    ▼
             /qa-fix-and-verify
                    │
                    ▼
          /qa-health-score  ──→  /qa-generate-report
```

---

## 5. Directory Structure

```
AIQALab/
├── .claude/
│   ├── commands/                  ← 22 AI command files
│   │   ├── shared-qa-rules.md     ← single source of truth
│   │   └── qa-*.md                ← individual commands
│   └── artifacts/                 ← command output chaining
│       ├── brainstorm/
│       ├── test-plan/
│       ├── testcase/
│       ├── failure-analysis/
│       ├── regression-analysis/
│       ├── impact-analysis/
│       ├── review-report/
│       └── reports/
├── src/
│   ├── main/java/com/framework/
│   │   ├── api/
│   │   │   ├── client/            ← ApiClients (endpoint-based naming OK)
│   │   │   ├── service/           ← Services (business-driven naming)
│   │   │   └── model/             ← Request params + response models
│   │   ├── core/
│   │   │   ├── config/            ← ConfigManager (JVM prop > env var > .properties)
│   │   │   ├── driver/            ← DriverFactory + ThreadLocal DriverManager
│   │   │   ├── report/            ← Allure helpers + screenshot capture
│   │   │   └── retry/             ← RetryAnalyzer + RetryListener
│   │   ├── ui/
│   │   │   ├── base/              ← BasePage
│   │   │   └── pages/             ← LoginPage, HomePage, DashboardPage
│   │   ├── utils/                 ← WaitHelper, RandomDataUtils, JsonUtils
│   │   └── validator/             ← ApiValidator, UiValidator (fluent AssertJ)
│   └── test/
│       ├── java/com/framework/
│       │   ├── base/              ← BaseTest, BaseApiTest
│       │   ├── api/               ← API test classes
│       │   ├── ui/                ← UI test classes
│       │   └── data/constants/    ← Test data constants
│       └── resources/
│           ├── config/            ← config-dev/staging/prod.properties
│           └── testdata/          ← JSON fixtures, protocols.properties
├── README.md
├── ARCHITECTURE.md
├── CLAUDE.md
├── pom.xml
└── testng*.xml                    ← TestNG suite files
```

---

## 6. Design Decisions

| Decision | Rationale | Alternative rejected |
|---|---|---|
| Service layer is mandatory | Decouples test intent from transport; a service rename doesn't break tests | Test calls ApiClient directly — couples test to HTTP details |
| Schema tests separated from business tests | Schema change = different root cause than logic change | Inline schema assertions — masks root cause in failure analysis |
| 5 scenario groups, not 3 or 4 | Cross-cutting (auth, pagination, perf) and Security are distinct concerns consistently missed | Freeform grouping — inconsistent coverage across tests |
| Domain-agnostic enforcement | Commands reusable across any project; prevents platform lock-in | Domain-specific helpers — reduces portability |
| Format terms allowed, domain terms forbidden | Format = standard encoding (e.g., Base58); domain = platform name (e.g., Solana) | Blanket prohibition — blocks legitimate technical names |
| 6 regression categories (REG/EXP/ENV/FLK/STL/UNK) | Prevents wasting time fixing tests that should be updated (EXP) or dismissing real bugs as flaky (REG vs FLK) | Pass/fail only — no actionable signal for triage |
| Self-validating command system | `/qa-skill-validation` catches drift; prevents rule divergence as commands evolve | Manual review — inconsistent and slow |
| Artifact chaining via `.claude/artifacts/` | Commands feed each other without manual copy-paste; each still works standalone | Output only to stdout — breaks workflow continuity |

---

## 7. Quality Gates

| Gate | What it validates | Command |
|---|---|---|
| Code quality | Architecture compliance, naming, assertion placement, anti-patterns | `/qa-review-test` |
| Command quality | Structure, rule cross-references, domain terms, cross-command consistency | `/qa-skill-validation` |
| System health | Pass rate, coverage, stability, speed, debt → single 0–100 score | `/qa-health-score` |
