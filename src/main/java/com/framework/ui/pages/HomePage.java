package com.framework.ui.pages;

import com.framework.ui.base.BasePage;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

/**
 * Page Object for the Copin.io Home / Dashboard page shown after successful login.
 *
 * <p>This page is reached at the root URL after authentication.
 * Adjust locators if the post-login route changes (e.g. /home, /dashboard).
 */
public class HomePage extends BasePage {

    // ── Locators ──────────────────────────────────────────────────────────
    // TODO: Update after inspecting DOM post-login on https://app.copin.io/

    private static final By USER_AVATAR      = By.cssSelector("[class*='avatar'], [class*='Avatar'], img[alt*='user']");
    private static final By USER_MENU        = By.cssSelector("[class*='user-menu'], [class*='UserMenu'], [aria-label*='user']");
    private static final By LOGOUT_BUTTON    = By.cssSelector("[class*='logout'], button[aria-label*='logout']");
    private static final By PAGE_HEADER      = By.cssSelector("header, [class*='Header'], [class*='Navbar']");
    private static final By TRADER_EXPLORER  = By.cssSelector("[class*='trader'], [href*='explorer'], a[href='/']");

    // ── Actions ───────────────────────────────────────────────────────────

    @Step("Open user menu")
    public HomePage openUserMenu() {
        click(USER_MENU);
        return this;
    }

    @Step("Logout")
    public LoginPage logout() {
        openUserMenu();
        click(LOGOUT_BUTTON);
        return new LoginPage();
    }

    // ── Queries ───────────────────────────────────────────────────────────

    public boolean isUserAvatarVisible() {
        return isDisplayed(USER_AVATAR);
    }

    public boolean isTraderExplorerVisible() {
        return isDisplayed(TRADER_EXPLORER);
    }

    @Override
    public boolean isPageLoaded() {
        return isDisplayed(PAGE_HEADER);
    }
}
