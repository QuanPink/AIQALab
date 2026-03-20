package com.framework.core.retry;

import com.framework.core.config.ConfigManager;
import com.framework.core.logger.LogManager;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Retries failed tests up to {@code retry.max.count} times (default: 2).
 *
 * <p>TestNG creates a new {@code RetryAnalyzer} instance per test method,
 * so {@code retryCount} correctly resets for each test.
 *
 * <p>Applied automatically to all tests via {@link RetryListener}.
 * Override per-test with {@code @Test(retryAnalyzer = RetryAnalyzer.class)}.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    /** Read once at construction — avoids a ConfigManager lookup on every failure. */
    private final int maxRetry = ConfigManager.getInt("retry.max.count", 2);
    private int retryCount = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < maxRetry) {
            retryCount++;
            LogManager.warn(String.format(
                    "[RETRY] '%s' failed — attempt %d/%d",
                    result.getName(), retryCount, maxRetry
            ));
            return true;
        }
        return false;
    }
}
