package com.eventhub.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

/**
 * All common browser actions: clicks, scrolls, typed input, tab/window handling,
 * browser info queries, and fixed pauses.
 *
 * Uses WebDriverWaitUtils for any pre-action clickability checks.
 */
public class ActionUtils {

    private final WebDriver driver;
    private final WebDriverWaitUtils waitUtils;

    public ActionUtils(WebDriver driver, WebDriverWaitUtils waitUtils) {
        this.driver    = driver;
        this.waitUtils = waitUtils;
    }

    // ── Click ───────────────────────────────────────────────────────────────

    public void jsClick(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    // ── Scroll ──────────────────────────────────────────────────────────────

    public void scrollToElement(WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", element);
    }

    public void scrollToBottom() {
        ((JavascriptExecutor) driver).executeScript(
                "window.scrollTo(0, document.body.scrollHeight);");
    }

    // ── Input ───────────────────────────────────────────────────────────────

    public void clearAndType(WebElement element, String text) {
        WebElement el = waitUtils.waitForClickable(element);
        el.click();
        el.clear();
        el.sendKeys(text);
    }

    public void clearAndType(By locator, String text) {
        clearAndType(waitUtils.waitForClickable(locator), text);
    }

    // ── Tab / window ────────────────────────────────────────────────────────

    /** Waits for a new tab to open, switches to it, and returns the original handle. */
    public String switchToNewTab() {
        String original = driver.getWindowHandle();
        waitUtils.getWait().until(d -> d.getWindowHandles().size() > 1);
        List<String> handles = new ArrayList<>(driver.getWindowHandles());
        driver.switchTo().window(handles.get(handles.size() - 1));
        return original;
    }

    public void closeCurrentTabAndSwitchTo(String handle) {
        driver.close();
        driver.switchTo().window(handle);
    }

    // ── Browser info ────────────────────────────────────────────────────────

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    // ── Timing ──────────────────────────────────────────────────────────────

    /** Fixed pause — use only when an explicit wait cannot replace it (e.g. CSS animations). */
    public void pause(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
