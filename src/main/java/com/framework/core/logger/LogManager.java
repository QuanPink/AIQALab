package com.framework.core.logger;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Static logging façade over Log4j2 with two usage modes:
 *
 * <p><b>Mode 1 — instance logger (preferred for frequent logging):</b>
 * <pre>{@code
 * private static final org.apache.logging.log4j.Logger LOG =
 *     LogManager.getLogger(MyClass.class);
 * LOG.info("Fast, zero stack-walk overhead");
 * }</pre>
 *
 * <p><b>Mode 2 — static convenience methods (acceptable for occasional use):</b>
 * <pre>{@code
 * LogManager.info("Quick one-liner, minimal overhead via stack cache");
 * }</pre>
 *
 * <p>Static methods resolve the caller class via a stack walk, then cache the
 * resulting Logger by class name — so the walk happens at most once per calling class.
 */
public class LogManager {

    /** Cache Logger instances by class name to avoid repeated Log4j2 lookup. */
    private static final ConcurrentHashMap<String, org.apache.logging.log4j.Logger> CACHE =
            new ConcurrentHashMap<>();

    private LogManager() {}

    // ── Factory (preferred) ───────────────────────────────────────────────

    /**
     * Returns a Log4j2 {@code Logger} for the given class.
     * The instance is cached, so repeated calls with the same class are free.
     */
    public static org.apache.logging.log4j.Logger getLogger(Class<?> clazz) {
        return CACHE.computeIfAbsent(
                clazz.getName(),
                org.apache.logging.log4j.LogManager::getLogger
        );
    }

    // ── Static convenience methods ────────────────────────────────────────

    public static void info(String message)  { resolveLogger().info(message); }
    public static void debug(String message) { resolveLogger().debug(message); }
    public static void warn(String message)  { resolveLogger().warn(message); }
    public static void error(String message) { resolveLogger().error(message); }

    public static void error(String message, Throwable t) {
        resolveLogger().error(message, t);
    }

    // ── Internal ──────────────────────────────────────────────────────────

    /**
     * Walks the call stack to find the first frame outside this class and
     * {@code java.lang.Thread}, then returns a cached Logger for that class.
     *
     * <p>The cache ensures Logger creation happens at most once per caller class,
     * so the only ongoing cost is the {@link Thread#getStackTrace()} call itself.
     * This is acceptable for test-framework code; performance-critical classes
     * should use {@link #getLogger(Class)} instead.
     */
    private static org.apache.logging.log4j.Logger resolveLogger() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement el : stack) {
            String name = el.getClassName();
            if (!name.startsWith("com.framework.core.logger")
                    && !name.startsWith("java.lang.Thread")) {
                return CACHE.computeIfAbsent(
                        name,
                        org.apache.logging.log4j.LogManager::getLogger
                );
            }
        }
        return CACHE.computeIfAbsent(
                LogManager.class.getName(),
                org.apache.logging.log4j.LogManager::getLogger
        );
    }
}
