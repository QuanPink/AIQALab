Generate a reusable Page Object for: $ARGUMENTS

> This command follows rules defined in /shared-qa-rules. All rules in that file are mandatory.

---

## Purpose

Generate a clean, reusable, business-driven Page Object that encapsulates UI interactions
for a single page or significant component.

---

## Rules (MANDATORY)

### 1. Business-driven methods

Method names MUST reflect user actions, NOT technical interactions.

| ❌ BAD              | ✔ GOOD            |
| ------------------- | ----------------- |
| `clickButton()`     | `submitForm()`    |
| `typeText()`        | `search()`        |
| `clickLoginBtn()`   | `login()`         |
| `fillEmailInput()`  | `enterEmail()`    |

### 2. Fluent API — same-page actions return `this`

When an action stays on the same page, return `this` to enable chaining:

```java
@Step("Enter search keyword")
public SearchPage enterKeyword(String keyword) {
    WaitHelper.waitForVisible(driver, keywordField);
    driver.findElement(keywordField).sendKeys(keyword);
    return this;
}
```

### 3. Fluent navigation — cross-page actions return the destination Page Object

When an action navigates to a new page, return an instance of that page:

```java
// ✔ Correct — submit navigates to the next page
@Step("Submit login form")
public DashboardPage login(String username, String password) {
    driver.findElement(usernameField).sendKeys(username);
    driver.findElement(passwordField).sendKeys(password);
    driver.findElement(submitButton).click();
    return new DashboardPage(driver);
}

// ❌ Forbidden — returns this, but navigation has already happened
public LoginPage login(String username, String password) {
    // ...
    return this;  // wrong: user is no longer on LoginPage
}
```

### 4. No assertions

Page Objects MUST NOT contain `assertThat(...)` or any validation logic.
All assertions belong in the test class.

### 5. No business logic

Page Objects encapsulate UI mechanics only — not test decisions or conditionals.

---

## Locator Strategy

Priority order (use the highest available):

1. `By.id()`
2. `By.cssSelector("[data-testid='...']")` or `By.cssSelector("[aria-label='...']")`
3. `By.cssSelector()` with structural selectors
4. `By.xpath()` — last resort only

---

## Wait Strategy

- Use `WaitHelper` for ALL element interactions — never access elements without waiting
- No `Thread.sleep()` anywhere

```java
// ✔ Correct
WaitHelper.waitForVisible(driver, submitButton);
driver.findElement(submitButton).click();

// ❌ Forbidden
Thread.sleep(2000);
driver.findElement(submitButton).click();
```

---

## Template

```java
package com.framework.ui.pages;

import com.framework.base.BasePage;
import com.framework.core.config.ConfigManager;
import com.framework.utils.WaitHelper;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class <Name>Page extends BasePage {

    // ── Locators ─────────────────────────────────────────────────────
    private final By mainElement  = By.id("main-element-id");
    private final By actionButton = By.cssSelector("[data-testid='action-button']");

    // ── Constructor ──────────────────────────────────────────────────
    public <Name>Page(WebDriver driver) {
        super(driver);
    }

    // ── Navigation ───────────────────────────────────────────────────
    @Step("Navigate to <Name> page")
    public <Name>Page navigate() {
        driver.get(ConfigManager.get("app.url") + "/<path>");
        WaitHelper.waitForVisible(driver, mainElement);
        return this;
    }

    // ── Same-page actions (return this) ──────────────────────────────
    @Step("Perform action with: {input}")
    public <Name>Page performAction(String input) {
        WaitHelper.waitForVisible(driver, mainElement);
        // ... interact with elements
        return this;
    }

    // ── Navigation actions (return destination Page Object) ──────────
    @Step("Submit and proceed")
    public <DestinationPage>Page submit() {
        WaitHelper.waitForVisible(driver, actionButton);
        driver.findElement(actionButton).click();
        return new <DestinationPage>Page(driver);
    }
}
```

---

## Anti-patterns (STRICTLY FORBIDDEN)

| ❌ Forbidden                              | ✔ Required                                     |
| ----------------------------------------- | ---------------------------------------------- |
| `clickSubmitButton()`                     | `submitForm()` or `placeOrder()`               |
| `assertThat(...).isDisplayed()`           | Assertions belong in test class only           |
| Returning `this` after cross-page action  | Return the destination Page Object             |
| Accessing element without waiting         | Always use `WaitHelper` before interaction     |
| `Thread.sleep()`                          | `WaitHelper.waitForVisible()` etc.             |
| Exposing `By` locators as public fields   | All locators are `private final`               |
| Putting test logic inside Page Object     | Page Object is mechanics only                  |

---

## Output

Generate the following artifact(s):

1. **Page Object class** — one class per page or component
2. **List of public methods** — brief description of each method and what it returns
3. **Navigation map** — describe which actions navigate to which Page Object

Example navigation map:
```
LoginPage.login()         → DashboardPage
DashboardPage.openMenu()  → NavigationPage
NavigationPage.goTo()     → <TargetPage>
```
