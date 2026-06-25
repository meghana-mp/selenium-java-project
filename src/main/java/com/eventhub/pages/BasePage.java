package com.eventhub.pages;

import com.eventhub.config.ConfigManager;
import com.eventhub.constants.AppConstants;
import com.eventhub.utils.ActionUtils;
import com.eventhub.utils.WebDriverWaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Abstract base for all page objects.
 *
 * Wires up two utilities and exposes protected one-liner delegates:
 *   waitUtils  — explicit waits only  (WebDriverWaitUtils)
 *   actionUtils — all other actions   (ActionUtils)
 *
 * The {@code wait} field stays accessible for pages that need custom
 * {@code wait.until(...)} lambdas (e.g. LoginPage, EventsPage).
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;
    protected final WebDriverWaitUtils waitUtils;
    protected final ActionUtils actionUtils;

    protected BasePage(WebDriver driver) {
        this.driver     = driver;
        int timeout     = ConfigManager.getInstance().getIntProperty(
                "explicit.wait.seconds", AppConstants.DEFAULT_EXPLICIT_WAIT);
        this.wait       = new WebDriverWait(driver, Duration.ofSeconds(timeout));
        this.waitUtils  = new WebDriverWaitUtils(driver, wait);
        this.actionUtils = new ActionUtils(driver, waitUtils);
    }

    // ── Wait delegates ──────────────────────────────────────────────────────

    protected WebElement waitForVisible(By locator)              { return waitUtils.waitForVisible(locator); }
    protected WebElement waitForClickable(By locator)            { return waitUtils.waitForClickable(locator); }
    protected WebElement waitForClickable(WebElement element)    { return waitUtils.waitForClickable(element); }
    protected boolean    waitForInvisibility(By locator)         { return waitUtils.waitForInvisibility(locator); }
    protected boolean    waitForText(By locator, String text)    { return waitUtils.waitForText(locator, text); }
    protected List<WebElement> waitForAllVisible(By locator)     { return waitUtils.waitForAllVisible(locator); }

    /** Self-healing: tries primary locator then each fallback in order. */
    protected WebElement waitForVisibleWithFallback(By primary, By... fallbacks) {
        return waitUtils.waitForVisibleWithFallback(primary, fallbacks);
    }

    /** Self-healing: re-finds stale elements automatically before clicking. */
    protected WebElement waitForClickableWithStaleRecovery(By locator) {
        return waitUtils.waitForClickableWithStaleRecovery(locator);
    }

    /** Dynamic MCP healing: static fallbacks first, then Claude API as last resort. */
    protected WebElement waitForVisibleWithMcpHealing(String description, By primary, By... fallbacks) {
        return waitUtils.waitForVisibleWithMcpHealing(description, primary, fallbacks);
    }

    // ── Action delegates ────────────────────────────────────────────────────

    protected void jsClick(WebElement element)                   { actionUtils.jsClick(element); }
    protected void scrollToElement(WebElement element)           { actionUtils.scrollToElement(element); }
    protected void scrollToBottom()                              { actionUtils.scrollToBottom(); }
    protected void clearAndType(WebElement element, String text) { actionUtils.clearAndType(element, text); }
    protected void clearAndType(By locator, String text)         { actionUtils.clearAndType(locator, text); }

    // ── Tab / window delegates ──────────────────────────────────────────────

    protected String switchToNewTab()                            { return actionUtils.switchToNewTab(); }
    protected void closeCurrentTabAndSwitchTo(String handle)     { actionUtils.closeCurrentTabAndSwitchTo(handle); }

    // ── Browser info delegates ──────────────────────────────────────────────

    public String getCurrentUrl()  { return actionUtils.getCurrentUrl(); }
    public String getPageTitle()   { return actionUtils.getPageTitle(); }
}
