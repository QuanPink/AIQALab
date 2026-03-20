# Shared QA Rules

This file defines rules that apply to **all QA commands** in this framework.
Every command that references this file MUST enforce all rules below.

---

## Global QA Rules (MUST FOLLOW)

### 1. Business-driven naming

- Class, method, and service names MUST reflect business behavior
- MUST NOT depend on:
    - endpoint path
    - backend API naming
    - technical field names (id, code, key…)

### 2. Domain-agnostic

- Do NOT assume any specific domain (crypto, ecommerce, fintech…)
- Do NOT hardcode domain-specific formats or naming
- Do NOT assume response format — verify from actual API behavior

### 3. API is an implementation detail

- Endpoint is ONLY used for reference (in comments)
- MUST NOT be used for naming classes, methods, or services

### 4. Separation of concerns (STRICT)

- If multiple APIs or flows are detected:
  → MUST split into multiple test classes
  → DO NOT mix test logic across APIs or flows

### 5. Consistency

- All test methods MUST follow this naming pattern:

```
should<Behavior>[When<Condition>]()
```

Examples:
- `shouldReturnResultsForValidInput()`
- `shouldReturnEmptyWhenNoDataFound()`
- `shouldReturnErrorWhenRequiredParamMissing()`
- `shouldFilterResultsCorrectly()`
- `shouldRespectPaginationLimit()`

### 6. No cross-API reuse

- DO NOT reuse logic across different APIs if behavior differs
- Each API must be validated based on its own behavior

---

## Standard Scenario Groups

All scenario and test outputs MUST use exactly these 5 groups:

| # | Group | Description |
|---|-------|-------------|
| 1 | **Positive / Happy Path** | Valid inputs, expected success behavior |
| 2 | **Negative / Error Handling** | Invalid inputs, system rejects gracefully |
| 3 | **Edge Cases** | Boundary conditions, unusual inputs, empty states |
| 4 | **Cross-cutting** | Auth, pagination, schema validation, response time (< 3000ms) |
| 5 | **Security** (if applicable) | XSS, injection, auth bypass, IDOR, data exposure |

> **Note:** Performance scenarios (response time, large datasets) belong to **Cross-cutting**, not a separate group.

---

## Framework Paths Reference

| Artifact | Path |
|----------|------|
| API Clients (production) | `src/main/java/com/framework/api/client/` |
| Request models (production) | `src/main/java/com/framework/api/model/` |
| Service classes (production) | `src/main/java/com/framework/api/service/` |
| Page Objects (production) | `src/main/java/com/framework/ui/pages/` |
| API Tests | `src/test/java/com/framework/api/` |
| UI Tests | `src/test/java/com/framework/ui/` |
| Config | `src/test/resources/config/config-{env}.properties` |
| Test data | `src/test/resources/testdata/` |

---

## Artifact Output Convention

Commands in this framework can produce artifacts (output files) that other commands consume. This enables automated workflow chaining without manual copy-paste.

### Directory Structure
```
.claude/artifacts/
├── brainstorm/          ← output from /qa-brainstorm-test
├── test-plan/           ← output from /qa-create-test-plan
├── testcase/            ← output from /qa-generate-testcase
├── review-report/       ← output from /qa-review-test
├── failure-analysis/    ← output from /qa-analyze-failure
├── regression-analysis/ ← output from /qa-analyze-regression
├── impact-analysis/     ← output from /qa-impact-analysis
├── reports/             ← output from /qa-generate-report
├── fix-test/            ← output from /qa-fix-test
├── fix-verify/          ← output from /qa-fix-and-verify
├── skill-validation/    ← output from /qa-skill-validation
├── health-score/        ← output from /qa-health-score
├── docs-update/         ← output from /qa-update-docs
├── requirement-analysis/ ← output from /qa-analyze-requirement
├── generated-api-test/  ← output from /qa-generate-api-test
├── generated-ui-test/   ← output from /qa-generate-ui-test
├── generated-page-object/ ← output from /qa-generate-page-object
├── generated-ci-config/ ← output from /qa-generate-ci-config
├── generated-schema-test/ ← output from /qa-generate-schema-test
├── generated-test-data/ ← output from /qa-generate-test-data
├── setup-environment/   ← output from /qa-setup-environment
└── run-tests/           ← output from /qa-run-tests
```

### Rules

1. **Write rule:** After generating output, save a copy to `.claude/artifacts/<command-name>/latest.md`
2. **Read rule:** Before generating, check `.claude/artifacts/` for output from upstream commands. If found, use it as input automatically.
3. **Naming:** Always overwrite `latest.md`. Keep previous runs as `<date>-<time>.md` if history is needed.
4. **No dependency:** Artifacts are optional input. Every command MUST still work with `$ARGUMENTS` alone — artifacts are a convenience, not a requirement.

---

## Safety Warnings

Commands that modify code or files MUST warn before destructive actions:

| Action | Warning Required | Commands Affected |
|--------|-----------------|-------------------|
| Deleting test file | "This will permanently remove test coverage for [entity]. Confirm?" | qa-fix-test, qa-fix-and-verify |
| Overwriting Service class | "Service class already exists. Overwrite will lose current implementation. Confirm?" | qa-generate-api-test |
| Overwriting Page Object | "Page Object already exists. Overwrite will lose current locators. Confirm?" | qa-generate-page-object, qa-generate-ui-test |
| Running tests on prod | "You are about to run tests on PRODUCTION environment. Confirm?" | qa-run-tests |
| Clearing artifacts | "This will delete all artifacts in .claude/artifacts/. Confirm?" | manual action |
| Regenerating test class | "Test class exists with [N] test methods. Regenerate will replace all. Confirm?" | qa-generate-api-test, qa-generate-ui-test |

Commands that only READ or REPORT (`qa-review-test`, `qa-analyze-failure`, `qa-skill-validation`, `qa-health-score`) do NOT require warnings.

---

## Anti-patterns (STRICTLY FORBIDDEN)

| ❌ Forbidden                              | ✔ Required                        |
| ----------------------------------------- | --------------------------------- |
| Naming based on endpoint                  | Business-driven naming            |
| Domain hardcoding                         | Domain-agnostic design            |
| Test names like TC001, TC002              | Behavior-driven method names      |
| Scattered assertions across layers        | Assertions only in test class     |
| Mixing multiple APIs in one test class    | One API per test class            |
| Test calling ApiClient directly           | Test calls Service only           |
| Vietnamese mixed into command content     | English only in all command files |
| Scenario IDs like S001, S002 in code      | Method names from `should<X>` pattern |

---

## Language Rule

All command file content MUST be written in **English only**.
Do not mix Vietnamese or any other language into instructions, comments, or examples.
