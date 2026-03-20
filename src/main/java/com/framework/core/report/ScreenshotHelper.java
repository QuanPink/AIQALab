package com.framework.core.report;

import com.framework.core.driver.DriverManager;
import com.framework.core.logger.LogManager;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.ByteArrayInputStream;

/**
 * Captures browser screenshots and attaches them to the Allure report.
 *
 * <p>Extracted from {@code BaseTest} to keep screenshot concerns separate
 * from test lifecycle management (Single Responsibility Principle).
 *
 * <p>Usage:
 * <pre>{@code
 * ScreenshotHelper.captureOnFailure(result.getName());
 * }</pre>
 */
public class ScreenshotHelper {

    private ScreenshotHelper() {}

    /**
     * Takes a full-page screenshot and attaches it to the current Allure test step.
     * Silently logs and returns if no driver is active or the capture fails.
     *
     * @param label the attachment label shown in the Allure report
     */
    public static void capture(String label) {
        if (!DriverManager.hasDriver()) {
            LogManager.debug("[SCREENSHOT] Skipped — no active driver");
            return;
        }
        try {
            byte[] bytes = ((TakesScreenshot) DriverManager.getDriver())
                    .getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment(label, "image/png", new ByteArrayInputStream(bytes), ".png");
            LogManager.info("[SCREENSHOT] Attached: " + label);
        } catch (Exception e) {
            LogManager.error("[SCREENSHOT] Failed to capture '" + label + "': " + e.getMessage());
        }
    }

    /**
     * Convenience method — captures a screenshot labelled for a specific failed test.
     *
     * @param testName the test method name
     */
    public static void captureOnFailure(String testName) {
        capture("FAILURE — " + testName);
    }
}
