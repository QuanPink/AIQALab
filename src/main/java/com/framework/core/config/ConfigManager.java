package com.framework.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton configuration manager with a 3-tier override chain.
 *
 * <h3>Priority (highest wins):</h3>
 * <ol>
 *   <li><b>JVM System Properties</b> — {@code -Dkey=value} on the Maven / test-runner command line.
 *       Highest priority; useful for one-off overrides.</li>
 *   <li><b>OS Environment Variables</b> — injected by CI/CD pipelines (GitHub Actions, Jenkins,
 *       GitLab CI) using the naming convention {@code KEY_NAME} → {@code key.name}
 *       (dots replaced by underscores, upper-cased).
 *       <em>Secrets (tokens, passwords) must only ever come from this source — never
 *       committed to source control.</em></li>
 *   <li><b>config-{env}.properties</b> — non-sensitive defaults loaded from the test classpath.
 *       Switch environments with {@code -Denv=staging}.</li>
 * </ol>
 *
 * <h3>Key-to-env-var mapping:</h3>
 * <pre>
 *   api.token        →  API_TOKEN
 *   api.base.url     →  API_BASE_URL
 *   base.url         →  BASE_URL
 * </pre>
 */
public class ConfigManager {

    private static volatile ConfigManager instance;
    private final Properties properties = new Properties();

    private ConfigManager() {
        loadConfig();
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }

    private void loadConfig() {
        String env = System.getProperty("env", "dev");
        String resourcePath = "config/config-" + env + ".properties";

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new RuntimeException("Config file not found on classpath: " + resourcePath);
            }
            properties.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config: " + resourcePath, e);
        }
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Returns the value for {@code key} using the 3-tier priority chain.
     *
     * @throws RuntimeException if the key is absent from all three sources
     */
    public static String get(String key) {
        String value = resolve(key);
        if (value == null) {
            throw new RuntimeException(
                    "Config key not found in any source (system property, env var, or properties file): '"
                    + key + "'"
            );
        }
        return value;
    }

    public static String get(String key, String defaultValue) {
        String value = resolve(key);
        return value != null ? value : defaultValue;
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }

    // ── Resolution chain ──────────────────────────────────────────────────

    /**
     * Walks the priority chain and returns the first non-null value found,
     * or {@code null} if the key is absent from all sources.
     *
     * <p>Chain: JVM system property → OS env var → properties file
     */
    private static String resolve(String key) {
        // [1] JVM system property — highest priority
        String systemProp = System.getProperty(key);
        if (systemProp != null) return systemProp;

        // [2] OS environment variable — convert "api.token" → "API_TOKEN"
        String envVar = System.getenv(toEnvVarName(key));
        if (envVar != null) return envVar;

        // [3] Properties file — lowest priority
        return getInstance().properties.getProperty(key);
    }

    /**
     * Converts a dot-separated property key to the OS env var naming convention.
     *
     * <pre>
     *   "api.token"    →  "API_TOKEN"
     *   "api.base.url" →  "API_BASE_URL"
     *   "base.url"     →  "BASE_URL"
     * </pre>
     */
    static String toEnvVarName(String key) {
        return key.toUpperCase().replace('.', '_');
    }
}
