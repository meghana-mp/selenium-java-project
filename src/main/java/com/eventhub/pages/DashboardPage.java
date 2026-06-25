package com.eventhub.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Set;

/**
 * Page object for the home / dashboard page (/).
 *
 * Real locators captured from live DOM:
 *   User email   → id="user-email-display"
 *   Nav Events   → id="nav-events"
 *   Nav Bookings → id="nav-bookings"
 *   Logout       → id="logout-btn"
 *   Event cards  → article[data-testid="event-card"]
 *   Hamburger    → button[aria-label="Toggle menu"]
 */
public class DashboardPage extends BasePage {

    private static final By USER_EMAIL            = By.id("user-email-display");
    // Fallback locators used by getDisplayedUserEmail() for self-healing
    private static final By USER_EMAIL_TESTID     = By.cssSelector("[data-testid='user-email']");
    private static final By USER_EMAIL_NAV_GENERIC = By.cssSelector("nav span[class*='email'], header span[class*='email'], .navbar-email");
    private static final By NAV_EVENTS    = By.id("nav-events");
    private static final By NAV_BOOKINGS  = By.id("nav-bookings");
    private static final By HAMBURGER_BTN = By.cssSelector("button[aria-label='Toggle menu']");
    private static final By EVENT_CARDS   = By.cssSelector("article[data-testid='event-card']");

    public DashboardPage(WebDriver driver) {
        super(driver);
    }

    public String getDisplayedUserEmail() {
        // Self-healing: try the known id first, then data-testid, then generic nav span
        return waitForVisibleWithFallback(USER_EMAIL, USER_EMAIL_TESTID, USER_EMAIL_NAV_GENERIC)
                .getText().trim();
    }

    public boolean isLoggedIn() {
        try { return waitForVisible(USER_EMAIL).isDisplayed(); }
        catch (Exception e) { return false; }
    }

    public boolean isOnDashboard() {
        try {
            waitForVisible(USER_EMAIL);
            String url = getCurrentUrl();
            return !url.contains("login") && !url.contains("register");
        } catch (Exception e) {
            return false;
        }
    }

    public void clearSession() {
        driver.manage().deleteAllCookies();
        ((JavascriptExecutor) driver).executeScript(
                "window.localStorage.clear(); window.sessionStorage.clear();");
    }

    public void navigateToMyBookings() {
        waitForClickable(NAV_BOOKINGS).click();
    }

    public boolean isRedirectedToLogin() {
        return getCurrentUrl().contains("login");
    }

    /** Returns true when the app has stored an auth token in localStorage/sessionStorage. */
    public boolean hasActiveSession() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        // Check localStorage for any key containing "token" or "auth"
        String script =
            "var keys = Object.keys(window.localStorage);" +
            "return keys.some(function(k){ return k.toLowerCase().includes('token') || k.toLowerCase().includes('auth'); });" ;
        Object result = js.executeScript(script);
        if (Boolean.TRUE.equals(result)) return true;
        // Fallback: localStorage is non-empty (app stores session data there)
        Long count = (Long) js.executeScript("return Object.keys(window.localStorage).length;");
        return count != null && count > 0;
    }

    /** @deprecated App uses localStorage, not cookies. Use {@link #hasActiveSession()} instead. */
    @Deprecated
    public Set<org.openqa.selenium.Cookie> getSessionCookies() {
        return driver.manage().getCookies();
    }

    public int getEventCardCount() {
        return driver.findElements(EVENT_CARDS).size();
    }

    public EventsPage clickNavEvents() {
        waitForClickable(NAV_EVENTS).click();
        return new EventsPage(driver);
    }

    public String getDisplayedUsername() {
        return getDisplayedUserEmail();
    }

    public boolean isSessionExpiredMessageVisible() {
        try {
            By msg = By.cssSelector("div[aria-live='polite'] div, .session-expired, .auth-error");
            return driver.findElements(msg).stream().anyMatch(WebElement::isDisplayed);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isMobileHamburgerVisible() {
        try { return driver.findElement(HAMBURGER_BTN).isDisplayed(); }
        catch (Exception e) { return false; }
    }

    public void clickHamburger() {
        waitForClickable(HAMBURGER_BTN).click();
    }
}
