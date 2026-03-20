Analyze test failures: $ARGUMENTS

> This command follows rules defined in /shared-qa-rules. All rules in that file are mandatory.

## Instructions

Perform root cause analysis on failed tests. `$ARGUMENTS` can be:
- Empty or `all` → analyze all recent failures
- `<TestClass>` → analyze all failures in that class
- `<TestClass>#<method>` → analyze specific test failure
- A stack trace or error message pasted directly

### Step 1: Collect Failure Data

Read the following files:
1. `target/surefire-reports/*.xml` — find failed test entries
2. `target/surefire-reports/testng-results.xml` — overall result summary
3. `target/logs/test.log` — search for ERROR/FAILED entries related to the test(s)

For each failed test, extract:
- Full class name and method name
- Exception type
- Exception message (first line)
- Full stack trace (first 10-15 lines)
- Duration before failure

### Step 2: Read Source Code

For each failed test:
1. Read the test method source from `src/test/java/...`
2. Read the related Page Object(s) or API Client(s)
3. Identify the exact line that threw the exception

### Step 3: Classify Root Cause

Use these classification rules:

| Exception / Pattern | Root Cause Category | Likely Location |
|---------------------|--------------------|-----------------------|
| `NoSuchElementException` | **Locator Failure** | Page Object `By.*` selector |
| `TimeoutException` | **Timing / Locator** | WaitHelper timeout or bad locator |
| `StaleElementReferenceException` | **Timing Issue** | Re-fetch element after DOM update |
| `AssertionError` | **Assertion Failure** | Wrong expected value or assertion logic |
| HTTP 500 / 502 / 503 | **App/Server Error** | Backend issue, may be transient |
| HTTP 401 / 403 | **Auth/Config Issue** | Token missing, expired, or wrong env |
| HTTP 404 | **Endpoint/Config** | Wrong URL, wrong env, deleted resource |
| HTTP 422 / 400 | **Test Data Issue** | Invalid payload, changed API contract |
| `NullPointerException` | **Test Data / Config** | Missing config key or null response |
| `WebDriverException: session not found` | **Driver Issue** | Driver crashed, parallel conflict |
| `FileNotFoundException` | **Config/Data Issue** | Missing test data file |
| `AssertionError` on null field | **Null Field by Design** | API returns null intentionally — assertion is wrong |
| HTTP 200 on auth test expecting 401/403 | **Public Endpoint** | Endpoint requires no auth — test expectation is wrong |
| `NullPointerException` in Service class | **Service Layer Failure** | Service builds wrong params or calls wrong ApiClient method |

#### Extended Categories — API-specific

**Null Field by Design**
- Pattern: `AssertionError` asserting a field is non-null/non-blank, but the field is consistently `null`
- Verify: call the endpoint directly (curl) and confirm the field is always `null`
- If confirmed: the API intentionally omits this field — it is NOT a bug
- Action: remove or update the assertion to use `.isNull()` or skip the field entirely
- Do NOT use `getInt()` / `getString()` on null fields — use `getObject(..., String.class)` with a null check

**Public Endpoint 200 without Auth**
- Pattern: test expects HTTP 401 or 403, but actual response is 200
- Verify: call the endpoint without any `Authorization` header — if it returns 200, the endpoint is public
- If confirmed: the test expectation is wrong, NOT the API behavior
- Action: update the test to assert 200, or remove the auth-required test for this endpoint
- Document the endpoint as public in the test class Javadoc

**Service Layer Failure**
- Pattern: `NullPointerException`, `IllegalArgumentException`, or wrong HTTP status in a test that uses a Service class
- Verify: read the Service class in `src/main/java/com/framework/api/service/`
- Common causes:
  - Service builds params incorrectly (wrong field, missing required param)
  - Service calls wrong ApiClient method
  - Service returns wrong response object
- Action: fix the Service class directly — do NOT work around it in the test
- Command: `/qa-fix-test <TestClass>#<method>` → will land in "Fix: Service Layer" section

---

### Step 4: Output Per-Test Analysis

For each failed test:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
FAILURE: <TestClass>#<methodName>
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Category : <Locator Failure | Assertion Failure | App Error | ...>
Exception: <ExceptionType>
Message  : <error message>

Root Cause Analysis:
  - <explain what went wrong based on source code>
  - <identify the specific line/locator/assertion that failed>
  - <explain why it might have failed>

Evidence:
  - Source: <file>:<line> → <code snippet>
  - Locator: <By expression if applicable>
  - Expected: <expected value if assertion>
  - Actual  : <actual value if assertion>

Recommended Fix:
  → <specific action to take>
  → <file to edit>
  → Command: /qa-fix-test <TestClass>#<method>
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Step 5: Summary & Patterns

```
FAILURE SUMMARY
───────────────────────────────────────
Total failures analyzed: X

By category:
  Locator Failure        : X
  Assertion Failure      : X
  App/Server Error       : X
  Test Data Issue        : X
  Timing Issue           : X
  Config Issue           : X
  Null Field by Design   : X
  Public Endpoint        : X
  Service Layer Failure  : X

Common patterns:
  <list any repeated root causes>

Suggested batch fixes:
  → If multiple locator failures: audit Page Objects in <package>
  → If auth errors: verify token in config-<env>.properties
  → If timing issues: review WaitHelper timeouts
```

### Step 6: Action

```
Next steps:
  → /qa-fix-test <TestClass>#<method>   (fix specific failure)
  → /qa-fix-test all                    (attempt to fix all failures)
  → mvn allure:serve                    (view full report with screenshots)
```

Save analysis output to `.claude/artifacts/failure-analysis/latest.md`

---

## Anti-patterns (STRICTLY FORBIDDEN)

| ❌ Forbidden | ✔ Required |
|-------------|-----------|
| Marking flaky test as fixed without root cause | Investigate and classify with evidence |
| Blaming environment without verification | Verify by running outside test framework (curl, browser) |
| Classifying all failures as same category | Classify each failure individually |
| Ignoring Service Layer as potential root cause | Check Service class when test uses layered architecture |
| Reporting without stack trace or line number | Every failure must include source location |
