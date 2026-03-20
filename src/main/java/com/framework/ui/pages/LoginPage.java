package com.framework.ui.pages;

import com.framework.core.logger.LogManager;
import com.framework.ui.base.BasePage;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Page Object for the copin.io login flow.
 *
 * <p><b>Actual UI flow (verified from DOM):</b>
 * <ol>
 *   <li>Main page (https://app.copin.io/) — click the "Login" button
 *       {@code id="login_button__id"}</li>
 *   <li>A Privy auth popup appears — {@code id="privy-dialog"}</li>
 *   <li>Inside the popup — enter email in {@code id="email-input"}</li>
 *   <li>Click Submit (enabled only after email is typed)</li>
 * </ol>
 *
 * <p><b>Note:</b> copin.io uses Privy for authentication.
 * There is <em>no password field</em> on the initial login popup.
 * Authentication is email-only (magic link / OTP sent to the inbox).
 * Any call to {@link #enterPassword(String)} is a documented no-op.
 */
public class LoginPage extends BasePage {

    // ── Step 1: Main page ─────────────────────────────────────────────────
    /** The "Login" button visible on the main page header. */
    private static final By BTN_OPEN_LOGIN = By.id("login_button__id");

    // ── Step 2: Privy auth dialog ─────────────────────────────────────────
    /** Root of the Privy auth popup. Becomes visible after clicking BTN_OPEN_LOGIN. */
    private static final By PRIVY_DIALOG = By.id("privy-dialog");

    /** The × close button inside the Privy popup. */
    private static final By BTN_CLOSE_MODAL =
            By.cssSelector("#privy-dialog button[aria-label='close modal']");

    // ── Step 3 & 4: Email form inside the popup ───────────────────────────
    /** Email input — stable {@code id} attribute, unlikely to change. */
    private static final By EMAIL_INPUT = By.id("email-input");

    /**
     * Submit button inside the Privy popup.
     * The button is {@code disabled} until the user types a non-empty email;
     * {@link #click(By)} already waits until it becomes clickable.
     * XPath is used because styled-component class hashes change on build.
     */
    private static final By BTN_SUBMIT =
            By.xpath("//div[@id='privy-dialog']//button[.//span[normalize-space(text())='Submit']]");

    // ── Error / validation ────────────────────────────────────────────────
    /**
     * Error or alert element rendered inside the Privy modal content.
     * Privy typically surfaces form errors via {@code [role='alert']} or
     * a class that contains "error".
     */
    private static final By ERROR_MESSAGE = By.cssSelector(
            "#privy-modal-content [role='alert'], " +
            "#privy-modal-content [class*='error'], " +
            "#privy-modal-content [class*='Error']"
    );

    /** Loaded marker — the Login button on the main page. */
    private static final By PAGE_LOADED_MARKER = By.id("login_button__id");

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Opens the Privy popup if it is not already visible. */
    private void ensurePopupOpen() {
        if (!isDisplayed(PRIVY_DIALOG)) {
            click(BTN_OPEN_LOGIN);
            getWaitHelper().waitForVisible(PRIVY_DIALOG);
        }
    }

    /**
     * Force-clicks an element via JavaScript.
     * Used when the target element is disabled (e.g. Submit with empty email)
     * and we want to verify the app's response to a programmatic submit attempt.
     */
    private void jsClick(By locator) {
        WebElement el = getWaitHelper().waitForPresence(locator);
        executeScript("arguments[0].click();", el);
    }

    // ── Actions ───────────────────────────────────────────────────────────

    @Step("Open login popup")
    public LoginPage openLoginPopup() {
        ensurePopupOpen();
        return this;
    }

    @Step("Enter email: {email}")
    public LoginPage enterEmail(String email) {
        ensurePopupOpen();
        clearAndType(EMAIL_INPUT, email);
        return this;
    }

    /**
     * No-op — copin.io uses Privy email-only auth; there is no password field.
     * Kept for source compatibility with existing test cases.
     */
    public LoginPage enterPassword(String password) {
        LogManager.warn("enterPassword() called but copin.io has no password field " +
                        "(Privy email-only auth). Call is ignored.");
        return this;
    }

    @Step("Click Login — expect success (navigates away from popup)")
    public HomePage clickLoginExpectSuccess() {
        ensurePopupOpen();
        click(BTN_SUBMIT);
        return new HomePage();
    }

    @Step("Click Login — expect failure / validation error")
    public LoginPage clickLoginExpectFailure() {
        ensurePopupOpen();
        // If submit is enabled, click normally; otherwise force-click so the
        // browser/app has a chance to show validation feedback.
        if (isEnabled(BTN_SUBMIT)) {
            click(BTN_SUBMIT);
        } else {
            jsClick(BTN_SUBMIT);
        }
        return this;
    }

    @Step("Close login popup")
    public LoginPage closePopup() {
        click(BTN_CLOSE_MODAL);
        getWaitHelper().waitForInvisible(PRIVY_DIALOG);
        return this;
    }

    // ── Queries ───────────────────────────────────────────────────────────

    public String getErrorMessage() {
        return getText(ERROR_MESSAGE);
    }

    public boolean isErrorDisplayed() {
        return isDisplayed(ERROR_MESSAGE);
    }

    public boolean isLoginPopupOpen() {
        return isDisplayed(PRIVY_DIALOG);
    }

    /**
     * Returns {@code false} — there is no password field in the Privy popup.
     * Tests checking password masking are not applicable to this auth flow.
     */
    public boolean isPasswordMasked() {
        LogManager.warn("isPasswordMasked() — no password field in Privy auth flow.");
        return false;
    }

    /** Returns {@code true} if the Submit button is currently enabled (email format is valid). */
    public boolean isSubmitEnabled() {
        return isEnabled(BTN_SUBMIT);
    }

    public boolean isEmailFieldEmpty() {
        String val = getAttribute(EMAIL_INPUT, "value");
        return val == null || val.trim().isEmpty();
    }

    @Override
    public boolean isPageLoaded() {
        return isDisplayed(PAGE_LOADED_MARKER);
    }
}
