package com.framework.ui.pages;

import com.framework.ui.base.BasePage;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

/**
 * Page Object for the Dashboard / home page after successful login.
 */
public class DashboardPage extends BasePage {

    private final By welcomeMessage = By.cssSelector(".welcome-message");
    private final By userAvatar     = By.cssSelector(".user-avatar");
    private final By logoutButton   = By.id("logout-btn");
    private final By pageHeader     = By.cssSelector("h1.dashboard-title");

    // ── Actions ───────────────────────────────────────────────────────────

    @Step("Click Logout")
    public LoginPage logout() {
        click(logoutButton);
        return new LoginPage();
    }

    // ── Queries ───────────────────────────────────────────────────────────

    public String getWelcomeMessage() {
        return getText(welcomeMessage);
    }

    public boolean isUserAvatarDisplayed() {
        return isDisplayed(userAvatar);
    }

    @Override
    public boolean isPageLoaded() {
        return isDisplayed(pageHeader) || isDisplayed(welcomeMessage);
    }
}
