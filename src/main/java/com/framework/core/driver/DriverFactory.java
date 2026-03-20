package com.framework.core.driver;

import com.framework.core.config.ConfigManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;

/**
 * Factory that creates fully configured {@link WebDriver} instances.
 *
 * <p>Applies timeouts from config so every driver created by this factory
 * behaves consistently regardless of which test class instantiates it:
 * <ul>
 *   <li>{@code page.load.timeout} — max seconds to wait for a page to load</li>
 *   <li>{@code explicit.wait.seconds} — used separately via {@link com.framework.utils.WaitHelper}</li>
 * </ul>
 */
public class DriverFactory {

    private DriverFactory() {}

    public static WebDriver createDriver(String browser) {
        switch (browser.toLowerCase()) {
            case "chrome":
                return configure(createChromeDriver());
            case "firefox":
                return configure(createFirefoxDriver());
            default:
                throw new IllegalArgumentException(
                        "Unsupported browser: '" + browser + "'. Supported: chrome, firefox");
        }
    }

    // ── Browser creators ──────────────────────────────────────────────────

    private static WebDriver createChromeDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        if (ConfigManager.getBoolean("headless", false)) {
            options.addArguments("--headless=new");
        }
        options.addArguments(
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--window-size=1920,1080",
                "--disable-extensions",
                "--disable-gpu"
        );
        return new ChromeDriver(options);
    }

    private static WebDriver createFirefoxDriver() {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();
        if (ConfigManager.getBoolean("headless", false)) {
            options.addArguments("--headless");
        }
        return new FirefoxDriver(options);
    }

    // ── Common post-creation configuration ───────────────────────────────

    /**
     * Applies timeout settings from config to a freshly created driver.
     * Centralising this here means all browser types get identical timeout behaviour.
     */
    private static WebDriver configure(WebDriver driver) {
        int pageLoadTimeout = ConfigManager.getInt("page.load.timeout", 30);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoadTimeout));
        return driver;
    }
}
