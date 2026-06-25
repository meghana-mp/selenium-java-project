package com.eventhub.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page object for /register.
 *
 * Real locators captured from live DOM (eventhub.rahulshettyacademy.com/register):
 *   Email        → id="register-email"
 *   Password     → id="register-password"
 *   Confirm pass → input[placeholder="Repeat your password"]
 *   Submit       → id="register-btn"
 *   Toast/errors → div[aria-live="polite"] div
 *   Field errors → span[data-testid="field-error"] or .error-message
 */
public class RegisterPage extends BasePage {

    private static final By EMAIL_INPUT    = By.id("register-email");
    private static final By PASSWORD_INPUT = By.id("register-password");
    private static final By CONFIRM_PASS   = By.cssSelector("input[placeholder='Repeat your password']");
    private static final By REGISTER_BTN   = By.cssSelector("[data-testid='register-btn']");
    private static final By TOAST_MSG      = By.cssSelector("div[aria-live='polite'] div");
    // Tailwind uses p.text-red-600 for inline validation messages on this form
    private static final By INLINE_ERRORS  = By.cssSelector(
            "p[class*='text-red'], span[data-testid='field-error'], .error-message, .text-danger");

    public RegisterPage(WebDriver driver) {
        super(driver);
    }

    public void enterEmail(String email) {
        clearAndType(EMAIL_INPUT, email);
    }

    public void enterPassword(String password) {
        clearAndType(PASSWORD_INPUT, password);
    }

    public void enterConfirmPassword(String confirm) {
        clearAndType(CONFIRM_PASS, confirm);
    }

    public void clickRegister() {
        waitForClickable(REGISTER_BTN).click();
    }

    public void fillForm(String ignored1, String ignored2, String email, String password, String phone) {
        enterEmail(email);
        enterPassword(password);
        try { enterConfirmPassword(password); } catch (Exception ignored) {}
    }

    public void fillAndSubmit(String email, String password, String confirm) {
        enterEmail(email);
        enterPassword(password);
        enterConfirmPassword(confirm);
        clickRegister();
    }

    public String getFirstInlineError() {
        try { return waitForVisible(INLINE_ERRORS).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public String getToastMessage() {
        return waitForVisible(TOAST_MSG).getText().trim();
    }

    public boolean hasValidationErrors() {
        return !driver.findElements(INLINE_ERRORS).isEmpty();
    }

    public boolean isToastVisible() {
        try { return waitForVisible(TOAST_MSG).isDisplayed(); }
        catch (Exception e) { return false; }
    }

    public boolean hasEmailError()    { return hasValidationErrors(); }
    public boolean hasPasswordError() { return hasValidationErrors(); }

    public boolean isOnRegisterPage() {
        return getCurrentUrl().contains("register");
    }
}
