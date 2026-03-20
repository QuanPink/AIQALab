Generate a complete Java UI test class for: $ARGUMENTS

> This command follows rules defined in /shared-qa-rules. All rules in that file are mandatory.

---

## Phase 0 — Flow Classification (MANDATORY)

Analyze `$ARGUMENTS` and classify:

| Dimension      | Options                                          |
| -------------- | ------------------------------------------------ |
| Flow type      | FORM / SEARCH / NAVIGATION / CRUD / MULTI-STEP   |
| Auth           | required / optional / public                     |
| Page count     | single-page / multi-page                         |
| Required data  | list clearly                                     |

→ Add as a comment block at the top of the test class

---

## Phase 1 — Naming (MANDATORY)

### Test Class Naming

Format:

```
<BusinessFlow>Test
```

- Reflects user behavior, NOT page/component name

Examples:

| ❌ BAD              | ✔ GOOD                  |
| ------------------- | ----------------------- |
| `LoginPageTest`     | `AuthenticateUserTest`  |
| `UserTableTest`     | `SearchUserTest`        |
| `FormSubmitTest`    | `CreateOrderTest`       |

### Method Naming

Pattern:

```
should<Behavior>[When<Condition>]()
```

Examples:
- `shouldRedirectToDashboardAfterLogin()`
- `shouldShowErrorWhenPasswordIsWrong()`
- `shouldDisplayEmptyStateWhenNoResults()`
- `shouldFilterResultsByCategory()`

---

## Phase 2 — Input Interpretation

`$ARGUMENTS` can be:
- Business flow description (e.g., "User login flow")
- Test scenarios from `/qa-generate-testcase`
- User behavior description

Rules:
- If input is test scenarios: each scenario → 1 test method; use Automation Mapping as method name; do NOT invent new behaviors
- If input is a flow description: extract core behaviors and map to test methods

---

## Phase 3 — Test Strategy

### FORM / CRUD Flow

**Positive:**
- Valid input → operation succeeds, success state shown
- Form submitted → correct page/state follows

**Negative:**
- Invalid or missing input → validation error shown
- Unauthorized access → redirect to login

**Cross-cutting:**
- Auth state (logged in / logged out)
- Page navigation after action
- UI state persistence after reload

**Edge Cases:**
- empty form submission
- special characters in all input fields
- maximum length input
- rapid repeated submissions (double-click)

**Security:** (if auth applicable)
- access page without authentication → verify redirect
- session expiry during interaction → verify behavior

### SEARCH / FILTER Flow

**Positive:**
- Valid keyword → results displayed
- Filter applied → results narrowed correctly

**Negative:**
- No match → empty state shown (not an error)

**Cross-cutting:**
- Pagination behavior
- Sort behavior
- Loading state during search

**Edge Cases:**
- empty form submission
- special characters in all input fields
- maximum length input
- rapid repeated submissions (double-click)

**Security:** (if auth applicable)
- access page without authentication → verify redirect
- session expiry during interaction → verify behavior

### NAVIGATION Flow

**Positive:**
- Correct page loads after navigation action

**Negative:**
- Unauthorized page → redirect to login

**Cross-cutting:**
- URL state / deep linking
- Browser back/forward behavior

**Edge Cases:**
- empty form submission
- special characters in all input fields
- maximum length input
- rapid repeated submissions (double-click)

**Security:** (if auth applicable)
- access page without authentication → verify redirect
- session expiry during interaction → verify behavior

---

## Phase 4 — Framework

- Package: `com.framework.ui`
- Extend: `BaseTest`
- Location: `src/test/java/com/framework/ui/`
- DO NOT use `ApiClient` or REST Assured in UI tests
- DO NOT use `WebDriver` directly in test class — use Page Objects only

---

## Phase 5 — Architecture (MANDATORY)

UI test MUST follow this dependency chain:

```
Test → Page Object → WebDriver (via WaitHelper)
```

### Page Object Rules

1. MUST create a Page Object per page or significant component
   - Extend `BasePage`
   - Location: `src/main/java/com/framework/ui/pages/`

2. Test MUST NOT call `driver.*` directly

3. Page Object responsibilities:
   - Encapsulate all locators (private)
   - Perform actions via `WaitHelper`
   - Return `this` (same page) or the destination Page Object (on navigation)

4. Page Object MUST NOT:
   - Contain assertions
   - Contain test logic
   - Expose `By` locators as public fields

5. **Fluent navigation pattern** — when an action navigates to a new page,
   return the destination Page Object:

   ```java
   // ✔ Correct — login() navigates and returns the next page
   @Step("Log in as user")
   public DashboardPage login(String username, String password) {
       WaitHelper.waitForVisible(driver, usernameField);
       driver.findElement(usernameField).sendKeys(username);
       driver.findElement(passwordField).sendKeys(password);
       driver.findElement(submitButton).click();
       return new DashboardPage(driver);
   }

   // ❌ Forbidden — returns this, but navigation has already occurred
   public LoginPage login(String username, String password) { ... }
   ```

6. Page Object reuse:
   - If Page Object already exists → **reuse it, do NOT regenerate**
   - If Page Object does not exist → generate following `/qa-generate-page-object` rules

---

## Phase 6 — Data Rules

1. Read test credentials and inputs from config:

   ```java
   String username = ConfigManager.get("test.username");
   String password = ConfigManager.get("test.password");
   ```

2. For dynamic / unique data:

   ```java
   String email = RandomDataUtils.uniqueEmail();
   String name  = RandomDataUtils.randomString(8);
   ```

3. For static fixtures → read from `src/test/resources/testdata/*.json` via `JsonUtils`

4. Do NOT hardcode credentials, URLs, or domain-specific values inline in test methods

---

## Phase 7 — Output

Generate the following artifacts:

1. **Test class** — tests grouped by category (Positive, Negative, Edge Cases, Cross-cutting, Security)
2. **Page Object** — if not already exists; follow `/qa-generate-page-object` rules
3. **Test data** — list any new config keys or JSON fixture entries needed
4. **Test case list** — grouped by category with brief description of each group

### Test Structure Template

```java
/**
 * UI Flow Classification:
 * - Flow type : <FORM / SEARCH / NAVIGATION / CRUD>
 * - Auth      : <required / optional / public>
 * - Pages     : <page list>
 */
@Epic("<AppName>")
@Feature("<BusinessFeature>")
public class <BusinessFlow>Test extends BaseTest {

    private <PageObject> page;

    @BeforeMethod
    public void setUp() {
        page = new <PageObject>(getDriver());
    }

    @Test(groups = {"smoke", "regression"})
    @Story("<BusinessStory>")
    @Severity(SeverityLevel.BLOCKER)
    public void should<Behavior>[When<Condition>]() {
        String input = ConfigManager.get("test.input");

        page.navigate()
            .performAction(input);

        UiValidator.assertThat(page)
            .isOnPage("<expected page>");
    }
}
```

### Required Imports

```java
import com.framework.utils.ConfigManager;
import com.framework.utils.JsonUtils;
import com.framework.utils.RandomDataUtils;
import com.framework.validator.UiValidator;
import io.qameta.allure.*;
import org.testng.annotations.*;
```

---

## Anti-patterns (STRICTLY FORBIDDEN)

| ❌ Forbidden                                    | ✔ Required                                    |
| ----------------------------------------------- | --------------------------------------------- |
| Naming test class after page/component          | Business-driven class naming                  |
| Test calls `driver.*` directly                  | Test calls Page Object only                   |
| Page Object contains `assertThat(...)`          | Assertions only in test class                 |
| Returning `this` when navigation occurs         | Return the destination Page Object            |
| Hardcoding credentials or URLs in test          | Read from `ConfigManager`                     |
| Mixing multiple business flows in one class     | One flow per test class                       |
| Redefining Page Object in a different style     | Always follow `/qa-generate-page-object` rules |

---

## Run

```bash
# Run full test class
mvn test -Dtestng.suite=testng-ui.xml -Denv=dev -Dtest=<ClassName>Test

# Run single method
mvn test -Denv=dev -Dtest=<ClassName>#<methodName>
```

Save generation manifest (list of files created + test case summary) to `.claude/artifacts/generated-ui-test/latest.md`
