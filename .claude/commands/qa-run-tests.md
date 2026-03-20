Run tests with the following parameters: $ARGUMENTS

> This command follows rules defined in /shared-qa-rules. All rules in that file are mandatory.

## Instructions

Parse `$ARGUMENTS` to determine execution parameters, then run the tests and summarize results.

### Parameter Parsing

From `$ARGUMENTS`, extract:
- **suite**: `testng.xml` (default) | `testng-ui.xml` | `testng-api.xml` | `testng-smoke.xml`
- **env**: `dev` (default) | `staging` | `prod`
- **browser**: `chrome` (default) | `firefox` | `edge`
- **test class**: specific class name if provided
- **test method**: specific method name if provided (use `#` separator)
- **groups**: specific TestNG groups if provided
- **test data overrides**: `-Dtest.<key>=<value>` properties if provided

Examples:
- `smoke staging` → `mvn test -Dtestng.suite=testng-smoke.xml -Denv=staging`
- `ui dev firefox` → `mvn test -Dtestng.suite=testng-ui.xml -Denv=dev -Dbrowser=firefox`
- `LoginTest` → `mvn test -Denv=dev -Dtest=LoginTest`
- `LoginTest#shouldRedirectAfterLogin` → `mvn test -Denv=dev -Dtest=LoginTest#shouldRedirectAfterLogin`
- (empty) → `mvn test -Denv=dev`

**Single method run format:**
```bash
mvn test -Denv=dev -Dtest=<ClassName>#<methodName>
```

**Test data override format** (for configurable test properties):
```bash
# Override a specific entity ID used in the test
mvn test -Denv=dev -Dtest=<ClassName> -Dtest.entity.id=<value>

# Real example — provide a known entity key for lookup tests
mvn test -Denv=dev -Dtest=<ClassName> -Dtest.entity.key=<identifier-value>
```
Properties prefixed with `test.` are read via `System.getProperty("test.<key>", "<fallback>")` in test classes.

### Step 1: Build Maven Command

Construct the full command:
```bash
mvn test -Dtestng.suite=<suite> -Denv=<env> -Dbrowser=<browser> [additional params]
```

Run it now using the Bash tool.

### Step 2: Parse Results

After execution, read these files:
- `target/surefire-reports/*.xml` — TestNG XML results
- `target/surefire-reports/testng-results.xml` — overall summary
- `target/logs/test.log` (last 100 lines) — for error context

Extract:
- Total tests run
- Passed count
- Failed count
- Skipped count
- Total duration

### Step 3: Output Summary

```
╔══════════════════════════════════════════════
║  TEST EXECUTION SUMMARY
╠══════════════════════════════════════════════
║  Suite    : <suite>
║  Env      : <env>
║  Duration : <X>m <Y>s
╠══════════════════════════════════════════════
║  Total    : XX
║  ✓ Passed : XX
║  ✗ Failed : XX
║  ○ Skipped: XX
║  Pass Rate: XX%
╚══════════════════════════════════════════════
```

### Step 4: Failed Tests Detail

If there are failures, list each one:
```
FAILED TESTS:
─────────────────────────────────────────────
1. <TestClass>#<testMethod>
   Error: <first line of exception message>
   Type: <AssertionError | NoSuchElementException | etc.>

2. ...
```

### Step 5: Next Steps

**If all tests passed:**
```
✓ All tests passed!
View full report: mvn allure:serve
```

**If tests failed:**
```
✗ <N> test(s) failed.
Suggested actions:
  → /qa-analyze-failure          (analyze all failures)
  → /qa-analyze-failure <TestClass>#<method>  (analyze specific failure)
  → mvn allure:serve             (view detailed Allure report)
```

**If suite not found:**
```
Available suites in project root:
- testng.xml
- testng-ui.xml
- testng-api.xml
- testng-smoke.xml
```

### Available Run Configurations

| Alias | Command |
|-------|---------|
| `smoke` | `mvn test -Dtestng.suite=testng-smoke.xml -Denv=dev` |
| `ui` | `mvn test -Dtestng.suite=testng-ui.xml -Denv=dev` |
| `api` | `mvn test -Dtestng.suite=testng-api.xml -Denv=dev` |
| `all` | `mvn test -Denv=dev` |
| `staging` | `mvn test -Denv=staging` |
| `report` | `mvn allure:serve` |
| single class | `mvn test -Denv=dev -Dtest=<ClassName>` |
| single method | `mvn test -Denv=dev -Dtest=<ClassName>#<methodName>` |
| with data override | `mvn test -Denv=dev -Dtest=<ClassName> -Dtest.<key>=<value>` |

---

## Anti-patterns (STRICTLY FORBIDDEN)

| ❌ Forbidden | ✔ Required |
|-------------|-----------|
| Re-running failed tests without investigating cause | Analyze failure before re-run |
| Running full regression for every change | Use /qa-impact-analysis for targeted runs |
| Ignoring skipped tests in summary | Report skip count and reasons |
| Running on wrong environment without noticing | Always confirm env in summary output |
| No next-step suggestion after failures | Always suggest /qa-analyze-failure |

Save execution summary to `.claude/artifacts/run-tests/latest.md`
