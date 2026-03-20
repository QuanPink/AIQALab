package com.framework.utils;

import com.framework.core.config.ConfigManager;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Explicit wait helper. All UI interactions should go through this class
 * rather than raw {@code Thread.sleep()} calls.
 *
 * <p>Two wait durations are used:
 * <ul>
 *   <li><b>wait</b>  — {@code explicit.wait.seconds} (default 15 s) for expected elements</li>
 *   <li><b>shortWait</b> — {@code short.wait.seconds} (default 5 s) for presence checks</li>
 * </ul>
 */
public class WaitHelper {

    private final WebDriverWait wait;
    private final WebDriverWait shortWait;

    public WaitHelper(WebDriver driver) {
        int timeout      = ConfigManager.getInt("explicit.wait.seconds", 15);
        int shortTimeout = ConfigManager.getInt("short.wait.seconds", 5);
        this.wait      = new WebDriverWait(driver, Duration.ofSeconds(timeout));
        this.shortWait = new WebDriverWait(driver, Duration.ofSeconds(shortTimeout));
    }

    public WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    public WebElement waitForPresence(By locator) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    public List<WebElement> waitForAllVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    public boolean waitForInvisible(By locator) {
        return shortWait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public boolean waitForUrlContains(String fragment) {
        return wait.until(ExpectedConditions.urlContains(fragment));
    }

    public boolean waitForTitleContains(String title) {
        return wait.until(ExpectedConditions.titleContains(title));
    }

    /**
     * Returns {@code true} if an element matching {@code locator} is present in the DOM
     * within the short-wait timeout. Does NOT require the element to be visible.
     */
    public boolean isElementPresent(By locator) {
        try {
            shortWait.until(ExpectedConditions.presenceOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** Exposes the full-duration wait for custom {@code ExpectedConditions}. */
    public WebDriverWait getWait() {
        return wait;
    }

    /** Exposes the short-duration wait for custom {@code ExpectedConditions}. */
    public WebDriverWait getShortWait() {
        return shortWait;
    }
}
