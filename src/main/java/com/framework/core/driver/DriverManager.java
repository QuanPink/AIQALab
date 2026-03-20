package com.framework.core.driver;

import org.openqa.selenium.WebDriver;

/**
 * ThreadLocal-based WebDriver holder.
 * Each thread gets its own isolated driver instance,
 * making parallel test execution safe without synchronisation overhead.
 */
public class DriverManager {

    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    private DriverManager() {}

    public static WebDriver getDriver() {
        return driverThreadLocal.get();
    }

    public static void setDriver(WebDriver driver) {
        driverThreadLocal.set(driver);
    }

    /** Quits the browser and removes the reference to prevent memory leaks. */
    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            driver.quit();
            driverThreadLocal.remove();
        }
    }

    public static boolean hasDriver() {
        return driverThreadLocal.get() != null;
    }
}
