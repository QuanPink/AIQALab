package com.framework.base;

import com.framework.core.config.ConfigManager;
import com.framework.core.driver.DriverFactory;
import com.framework.core.driver.DriverManager;
import com.framework.core.logger.LogManager;
import com.framework.core.report.AllureReportHelper;
import com.framework.core.report.ScreenshotHelper;
import org.testng.ITestResult;
import org.testng.annotations.*;

/**
 * Abstract base for all test classes.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Browser lifecycle — setup and teardown scoped to each test method</li>
 *   <li>Screenshot on failure — delegated to {@link ScreenshotHelper}</li>
 *   <li>Environment info attachment on the first suite run — via {@link AllureReportHelper}</li>
 *   <li>Test-result logging</li>
 * </ul>
 *
 * <p>API-only test classes must override {@link #needsDriver()} to return {@code false}
 * so no browser is started, keeping those test runs lightweight.
 */
@Listeners({com.framework.core.retry.RetryListener.class})
public abstract class BaseTest {

    // ── Suite-level hooks ─────────────────────────────────────────────────

    @BeforeSuite(alwaysRun = true)
    public void attachEnvironmentInfo() {
        AllureReportHelper.attachEnvironmentInfo();
    }

    // ── Method-level hooks ────────────────────────────────────────────────

    /**
     * Initialises the WebDriver before each test method.
     * The {@code browser} parameter comes from testng.xml; falls back to "chrome".
     */
    @BeforeMethod(alwaysRun = true)
    @Parameters("browser")
    public void setUpDriver(@Optional("chrome") String browser) {
        if (needsDriver()) {
            LogManager.info("[SETUP] Starting browser: " + browser);
            DriverManager.setDriver(DriverFactory.createDriver(browser));
            DriverManager.getDriver().get(ConfigManager.get("base.url"));
        }
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE && needsDriver()) {
            ScreenshotHelper.captureOnFailure(result.getName());
        }
        logTestResult(result);
        if (needsDriver()) {
            DriverManager.quitDriver();
        }
    }

    // ── Extension points ──────────────────────────────────────────────────

    /**
     * Override and return {@code false} in API-only test classes.
     * When {@code false}, no browser is started and no driver is cleaned up.
     */
    protected boolean needsDriver() {
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void logTestResult(ITestResult result) {
        String status;
        switch (result.getStatus()) {
            case ITestResult.SUCCESS: status = "PASSED";  break;
            case ITestResult.FAILURE: status = "FAILED";  break;
            case ITestResult.SKIP:    status = "SKIPPED"; break;
            default:                  status = "UNKNOWN";
        }
        LogManager.info(String.format("[RESULT] %s — %s", status, result.getName()));
    }
}
