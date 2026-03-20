Fix failing test(s): $ARGUMENTS

> This command follows rules defined in /shared-qa-rules. All rules in that file are mandatory.

## Instructions

Diagnose and fix test failures. `$ARGUMENTS` can be:
- `<TestClass>#<method>` — fix specific test
- `<TestClass>` — fix all failures in class
- `all` — fix all recent failures
- A failure description — diagnose and fix based on description

### Step 1: Diagnose (Run qa-analyze-failure Logic)

1. Read `target/surefire-reports/*.xml` to find the failure(s)
2. Read `target/logs/test.log` for stack traces
3. Read the failing test source: `src/test/java/com/framework/...`
4. Read related Page Objects or API Clients
5. Classify the root cause (same classification as `qa-analyze-failure`)

### Step 1b: Classification Context

When fixing a failure, consider its regression classification (from `/qa-analyze-regression`):

| Classification | Fix Strategy |
|---------------|-------------|
| REG — True Regression | Fix the code bug, not the test. Test expectation was correct. |
| EXP — Expected Change | Update the test to match new requirement. Code is correct. |
| ENV — Environment Issue | Fix config/data, not code. No test or source change needed. |
| FLK — Flaky | Fix timing, data dependency, or parallelism issue. Add retry if appropriate. |
| STL — Stale Test | Remove or rewrite test. Feature no longer exists. |
| UNK — Unknown | Investigate further before fixing. Do not guess. |

If classification is unknown, run `/qa-analyze-regression` first before applying fix.

### Step 2: Apply Fix by Category

#### Fix: Locator Failure
`NoSuchElementException` or `TimeoutException` pointing to a `By.*` selector

```java
// Before (broken)
private final By loginButton = By.id("login-btn");  // id changed

// After (fixed)
private final By loginButton = By.cssSelector("[data-testid='login-button']");
// or
private final By loginButton = By.cssSelector("[aria-label='Login']");
```

**Steps:**
1. Open browser DevTools on the target page to inspect current element attributes
2. Update the `By.*` locator in the Page Object file
3. Prefer: `By.id()` > `By.cssSelector()` with data-testid/aria-label > XPath

#### Fix: Assertion Failure
`AssertionError` — expected value doesn't match actual

```java
// Before (wrong expected)
UiValidator.assertThat(page).titleIs("<AppName> - Login");

// After (corrected — update to match actual page title)
UiValidator.assertThat(page).titleIs("Login | <AppName>");
```

**Steps:**
1. Check current actual value from the error message
2. Determine if: (a) expected value is wrong, or (b) app behavior changed
3. Update assertion if expected value is wrong; flag as bug if app behavior is wrong

#### Fix: Timing Issue
`StaleElementReferenceException` or intermittent `TimeoutException`

```java
// Before (no wait after action)
page.clickButton();
assertThat(page.getMessage()).isEqualTo("Success");

// After (explicit wait)
page.clickButton();
WaitHelper.waitForVisible(driver, messageLocator);
assertThat(page.getMessage()).isEqualTo("Success");
```

**Also check:**
```java
// Add waitForInvisible for loading spinners
WaitHelper.waitForInvisible(driver, By.cssSelector(".loading-spinner"));

// Use waitForUrlContains after navigation
WaitHelper.waitForUrlContains(driver, "/dashboard");
```

#### Fix: Flaky Test (Parallel Safety)
`NullPointerException` or driver conflicts in parallel execution

```java
// Before (static - NOT thread safe)
private static WebDriver driver;
private static LoginPage loginPage;

// After (use ThreadLocal via BaseTest)
// Just use getDriver() from BaseTest - it's already ThreadLocal
LoginPage loginPage = new LoginPage(getDriver());
```

**Checklist:**
- No `static` fields holding driver or page objects
- `volatile` keyword on shared state between `@Test` methods in same class
- No `Thread.sleep()` — use `WaitHelper` instead

#### Fix: Config/Auth Issue
HTTP 401/403 or `NullPointerException` on config key

**Checklist:**
1. Confirm the config key exists in `src/test/resources/config/config-{env}.properties`:
   ```properties
   api.token=<your-token>
   api.base.url=https://api.example.com
   ```
2. Verify the value is not null at runtime:
   ```java
   System.out.println(ConfigManager.get("api.token")); // add temporarily
   ```
3. If using environment variable override:
   ```bash
   export API_TOKEN=<token>          # maps to api.token
   export API_BASE_URL=https://...   # maps to api.base.url
   ```
4. If the endpoint is **public** (no auth needed) and the test expects 401/403:
   - This is NOT a config issue — it is a wrong test expectation
   - Update the test to assert HTTP 200 instead
   - Document the endpoint as public in the test class Javadoc

#### Fix: Test Data Issue
HTTP 422 / 400 or `AssertionError` on response fields that no longer exist

**Checklist:**
1. Re-call the endpoint manually (curl or Postman) with the same payload — confirm what the API currently returns
2. If the API contract changed (new required field, renamed field, removed field):
   - Update the request model in `src/main/java/com/framework/api/model/`
   - Update the params builder in the Service class
3. If a response field is now null:
   - Check if null is intentional (by-design) — if so, update the assertion to `.isNull()` or remove it
   - Do NOT use `getInt()` / `getString()` on nullable fields — use `getObject(..., Integer.class)` with a null guard
4. If static test data is stale:
   - Update `src/test/resources/testdata/*.json`
   - Or switch to a `System.getProperty("test.entity.id", "fallback")` pattern

#### Fix: Service Layer Failure
`NullPointerException`, `IllegalArgumentException`, or unexpected HTTP status originating inside a Service class

**Steps:**
1. Read the Service class in `src/main/java/com/framework/api/service/`
2. Check param construction — are all required fields set?

   ```java
   // Before (missing required field)
   SearchParams params = SearchParams.builder()
       .limit(20)
       .build();  // keyword is missing → API returns 400

   // After (all required fields)
   SearchParams params = SearchParams.builder()
       .keyword(keyword)
       .limit(20)
       .build();
   ```

3. Check the ApiClient method being called — does it match the intended action?

   ```java
   // Before (wrong method)
   return apiClient.list(params);   // wrong — should be search

   // After (correct method)
   return apiClient.search(params);
   ```

4. Check return value — is the Response object being returned, not null?

   ```java
   // Before (forgot return)
   apiClient.search(params);

   // After
   return apiClient.search(params);
   ```

5. Fix in the Service class directly — do NOT add workarounds in the test class

### Step 3: Edit Files Directly

Use the Edit tool to apply the fix to the actual source files:
- Page Objects: `src/main/java/com/framework/ui/pages/<Name>Page.java`
- Test Classes: `src/test/java/com/framework/ui/<Name>Test.java` or `com/framework/api/<Name>ApiTest.java`
- API Clients: `src/main/java/com/framework/api/client/<Name>ApiClient.java`
- Test data: `src/test/resources/testdata/*.json`

### Step 4: Explain Changes

For each file edited, provide:
```
FIXED: <TestClass>#<method>
─────────────────────────────
File    : <path>
Change  : <what was changed>
Reason  : <why this fixes the issue>
Confidence: High / Medium / Low
```

If confidence is **Low** (e.g., can't verify locator without running browser):
- Explain what needs manual verification
- Suggest: open browser, inspect element, verify selector

### Step 4b: Generate Regression Test

After fix is verified, automatically create a regression test that catches the exact scenario:
```java
// Pattern: regression test for [original failure]
@Test(groups = {"regression"}, description = "Regression — [brief failure description]")
@Story("[Feature] — Regression Guard")
@Severity(SeverityLevel.CRITICAL)
public void shouldNotRegress_[originalMethodName]() {
    // Reproduce the exact conditions that caused the original failure
    // Assert the fix holds — same assertion that previously failed
}
```

**Rules:**
1. Regression test MUST target the exact input/condition that caused the failure
2. Method name: `shouldNotRegress_<originalMethodName>()`
3. Groups: always `{"regression"}` — never `{"smoke"}`
4. Severity: CRITICAL — regression means something broke before
5. Add comment linking back to the original failure: `// Regression guard for: <TestClass>#<method> — <date>`
6. If the fix was in Service layer or ApiClient, the regression test still belongs in the Test class

### Step 5: Verify Fix

After editing, suggest verification command:
```bash
# Run specific test to verify fix
mvn test -Denv=dev -Dtest=<ClassName>#<methodName>

# Or run full class
mvn test -Denv=dev -Dtest=<ClassName>

# If timing fix — run multiple times to check for flakiness
mvn test -Denv=dev -Dtest=<ClassName> -Dsurefire.rerunFailingTestsCount=3
```

Then: `/qa-analyze-failure` to verify no new failures introduced.

---

## Anti-patterns (STRICTLY FORBIDDEN)

| ❌ Forbidden | ✔ Required |
|-------------|-----------|
| Changing test assertions to pass without verifying correct behavior | Confirm requirement change before updating expectations |
| Fixing the test instead of the bug when root cause is REG | Fix the code; update the test only for EXP |
| Using Thread.sleep() for timing fixes | Use WaitHelper explicit waits |
| Adding static driver/page object fields | Use ThreadLocal via BaseTest getDriver() |
| Applying fix without running test to verify | Always verify with mvn test -Dtest=ClassName#method |
| Skipping regression test generation after fix | Always add shouldNotRegress_method() guard |

Save fix summary to `.claude/artifacts/fix-test/latest.md`

---

> Framework paths: See /shared-qa-rules for all standard file locations.
