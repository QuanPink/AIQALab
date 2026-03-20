package com.framework.ui.base;

import com.framework.core.driver.DriverManager;
import com.framework.core.logger.LogManager;
import com.framework.utils.WaitHelper;
import io.qameta.allure.Step;
import org.openqa.selenium.*;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for all Page Objects.
 *
 * <p>Provides wrapped Selenium interactions with built-in explicit waits,
 * Allure step annotations, and debug logging. Page Objects should always
 * delegate to these methods rather than calling {@code driver} directly.
 *
 * <p>Fields are {@code private} — subclasses interact via protected methods only,
 * which enforces the encapsulation boundary between framework internals and pages.
 */
public abstract class BasePage {

    private final WebDriver driver;
    private final WaitHelper waitHelper;

    protected BasePage() {
        this.driver     = DriverManager.getDriver();
        this.waitHelper = new WaitHelper(driver);
        PageFactory.initElements(driver, this);
    }

    // ── Navigation ───────────────────────────────────────────────────────

    @Step("Navigate to: {url}")
    protected void navigateTo(String url) {
        LogManager.info("Navigating to: " + url);
        driver.get(url);
    }

    // ── Interactions ─────────────────────────────────────────────────────

    @Step("Click element")
    protected void click(By locator) {
        LogManager.debug("Click: " + locator);
        waitHelper.waitForClickable(locator).click();
    }

    @Step("Type into element")
    protected void type(By locator, String text) {
        LogManager.debug("Type into: " + locator);
        WebElement el = waitHelper.waitForVisible(locator);
        el.clear();
        el.sendKeys(text);
    }

    /**
     * Clears a field using Ctrl+A → type, which fires the keyboard events that
     * React/Angular listen to — unlike {@code el.clear()} which can be silent.
     *
     * <p>{@code Keys.chord()} sends the keys simultaneously, making this
     * cross-platform safe (Chrome treats Ctrl+A as "select all" on all OSes).
     */
    @Step("Clear and type (keyboard)")
    protected void clearAndType(By locator, String text) {
        LogManager.debug("clearAndType into: " + locator);
        WebElement el = waitHelper.waitForClickable(locator);
        el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        el.sendKeys(text);
    }

    protected void selectByText(By locator, String visibleText) {
        new Select(waitHelper.waitForVisible(locator)).selectByVisibleText(visibleText);
    }

    protected void selectByValue(By locator, String value) {
        new Select(waitHelper.waitForVisible(locator)).selectByValue(value);
    }

    // ── JavaScript helpers ────────────────────────────────────────────────

    /**
     * Scrolls the element into the visible viewport. Useful when elements are
     * below the fold and standard {@code click()} throws an interception error.
     */
    protected void scrollToElement(By locator) {
        WebElement el = waitHelper.waitForPresence(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", el);
    }

    protected Object executeScript(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }

    // ── Getters ──────────────────────────────────────────────────────────

    protected String getText(By locator) {
        return waitHelper.waitForVisible(locator).getText().trim();
    }

    protected String getAttribute(By locator, String attribute) {
        return waitHelper.waitForPresence(locator).getAttribute(attribute);
    }

    protected List<String> getAllTexts(By locator) {
        return waitHelper.waitForAllVisible(locator).stream()
                .map(el -> el.getText().trim())
                .collect(Collectors.toList());
    }

    // ── State checks ─────────────────────────────────────────────────────

    protected boolean isDisplayed(By locator) {
        try {
            return waitHelper.waitForVisible(locator).isDisplayed();
        } catch (TimeoutException | NoSuchElementException e) {
            return false;
        }
    }

    protected boolean isEnabled(By locator) {
        try {
            return waitHelper.waitForPresence(locator).isEnabled();
        } catch (TimeoutException | NoSuchElementException e) {
            return false;
        }
    }

    protected boolean isPresent(By locator) {
        return waitHelper.isElementPresent(locator);
    }

    protected String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    protected String getPageTitle() {
        return driver.getTitle();
    }

    // ── WaitHelper exposure ───────────────────────────────────────────────

    /** Gives subclasses access to the wait helper for custom conditions. */
    protected WaitHelper getWaitHelper() {
        return waitHelper;
    }

    // ── Contract ─────────────────────────────────────────────────────────

    /**
     * Implement to verify the page is fully loaded.
     * Called by {@link com.framework.validator.UiValidator#pageIsLoaded()}.
     */
    public abstract boolean isPageLoaded();
}
