package com.framework.base;

/**
 * Base class for all API-only test classes.
 *
 * <p>Disables WebDriver lifecycle from {@link BaseTest} so no browser is
 * launched. Every API test class should extend this instead of extending
 * {@code BaseTest} directly and overriding {@code needsDriver()}.
 *
 * <p>Usage:
 * <pre>{@code
 * public class UserApiTest extends BaseApiTest {
 *     // no needsDriver() override needed
 * }
 * }</pre>
 */
public abstract class BaseApiTest extends BaseTest {

    @Override
    protected final boolean needsDriver() {
        return false;
    }
}
