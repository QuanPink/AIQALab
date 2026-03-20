Analyze impact of recent code changes on existing tests: $ARGUMENTS

> This command follows rules defined in /shared-qa-rules. All rules in that file are mandatory.

---

## Purpose

This command reads git diff, maps changed files to affected test classes, and recommends which tests to run. It replaces full regression with targeted testing after code changes.

---

## Phase 0 — Input Analysis

| Input | Action |
|-------|--------|
| Blank or `latest` | Analyze uncommitted changes (`git diff`) + staged (`git diff --cached`) |
| Branch name | Analyze diff between current branch and main (`git diff main..<branch>`) |
| PR number or commit hash | Analyze that specific diff |
| `last-deploy` | Compare current HEAD with last deployed tag |

---

## Phase 1 — Change Detection

Run git diff and classify each changed file:

| Changed File Pattern | Category | Example |
|---------------------|----------|---------|
| `src/main/java/**/api/client/*.java` | API Client changed | `UserApiClient.java` |
| `src/main/java/**/api/service/*.java` | Service layer changed | `SearchUserService.java` |
| `src/main/java/**/api/model/*.java` | Request/response model changed | `SearchParams.java` |
| `src/main/java/**/ui/pages/*.java` | Page Object changed | `LoginPage.java` |
| `src/test/java/**/api/*.java` | API test changed directly | `SearchUserApiTest.java` |
| `src/test/java/**/ui/*.java` | UI test changed directly | `AuthenticateUserTest.java` |
| `src/test/resources/testdata/**` | Test data changed | `protocols.properties` |
| `src/test/resources/config/**` | Config changed | `config-staging.properties` |
| `pom.xml` | Dependency changed | — |
| Other | Non-test code | Application logic |

---

## Phase 2 — Impact Mapping

For each changed file, identify affected tests:

### 2.1 Direct Impact (file itself is a test)
```
Changed: SearchUserApiTest.java
→ Impact: SearchUserApiTest (ALL methods)
→ Action: Run full class
```

### 2.2 Upstream Impact (production code changed → tests depend on it)
```
Changed: UserApiClient.java
→ Find: Which Service classes call UserApiClient?
→ Find: Which Test classes use those Services?
→ Impact: ManageUserApiTest, SearchUserApiTest
→ Action: Run all affected test classes
```

### 2.3 Page Object Impact
```
Changed: LoginPage.java
→ Find: Which UI test classes use LoginPage?
→ Impact: AuthenticateUserTest, any test with LoginPage in @BeforeMethod
→ Action: Run all affected UI test classes
```

### 2.4 Config/Data Impact
```
Changed: config-staging.properties
→ Impact: ALL tests when running on staging
→ Action: Run smoke suite on staging to validate
```
```
Changed: testdata/entities.properties
→ Find: Which test classes load EntityTestData?
→ Impact: SearchEntityApiTest, ListEntityApiTest
→ Action: Run affected classes
```

### 2.5 Dependency Impact
```
Changed: pom.xml
→ Impact: Potentially all tests (dependency change)
→ Action: Run full regression
```

---

## Phase 3 — Test Selection

### 3.1 Impact Summary Table
```
| Changed File | Category | Affected Tests | Method Count |
|-------------|----------|---------------|-------------|
| UserApiClient.java | API Client | ManageUserApiTest | 7 |
| SearchUserService.java | Service | SearchUserApiTest | 12 |
| LoginPage.java | Page Object | AuthenticateUserTest | 5 |
| **Total** | | **3 classes** | **24 methods** |
```

### 3.2 Recommended Run Command
```bash
# Targeted run — only affected tests
mvn test -Denv=dev -Dtest=ManageUserApiTest,SearchUserApiTest,AuthenticateUserTest

# If config changed — smoke suite on affected env
mvn test -Denv=staging -Dsuite=testng-smoke.xml

# If pom.xml changed — full regression
mvn test -Denv=dev
```

### 3.3 Risk Assessment

| Risk Level | Condition | Recommendation |
|-----------|-----------|---------------|
| 🟢 Low | Only test files changed | Run changed tests only |
| 🟡 Medium | Service or ApiClient changed | Run affected tests + smoke suite |
| 🟠 High | Config or test data changed | Run smoke + affected tests on target env |
| 🔴 Critical | pom.xml or base classes changed | Full regression required |

---

## Phase 4 — Output

Save to `.claude/artifacts/impact-analysis/latest.md`
```
=== Impact Analysis Report ===
Date: [date]
Scope: [git diff description]
Files changed: [N]

Impact Summary:
| Risk | Affected Tests | Methods | Run Time (est.) |
|------|---------------|---------|----------------|
| 🟡 Medium | 3 classes | 24 methods | ~3 min |

Recommended command:
mvn test -Denv=dev -Dtest=ManageUserApiTest,SearchUserApiTest,AuthenticateUserTest

Full regression needed: No
```

### Generated Artifacts

| # | Artifact | When |
|---|----------|------|
| 1 | Impact analysis report | Always |
| 2 | Maven run command | Always |
| 3 | Risk assessment | Always |

---

## Anti-patterns (STRICTLY FORBIDDEN)

| ❌ Forbidden | ✔ Required |
|-------------|-----------|
| Running full regression for every small change | Targeted test selection based on diff |
| Guessing which tests are affected | Trace dependency chain from changed file to test class |
| Ignoring config/data file changes | Config changes can affect all tests on that environment |
| Skipping impact analysis for "small" PRs | Every PR gets impact analysis — small changes can break critical paths |