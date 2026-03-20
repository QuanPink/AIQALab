package com.framework.validator;

import com.framework.core.driver.DriverManager;
import com.framework.ui.base.BasePage;
import org.assertj.core.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Fluent validator for UI / page-level assertions.
 *
 * <p>Usage:
 * <pre>{@code
 * UiValidator.assertThat(dashboardPage)
 *     .pageIsLoaded()
 *     .urlContains("/dashboard")
 *     .elementIsVisible(By.id("header"));
 * }</pre>
 */
public class UiValidator {

    private final BasePage page;
    private final WebDriver driver;

    /**
     * Short wait used for assertion-time visibility checks.
     * Long enough to absorb minor rendering delays, short enough to fail fast.
     */
    private final WebDriverWait assertWait;

    private UiValidator(BasePage page) {
        this.page       = page;
        this.driver     = DriverManager.getDriver();
        this.assertWait = new WebDriverWait(driver, Duration.ofSeconds(3));
    }

    public static UiValidator assertThat(BasePage page) {
        return new UiValidator(page);
    }

    // ── Page state ────────────────────────────────────────────────────────

    public UiValidator pageIsLoaded() {
        Assertions.assertThat(page.isPageLoaded())
                .as("Page [%s] did not report as loaded", page.getClass().getSimpleName())
                .isTrue();
        return this;
    }

    // ── URL ───────────────────────────────────────────────────────────────

    public UiValidator urlContains(String fragment) {
        Assertions.assertThat(driver.getCurrentUrl())
                .as("URL does not contain '%s'. Actual: %s", fragment, driver.getCurrentUrl())
                .contains(fragment);
        return this;
    }

    public UiValidator urlEquals(String expected) {
        Assertions.assertThat(driver.getCurrentUrl())
                .as("URL mismatch. Expected: %s, Actual: %s", expected, driver.getCurrentUrl())
                .isEqualTo(expected);
        return this;
    }

    // ── Title ─────────────────────────────────────────────────────────────

    public UiValidator titleContains(String text) {
        Assertions.assertThat(driver.getTitle())
                .as("Page title does not contain '%s'. Actual: '%s'", text, driver.getTitle())
                .contains(text);
        return this;
    }

    // ── Elements ──────────────────────────────────────────────────────────

    /**
     * Asserts the element is visible, with a short grace period to absorb
     * rendering delays. This prevents false negatives from instant checks on
     * elements that appear slightly after a page transition.
     */
    public UiValidator elementIsVisible(By locator) {
        Assertions.assertThat(isVisible(locator))
                .as("Expected element to be visible: %s", locator)
                .isTrue();
        return this;
    }

    public UiValidator elementIsNotVisible(By locator) {
        Assertions.assertThat(isVisible(locator))
                .as("Expected element to NOT be visible: %s", locator)
                .isFalse();
        return this;
    }

    public UiValidator textEquals(String actual, String expected) {
        Assertions.assertThat(actual)
                .as("Text mismatch — expected <%s> but was <%s>", expected, actual)
                .isEqualTo(expected);
        return this;
    }

    public UiValidator textContains(String actual, String substring) {
        Assertions.assertThat(actual)
                .as("Text does not contain <%s>. Actual: <%s>", substring, actual)
                .contains(substring);
        return this;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Uses a short explicit wait instead of a raw {@code findElement()} call.
     * A raw call would immediately throw {@code NoSuchElementException} if the
     * element hasn't rendered yet, causing false negatives in fast-running tests.
     */
    private boolean isVisible(By locator) {
        try {
            return assertWait
                    .until(ExpectedConditions.visibilityOfElementLocated(locator))
                    .isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }
}
