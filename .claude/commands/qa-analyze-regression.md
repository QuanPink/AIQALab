Analyze test failure as regression or expected change: $ARGUMENTS

> This command follows rules defined in /shared-qa-rules. All rules in that file are mandatory.

---

## Purpose

This command determines whether test failures after a code or API change are **true regressions** (something broke that should not have) or **expected changes** (test needs to update because the requirement changed). It prevents wasted debugging on tests that simply need updating, and ensures real regressions are not dismissed.

---

## Phase 0 — Input Analysis

Analyze `$ARGUMENTS` and determine:

| Input | Action |
|-------|--------|
| Test failure log or report | Analyze each failure |
| PR / commit reference | Compare changes against failing tests |
| "after deploy" or deployment context | Analyze as post-deploy regression |
| Specific test class name | Analyze that class's failures |

### Gather Context

Before analysis, collect:

1. **Failing tests** — list of test methods that failed
2. **Error messages** — actual vs expected, exception type
3. **Recent changes** — what code, API, or config changed
4. **Last known pass** — when did these tests last pass
5. **Environment** — which env is failing (dev / staging / prod)

---

## Phase 1 — Classification Framework

### 1.1 Decision Tree

```
Test failed after change
│
├─ API response structure changed?
│  ├─ YES → Was change documented/intended?
│  │  ├─ YES → EXPECTED CHANGE — update test
│  │  └─ NO  → TRUE REGRESSION — API broke contract
│  └─ NO  → continue
│
├─ API response values changed?
│  ├─ YES → Is the new value correct per updated requirement?
│  │  ├─ YES → EXPECTED CHANGE — update test data
│  │  └─ NO  → TRUE REGRESSION — API returning wrong data
│  └─ NO  → continue
│
├─ API endpoint removed or moved?
│  ├─ YES → Was migration planned?
│  │  ├─ YES → EXPECTED CHANGE — update endpoint in ApiClient
│  │  └─ NO  → TRUE REGRESSION — breaking change without notice
│  └─ NO  → continue
│
├─ Auth behavior changed?
│  ├─ YES → Was auth requirement added/removed intentionally?
│  │  ├─ YES → EXPECTED CHANGE — update auth config
│  │  └─ NO  → TRUE REGRESSION — auth broken
│  └─ NO  → continue
│
├─ Test data no longer valid?
│  ├─ YES → Was data cleaned up or migrated?
│  │  ├─ YES → EXPECTED CHANGE — update test data for env
│  │  └─ NO  → ENVIRONMENT ISSUE — data disappeared unexpectedly
│  └─ NO  → continue
│
├─ Test infrastructure issue?
│  ├─ YES → FLAKY / INFRA — not a regression
│  └─ NO  → continue
│
└─ None of the above
   └─ TRUE REGRESSION — unknown cause, needs investigation
```

### 1.2 Classification Categories

| Category | Code | Meaning | Action |
|----------|------|---------|--------|
| True Regression | `REG` | Something broke unintentionally | File bug, block release |
| Expected Change | `EXP` | Test needs update per new requirement | Update test, no bug |
| Environment Issue | `ENV` | Test data or config problem | Fix environment, no code change |
| Flaky Test | `FLK` | Intermittent failure, not deterministic | Investigate flakiness, add retry or fix |
| Stale Test | `STL` | Test validates removed/deprecated feature | Remove or rewrite test |
| Unknown | `UNK` | Cannot determine — needs manual investigation | Escalate to team |

---

## Phase 2 — Analysis by Failure Type

### 2.1 Schema Failures

**Symptom:** Field missing, wrong type, unexpected null

| Signal | Classification | Reasoning |
|--------|---------------|-----------|
| Field renamed in recent API update | `EXP` | Schema changed intentionally |
| Field removed without documentation | `REG` | Breaking change |
| New field added (test checks "no extra fields") | `EXP` if strict mode, ignore if lenient | Adjust strict mode policy |
| Field type changed (string → number) | `REG` unless documented | Type changes break consumers |
| Field became nullable | `EXP` if documented, `REG` if not | Nullability is part of contract |

**Action for EXP:** Run `/qa-generate-schema-test` to regenerate schema definition.

### 2.2 Business Logic Failures

**Symptom:** Wrong values, wrong calculation, wrong filtering

| Signal | Classification | Reasoning |
|--------|---------------|-----------|
| Calculation formula changed per new requirement | `EXP` | Business logic updated |
| Filter returns different results after data migration | `ENV` | Test data changed |
| Sort order changed without requirement change | `REG` | Unintended behavior change |
| Pagination returns different total count | `ENV` if data changed, `REG` if logic changed | Check if total count logic changed |

**Action for EXP:** Update expected values in test data. Run `/qa-generate-test-data` if data needs restructuring.

### 2.3 HTTP Status Failures

**Symptom:** Expected 200 got 404, expected 400 got 500

| Signal | Classification | Reasoning |
|--------|---------------|-----------|
| 404 — endpoint path changed | `EXP` if migration planned, `REG` if not | Check API changelog |
| 401 — auth requirement added to public endpoint | `EXP` if security update, `REG` if unintended | Verify with team |
| 500 — server error on valid request | `REG` | Server should not crash |
| 400 — validation rule changed | `EXP` if requirement changed | Update test expectation |
| 200 — previously failing request now succeeds | `EXP` if bug was fixed | Update negative test |

### 2.4 Connection / Timeout Failures

**Symptom:** SocketTimeoutException, ConnectionRefused

| Signal | Classification | Reasoning |
|--------|---------------|-----------|
| All tests fail with connection error | `ENV` | Environment is down |
| Only specific endpoint times out | `REG` if performance degraded, `ENV` if infra issue | Check with DevOps |
| Intermittent timeouts | `FLK` | Network instability |

### 2.5 Test Data Failures

**Symptom:** Entity not found, empty results where data expected

| Signal | Classification | Reasoning |
|--------|---------------|-----------|
| Test entity was deleted in env | `ENV` | Recreate test data or update ID |
| Database was reset/migrated | `ENV` | Update all env-specific IDs |
| Test creates data but cleanup failed last run | `FLK` | Fix cleanup logic |
| New environment has no test data | `ENV` | Run data setup for new env |

---

## Phase 3 — Impact Assessment

For each failing test, assess:

### 3.1 Blast Radius

```
| Failed Test | Classification | Related Tests at Risk | Impact |
|-------------|---------------|----------------------|--------|
| shouldReturnValidSchema | REG — field removed | All schema tests for this entity | HIGH |
| shouldReturnResultsForValidInput | ENV — test data missing | All positive tests for this entity | MEDIUM |
| shouldFilterByDateRange | EXP — filter logic changed | Only this test | LOW |
```

### 3.2 Priority Matrix

| Classification | Failures Count | Priority | Action Timeline |
|---------------|---------------|----------|----------------|
| `REG` | Any | 🔴 Critical | Fix before release |
| `REG` | 5+ related | 🔴 Critical | Likely single root cause — fix source |
| `EXP` | Any | 🟡 Normal | Update in current sprint |
| `ENV` | Any | 🟡 Normal | Fix environment, re-run |
| `FLK` | Recurring | 🟡 Normal | Fix flakiness pattern |
| `STL` | Any | 🟢 Low | Clean up in backlog |
| `UNK` | Any | 🟡 Normal | Investigate, then reclassify |

---

## Phase 4 — Recommended Actions

### For TRUE REGRESSION (`REG`)

1. File bug report with:
    - Failing test name and error message
    - Last known passing run
    - Suspected root cause (which change broke it)
    - Severity based on blast radius
2. Reference: `/qa-analyze-failure` for detailed root cause analysis
3. Reference: `/qa-fix-test` ONLY after bug is confirmed and fixed

### For EXPECTED CHANGE (`EXP`)

1. Identify all affected tests (not just the failing ones)
2. Generate update plan:

```
| Test to Update | What Changed | Update Needed |
|---------------|-------------|---------------|
| ValidateEntitySchemaApiTest | Field 'x' renamed to 'y' | Update SchemaDefinition |
| shouldReturnResultsForValidInput | Filter logic changed | Update expected values |
| shouldReturnErrorWhenInvalidInput | Validation rule added | Add new test case |
```

3. Reference: Appropriate generate command to regenerate
    - Schema change → `/qa-generate-schema-test`
    - Data change → `/qa-generate-test-data`
    - New behavior → `/qa-generate-api-test` or `/qa-generate-ui-test`

### For ENVIRONMENT ISSUE (`ENV`)

1. Reference: `/qa-setup-environment validate` to diagnose
2. Fix environment config or test data
3. Re-run failing tests to confirm fix

### For FLAKY TEST (`FLK`)

1. Identify flakiness pattern (timing, data dependency, order dependency)
2. Add appropriate fix:
    - Timing: Add explicit wait or retry
    - Data: Ensure test data independence
    - Order: Remove inter-test dependencies
3. Track flakiness rate over time

### For STALE TEST (`STL`)

1. Confirm feature is truly removed/deprecated
2. Mark test as `@Ignore` with reason, or delete
3. Update test plan via `/qa-create-test-plan`

---

## Phase 5 — Output

### 5.1 Regression Analysis Report

```
=== Regression Analysis Report ===
Date: [date]
Environment: [env]
Trigger: [what changed — deploy / PR / data migration]
Total failures analyzed: [N]

Summary:
| Category | Count | Action |
|----------|-------|--------|
| REG — True Regression | 2 | Block release, file bugs |
| EXP — Expected Change | 5 | Update tests |
| ENV — Environment Issue | 1 | Fix env config |
| FLK — Flaky | 0 | — |
| STL — Stale | 1 | Remove test |
| UNK — Unknown | 0 | — |

Details:
[Per-test classification with reasoning]

Recommended Actions:
[Prioritized action list with command references]
```

Save regression analysis report to `.claude/artifacts/regression-analysis/latest.md`

### 5.2 Generated Artifacts

| # | Artifact | When to generate |
|---|----------|-----------------|
| 1 | Regression analysis report | Always |
| 2 | Bug report template (for REG) | When true regressions found |
| 3 | Test update plan (for EXP) | When expected changes found |
| 4 | Environment fix checklist (for ENV) | When environment issues found |
| 5 | Flakiness investigation notes (for FLK) | When flaky tests found |

---

## Anti-patterns (STRICTLY FORBIDDEN)

| ❌ Forbidden | ✔ Required |
|-------------|-----------|
| Assuming all failures are regressions | Classify each failure individually |
| Auto-fixing tests without understanding root cause | Classify first, then decide action |
| Ignoring failures as "probably flaky" | Investigate and classify with evidence |
| Updating tests to pass without verifying new behavior is correct | Confirm requirement change before updating |
| Treating environment issues as code bugs | Separate ENV from REG clearly |
| Analyzing failures without recent change context | Always gather what changed before classifying |
| Mixing regression analysis with fix implementation | This command analyzes only — fixing is separate |