package com.eventhub.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page object for /login.
 *
 * Real locators captured from live DOM (eventhub.rahulshettyacademy.com):
 *   Email  → id="email"
 *   Pass   → id="password"
 *   Submit → id="login-btn"
 *   Error  → div[aria-live="polite"] div (toast container)
 */
public class LoginPage extends BasePage {

    private static final By EMAIL_INPUT    = By.id("email");
    private static final By PASSWORD_INPUT = By.id("password");
    private static final By LOGIN_BTN      = By.id("login-btn");
    private static final By TOAST_MSG      = By.cssSelector("div[aria-live='polite'] div");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public void enterEmail(String email) {
        clearAndType(EMAIL_INPUT, email);
    }

    public void enterPassword(String password) {
        clearAndType(PASSWORD_INPUT, password);
    }

    public void clickLogin() {
        waitForClickable(LOGIN_BTN).click();
    }

    /** Types credentials and clicks Login; waits for SPA navigation before returning. */
    public DashboardPage login(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        clickLogin();
        // Only check the URL — never call findElements inside wait.until lambdas.
        // findElements respects implicit wait (10s), which blocks every poll iteration
        // and exhausts the 15s outer timeout before the URL check can re-run.
        wait.until(d -> !d.getCurrentUrl().contains("/login"));
        return new DashboardPage(driver);
    }

    /** Quick non-blocking check — uses findElements so it never blocks on absence. */
    public boolean isLoginErrorDisplayed() {
        try {
            return !driver.findElements(TOAST_MSG).isEmpty()
                    && driver.findElements(TOAST_MSG).get(0).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getLoginError() {
        return waitForVisible(TOAST_MSG).getText().trim();
    }

    public boolean isOnLoginPage() {
        return getCurrentUrl().contains("login");
    }
}
