package com.eventhub.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Explicit-wait utilities only.
 * Every method blocks until the expected condition is met or the timeout expires.
 *
 * Self-healing additions:
 *   waitForClickableWithStaleRecovery — re-finds stale elements automatically
 *   waitForVisibleWithFallback        — tries alternative locators when primary fails
 *   waitForVisibleWithMcpHealing      — last resort: asks Claude API to derive a locator
 *                                       from the live DOM when all static locators fail
 */
public class WebDriverWaitUtils {

    private static final Logger log = LoggerFactory.getLogger(WebDriverWaitUtils.class);
    private static final int    STALE_RETRY_LIMIT = 3;

    private final WebDriver     driver;
    private final WebDriverWait wait;

    public WebDriverWaitUtils(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait   = wait;
    }

    public WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    public WebElement waitForClickable(WebElement element) {
        return wait.until(ExpectedConditions.elementToBeClickable(element));
    }

    public boolean waitForInvisibility(By locator) {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public boolean waitForText(By locator, String text) {
        return wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    public List<WebElement> waitForAllVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    /**
     * Re-finds the element up to STALE_RETRY_LIMIT times when a
     * StaleElementReferenceException is thrown, then delegates to waitForClickable.
     */
    public WebElement waitForClickableWithStaleRecovery(By locator) {
        for (int attempt = 1; attempt <= STALE_RETRY_LIMIT; attempt++) {
            try {
                return waitForClickable(locator);
            } catch (StaleElementReferenceException e) {
                if (attempt == STALE_RETRY_LIMIT) throw e;
                log.warn("StaleElementReferenceException on [{}] — retry {}/{}", locator, attempt, STALE_RETRY_LIMIT);
            }
        }
        return waitForClickable(locator);
    }

    /**
     * Tries the primary locator first; if it times out, attempts each fallback in order.
     * Logs a warning when a fallback is used so flaky locators are visible in test logs.
     */
    public WebElement waitForVisibleWithFallback(By primary, By... fallbacks) {
        try {
            return waitForVisible(primary);
        } catch (Exception primaryEx) {
            for (By fallback : fallbacks) {
                try {
                    WebElement el = waitForVisible(fallback);
                    log.warn("Self-heal: primary locator [{}] failed — used fallback [{}]", primary, fallback);
                    return el;
                } catch (Exception ignored) {
                }
            }
            throw primaryEx;
        }
    }

    /**
     * Three-tier self-healing strategy:
     *   1. Try primary locator (fast path — no API cost).
     *   2. Try each static fallback in order (free, pre-written alternatives).
     *   3. If all fail, call Claude API via McpHealingUtils: send the live page body
     *      and element description; Claude returns a CSS/XPath that matches the current DOM.
     *
     * The MCP step is skipped gracefully when ANTHROPIC_API_KEY is not set, so tests
     * continue to run in environments that have no API access.
     */
    public WebElement waitForVisibleWithMcpHealing(String elementDescription,
                                                    By primary, By... staticFallbacks) {
        try {
            return waitForVisibleWithFallback(primary, staticFallbacks);
        } catch (Exception allStaticFailed) {
            log.warn("[MCP Healing] All static locators exhausted for '{}' — asking Claude",
                    elementDescription);
            By healedLocator = McpHealingUtils.healLocator(driver, elementDescription);
            if (healedLocator != null) {
                try {
                    WebElement el = waitForVisible(healedLocator);
                    log.warn("[MCP Healing] SUCCESS for '{}' using healed locator: {}",
                            elementDescription, healedLocator);
                    return el;
                } catch (Exception healFailed) {
                    log.error("[MCP Healing] Healed locator also failed for '{}': {}",
                            elementDescription, healedLocator);
                }
            }
            throw allStaticFailed;
        }
    }

    public WebDriverWait getWait() {
        return wait;
    }
}
