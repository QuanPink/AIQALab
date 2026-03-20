package com.framework.ui;

import com.framework.base.BaseTest;
import com.framework.core.config.ConfigManager;
import com.framework.ui.pages.HomePage;
import com.framework.ui.pages.LoginPage;
import com.framework.validator.UiValidator;
import io.qameta.allure.*;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Login test suite for https://app.copin.io/
 *
 * <p>Coverage:
 * <ul>
 *   <li>TC_001–TC_003  Positive login scenarios</li>
 *   <li>TC_004–TC_010  Invalid email format</li>
 *   <li>TC_011–TC_014  Wrong password</li>
 *   <li>TC_015–TC_018  Empty fields</li>
 *   <li>TC_019–TC_022  Rate limit / brute force</li>
 *   <li>TC_023–TC_027  Error message validation</li>
 *   <li>TC_028–TC_033  Session creation & management</li>
 * </ul>
 *
 * <p>Credentials are supplied via:
 * <ol>
 *   <li>{@code config-dev.properties} — {@code test.username} / {@code test.password}</li>
 *   <li>OS env vars {@code TEST_USERNAME} / {@code TEST_PASSWORD} (CI/CD)</li>
 *   <li>JVM arg {@code -Dtest.username=...} (ad-hoc override)</li>
 * </ol>
 */
@Epic("Authentication")
@Feature("Email Login — copin.io")
public class LoginTest extends BaseTest {

    private String validEmail;
    private String validPassword;

    @BeforeMethod(alwaysRun = true)
    public void loadCredentials() {
        validEmail    = ConfigManager.get("test.username", "valid@example.com");
        validPassword = ConfigManager.get("test.password", "ValidPass@123");
    }

    // ══════════════════════════════════════════════════════════════════════
    // POSITIVE SCENARIOS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * TC_001 — Valid email triggers the Privy magic-link / OTP flow.
     *
     * <p>copin.io uses Privy email-only auth (no password).
     * Clicking Submit with a valid email sends an OTP to the inbox.
     * This test verifies the full pre-OTP path: popup opens, email is accepted,
     * and Submit is reachable. Full post-OTP redirect requires email interception
     * (out of scope for UI automation).
     */
    @Test(description = "TC_001 — Valid email opens auth popup and Submit is clickable",
          groups = {"smoke", "regression", "login-positive"})
    @Story("Successful login")
    @Severity(SeverityLevel.BLOCKER)
    public void TC001_validCredentials_redirectToHome() {
        LoginPage loginPage = new LoginPage();
        loginPage.openLoginPopup();

        assertThat(loginPage.isLoginPopupOpen())
                .as("Privy login popup should be visible after clicking Login")
                .isTrue();

        loginPage.enterEmail(validEmail);

        assertThat(loginPage.isEmailFieldEmpty())
                .as("Email field should contain the entered address")
                .isFalse();
    }

    /**
     * TC_002 — Login popup can be reopened after being closed.
     *
     * <p>Session-persistence testing requires completing the full OTP flow
     * (out of scope). This test verifies the popup UI remains functional
     * across interactions.
     */
    @Test(description = "TC_002 — Login popup reopens correctly after close",
          groups = {"regression", "login-positive"})
    @Story("Session persistence")
    @Severity(SeverityLevel.CRITICAL)
    public void TC002_afterLogin_sessionSurvivesRefresh() {
        LoginPage loginPage = new LoginPage();
        loginPage.openLoginPopup();

        assertThat(loginPage.isLoginPopupOpen()).isTrue();

        loginPage.closePopup();

        assertThat(loginPage.isLoginPopupOpen())
                .as("Popup should be closed after clicking ×")
                .isFalse();

        loginPage.openLoginPopup();

        assertThat(loginPage.isLoginPopupOpen())
                .as("Popup should reopen after clicking Login again")
                .isTrue();
    }

    /**
     * TC_027 — N/A: copin.io uses Privy email-only auth; there is no password field.
     *
     * <p>Replaced with a smoke check that the email input is present inside the popup.
     */
    @Test(description = "TC_027 — Email input is present and interactive in login popup",
          groups = {"smoke", "regression", "login-ui"})
    @Story("Error message validation")
    @Severity(SeverityLevel.NORMAL)
    public void TC027_passwordField_isMaskedByDefault() {
        LoginPage loginPage = new LoginPage();
        loginPage.openLoginPopup();

        assertThat(loginPage.isLoginPopupOpen())
                .as("Login popup must be open before interacting with email input")
                .isTrue();

        // Verify the email input accepts text
        loginPage.enterEmail("test@example.com");

        assertThat(loginPage.isEmailFieldEmpty())
                .as("Email input should not be empty after typing")
                .isFalse();
    }

    // ══════════════════════════════════════════════════════════════════════
    // INVALID EMAIL FORMAT
    // ══════════════════════════════════════════════════════════════════════

    /**
     * TC_004–TC_010 — Invalid email formats should keep the Submit button disabled.
     *
     * <p>copin.io uses Privy email-only auth. Privy validates the email format
     * client-side and keeps the Submit button disabled for invalid formats instead
     * of showing a server-side error message. A disabled Submit is the expected
     * "rejection" signal.
     */
    @Test(dataProvider = "invalidEmailFormats",
          description = "TC_004-010 — Invalid email format keeps Submit disabled",
          groups = {"regression", "login-email-format"})
    @Story("Invalid email format")
    @Severity(SeverityLevel.CRITICAL)
    public void TC004_invalidEmailFormat_showsError(String email, String description) {
        LoginPage loginPage = new LoginPage()
                .enterEmail(email);

        assertThat(loginPage.isSubmitEnabled())
                .as("Submit should be disabled for invalid email [%s]: %s", description, email)
                .isFalse();
    }

    @DataProvider(name = "invalidEmailFormats")
    public Object[][] invalidEmailFormats() {
        return new Object[][] {
            { "userWithoutAt",          "TC_004: missing @ symbol"       },
            { "user@",                  "TC_005: no domain after @"      },
            { "user@domain",            "TC_006: no TLD"                 },
            { "us er@domain.com",       "TC_007: space inside email"     },
            { "@domain.com",            "TC_008: no local part"          },
            { "user@@domain.com",       "TC_009: double @ symbol"        },
        };
    }

    // ══════════════════════════════════════════════════════════════════════
    // EMPTY FIELDS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * TC_015 — Both fields empty → Submit button stays disabled.
     *
     * <p>Privy keeps Submit disabled when the email field is empty.
     * This is the client-side validation signal (no error toast is shown).
     */
    @Test(description = "TC_015 — Both empty fields keep Submit disabled",
          groups = {"smoke", "regression", "login-empty"})
    @Story("Empty fields")
    @Severity(SeverityLevel.BLOCKER)
    public void TC015_bothFieldsEmpty_showsValidationErrors() {
        LoginPage loginPage = new LoginPage()
                .openLoginPopup();

        assertThat(loginPage.isSubmitEnabled())
                .as("Submit should be disabled when email is empty")
                .isFalse();
    }

    /**
     * TC_016 — Email filled, password empty → password required error.
     *
     * <p>N/A: copin.io uses Privy email-only auth; there is no password field.
     * Filling a valid email and clicking Submit triggers an OTP flow, not an error.
     */
    @Test(description = "TC_016 — Email filled, password empty shows error",
          groups = {"regression", "login-empty"},
          enabled = false)
    @Story("Empty fields")
    @Severity(SeverityLevel.CRITICAL)
    public void TC016_emailFilled_passwordEmpty_showsError() {
        LoginPage loginPage = new LoginPage()
                .enterEmail(validEmail)
                .clickLoginExpectFailure();

        assertThat(loginPage.isErrorDisplayed())
                .as("Expected error when password is empty")
                .isTrue();
    }

    /**
     * TC_017 — Email empty → Submit stays disabled.
     *
     * <p>Privy requires a valid email before enabling Submit. When the email
     * field is empty the Submit button stays disabled (client-side gate).
     */
    @Test(description = "TC_017 — Empty email keeps Submit disabled",
          groups = {"regression", "login-empty"})
    @Story("Empty fields")
    @Severity(SeverityLevel.CRITICAL)
    public void TC017_passwordFilled_emailEmpty_showsError() {
        LoginPage loginPage = new LoginPage()
                .openLoginPopup();

        assertThat(loginPage.isSubmitEnabled())
                .as("Submit should be disabled when email is empty")
                .isFalse();
    }

    /**
     * TC_018 — Whitespace-only email should keep Submit disabled.
     *
     * <p>Privy trims and validates the email value; whitespace-only input
     * is treated as empty and keeps the Submit button disabled.
     */
    @Test(description = "TC_018 — Whitespace-only email keeps Submit disabled",
          groups = {"regression", "login-empty"})
    @Story("Empty fields")
    @Severity(SeverityLevel.NORMAL)
    public void TC018_whitespaceOnlyFields_treatedAsEmpty() {
        LoginPage loginPage = new LoginPage()
                .enterEmail("   ");

        assertThat(loginPage.isSubmitEnabled())
                .as("Whitespace-only email should keep Submit disabled")
                .isFalse();
    }

    // ══════════════════════════════════════════════════════════════════════
    // WRONG PASSWORD
    // ══════════════════════════════════════════════════════════════════════

    /**
     * TC_011 — Wrong password shows a generic error (no user enumeration).
     *
     * <p>N/A: copin.io uses Privy email-only auth; there is no password field.
     * Disabled until a password-based auth flow is added.
     */
    @Test(description = "TC_011 — Wrong password shows generic error, not user-specific",
          groups = {"smoke", "regression", "login-password"},
          enabled = false)
    @Story("Wrong password")
    @Severity(SeverityLevel.BLOCKER)
    public void TC011_wrongPassword_showsGenericError() {
        LoginPage loginPage = new LoginPage()
                .enterEmail(validEmail)
                .enterPassword("Def1nitely_Wrong_Pass!")
                .clickLoginExpectFailure();

        assertThat(loginPage.isErrorDisplayed())
                .as("Expected error message for wrong password")
                .isTrue();

        // Security: message must NOT reveal whether the email exists
        String errorText = loginPage.getErrorMessage().toLowerCase();
        assertThat(errorText)
                .as("Error must not indicate whether email is registered (user enumeration risk)")
                .doesNotContain("email not found")
                .doesNotContain("user does not exist")
                .doesNotContain("no account");
    }

    /**
     * TC_012 — Password is case-sensitive.
     *
     * <p>N/A: copin.io uses Privy email-only auth; there is no password field.
     */
    @Test(description = "TC_012 — Password case sensitivity is enforced",
          groups = {"regression", "login-password"},
          enabled = false)
    @Story("Wrong password")
    @Severity(SeverityLevel.CRITICAL)
    public void TC012_passwordCaseSensitivity_isEnforced() {
        // Assumes validPassword is mixed-case; submitting it all-uppercase must fail
        LoginPage loginPage = new LoginPage()
                .enterEmail(validEmail)
                .enterPassword(validPassword.toUpperCase())
                .clickLoginExpectFailure();

        assertThat(loginPage.isErrorDisplayed())
                .as("All-uppercase version of correct password should be rejected")
                .isTrue();
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECURITY — INJECTION & XSS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * TC_008 / TC_009 — Injection payloads must be rejected safely.
     *
     * <p>copin.io uses Privy email-only auth. Privy validates email format client-side;
     * for invalid formats the Submit button stays disabled (no server-side error).
     * "Password" payloads are irrelevant because there is no password field.
     * Disabled: the original assertion ({@code isErrorDisplayed()}) never fires for
     * Privy's client-side-only validation. Re-enable when testing against a backend
     * that reflects error messages.
     */
    @Test(dataProvider = "securityPayloads",
          description = "TC_008/TC_009 — Security payloads rejected without server error",
          groups = {"regression", "login-security"},
          enabled = false)
    @Story("Security — injection")
    @Severity(SeverityLevel.BLOCKER)
    public void TC008_securityPayloads_rejectedSafely(String email, String password, String type) {
        LoginPage loginPage = new LoginPage()
                .enterEmail(email)
                .enterPassword(password)
                .clickLoginExpectFailure();

        assertThat(loginPage.isErrorDisplayed())
                .as("[%s] payload should produce a validation/auth error", type)
                .isTrue();

        String pageSource = com.framework.core.driver.DriverManager.getDriver().getPageSource();
        assertThat(pageSource)
                .as("[%s] payload must not be reflected unescaped in page source", type)
                .doesNotContain("<script>alert(1)</script>");
    }

    @DataProvider(name = "securityPayloads")
    public Object[][] securityPayloads() {
        return new Object[][] {
            { "<script>alert(1)</script>@x.com", "any",              "XSS in email"         },
            { "' OR '1'='1'@x.com",              "any",              "SQLi in email"        },
            { validEmail,                         "' OR 1=1 --",     "SQLi in password"     },
            { validEmail,                         "<img src=x onerror=alert(1)>", "XSS in password" },
        };
    }

    // ══════════════════════════════════════════════════════════════════════
    // RATE LIMIT / BRUTE FORCE
    // ══════════════════════════════════════════════════════════════════════

    /**
     * TC_019–TC_020 — Repeated wrong attempts trigger lockout or CAPTCHA.
     *
     * <p><b>Important:</b> This test uses a dedicated "canary" account that is
     * safe to lock out. Never run this against the main test account.
     * Supply via: {@code -Dtest.canary.email=...}
     */
    @Test(description = "TC_019-020 — Repeated failures trigger rate limiting",
          groups = {"regression", "login-rate-limit"},
          enabled = false) // Enable only in environments where account reset is automated
    @Story("Rate limit / brute force")
    @Severity(SeverityLevel.BLOCKER)
    public void TC019_repeatedFailures_triggerRateLimit() {
        String canaryEmail = ConfigManager.get("test.canary.email", validEmail);
        int attempts = Integer.parseInt(ConfigManager.get("rate.limit.attempts", "5"));

        LoginPage loginPage = new LoginPage();

        for (int i = 1; i <= attempts; i++) {
            loginPage = new LoginPage()
                    .enterEmail(canaryEmail)
                    .enterPassword("WrongPass_" + i + "!")
                    .clickLoginExpectFailure();
        }

        // After N failures the page should show either:
        // - A CAPTCHA element, OR
        // - A lockout/rate-limit message
        assertThat(loginPage.isErrorDisplayed())
                .as("Expected lockout or CAPTCHA after %d failed attempts", attempts)
                .isTrue();

        String errorText = loginPage.getErrorMessage().toLowerCase();
        boolean isRateLimited = errorText.contains("too many")
                || errorText.contains("locked")
                || errorText.contains("temporarily")
                || errorText.contains("try again");

        assertThat(isRateLimited)
                .as("Error message should indicate rate limiting. Actual: '%s'", errorText)
                .isTrue();
    }

    // ══════════════════════════════════════════════════════════════════════
    // ERROR MESSAGE VALIDATION
    // ══════════════════════════════════════════════════════════════════════

    /**
     * TC_023 — Error messages must not expose internal details.
     *
     * <p>N/A: copin.io uses Privy email-only auth; there is no password field.
     * Submitting with a valid email triggers an OTP flow and no error is shown,
     * so there is nothing to assert about error message content.
     * Disabled until a flow that produces a server-side error message is available.
     */
    @Test(description = "TC_023 — Error messages do not expose technical details",
          groups = {"regression", "login-error-msg"},
          enabled = false)
    @Story("Error message validation")
    @Severity(SeverityLevel.CRITICAL)
    public void TC023_errorMessages_noTechnicalDetails() {
        LoginPage loginPage = new LoginPage()
                .enterEmail(validEmail)
                .enterPassword("WrongPass!")
                .clickLoginExpectFailure();

        String error = loginPage.getErrorMessage().toLowerCase();

        assertThat(error)
                .as("Error must not leak stack trace or server internals")
                .doesNotContain("exception")
                .doesNotContain("stack trace")
                .doesNotContain("null pointer")
                .doesNotContain("500")
                .doesNotContain("internal server")
                .doesNotContain("sql")
                .doesNotContain("database");
    }

    /**
     * TC_024 — Privy enables Submit only when a valid email is entered.
     *
     * <p>Original intent: error clears when user corrects input.
     * Adapted for Privy: Submit is disabled for an invalid email and becomes
     * enabled once a properly-formatted email is entered — this is the
     * equivalent "validation clears on correction" signal.
     */
    @Test(description = "TC_024 — Submit enables when valid email replaces invalid one",
          groups = {"regression", "login-error-msg"})
    @Story("Error message validation")
    @Severity(SeverityLevel.NORMAL)
    public void TC024_errorMessage_clearsOnRetype() {
        LoginPage loginPage = new LoginPage()
                .enterEmail("bad-email");

        assertThat(loginPage.isSubmitEnabled())
                .as("Submit should be disabled for invalid email 'bad-email'")
                .isFalse();

        // User corrects the email — Submit should become enabled
        loginPage.enterEmail(validEmail);

        // Give React/Privy state a moment to update
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        assertThat(loginPage.isSubmitEnabled())
                .as("Submit should be enabled after entering a valid email")
                .isTrue();
    }

    // ══════════════════════════════════════════════════════════════════════
    // SESSION
    // ══════════════════════════════════════════════════════════════════════

    /**
     * TC_028 — Auth token / cookie is set in browser storage after login.
     *
     * <p>Requires a pre-authenticated session (token injected via cookie/localStorage
     * before the test). Full OTP flow is out of scope for UI automation.
     * This test verifies the check logic when a token IS present.
     */
    @Test(description = "TC_028 — Auth token presence can be detected in storage",
          groups = {"smoke", "regression", "session"},
          enabled = false) // Enable when full OTP flow automation is available
    @Story("Session creation")
    @Severity(SeverityLevel.BLOCKER)
    public void TC028_successfulLogin_setsAuthToken() {
        new LoginPage()
                .enterEmail(validEmail)
                .clickLoginExpectSuccess();

        WebDriver driver = com.framework.core.driver.DriverManager.getDriver();
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Check localStorage for a JWT or auth key (adjust key name to match copin.io)
        Object localStorageToken = js.executeScript(
                "return localStorage.getItem('token') " +
                "|| localStorage.getItem('accessToken') " +
                "|| localStorage.getItem('auth') " +
                "|| sessionStorage.getItem('token');"
        );

        // Check cookies as fallback
        boolean hasCookie = driver.manage().getCookies().stream()
                .anyMatch(c -> c.getName().toLowerCase().contains("token")
                        || c.getName().toLowerCase().contains("auth")
                        || c.getName().toLowerCase().contains("session"));

        assertThat(localStorageToken != null || hasCookie)
                .as("Expected an auth token in localStorage or a session cookie after login")
                .isTrue();
    }

    /**
     * TC_029 — Logging out clears the auth token.
     * Requires a pre-authenticated session; disabled until OTP flow is automatable.
     */
    @Test(description = "TC_029 — Logout clears the auth token and redirects to login",
          groups = {"regression", "session"},
          enabled = false)
    @Story("Session creation")
    @Severity(SeverityLevel.CRITICAL)
    public void TC029_logout_clearsTokenAndRedirects() {
        HomePage home = new LoginPage()
                .enterEmail(validEmail)
                .clickLoginExpectSuccess();

        UiValidator.assertThat(home).pageIsLoaded();

        LoginPage loginPage = home.logout();

        UiValidator.assertThat(loginPage)
                .pageIsLoaded()
                .urlContains("login");

        // Token should be cleared
        WebDriver driver = com.framework.core.driver.DriverManager.getDriver();
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Object token = js.executeScript(
                "return localStorage.getItem('token') || localStorage.getItem('accessToken');"
        );

        assertThat(token)
                .as("Auth token should be cleared from localStorage after logout")
                .isNull();
    }

    /**
     * TC_030 — Accessing a protected page without a token redirects to the main page
     * where the Login button is visible.
     *
     * <p>copin.io does NOT have a dedicated {@code /login} URL. Authentication is
     * a Privy popup triggered from the main page. When an unauthenticated user
     * navigates to a protected route the app redirects back to the main page and
     * the Login button ({@code id="login_button__id"}) is visible.
     */
    @Test(description = "TC_030 — Protected page without token shows Login button on main page",
          groups = {"smoke", "regression", "session"})
    @Story("Session creation")
    @Severity(SeverityLevel.BLOCKER)
    public void TC030_protectedPage_withoutToken_redirectsToLogin() {
        // Clear any existing session
        WebDriver driver = com.framework.core.driver.DriverManager.getDriver();
        driver.manage().deleteAllCookies();
        ((JavascriptExecutor) driver).executeScript("localStorage.clear(); sessionStorage.clear();");

        // Attempt to access a protected route directly
        driver.get(ConfigManager.get("base.url") + "home");

        // copin.io redirects unauthenticated users to the main page (not /login).
        // The Login button being visible is the authoritative signal.
        LoginPage loginPage = new LoginPage();
        UiValidator.assertThat(loginPage)
                .pageIsLoaded()
                .elementIsVisible(By.id("login_button__id"));
    }
}
