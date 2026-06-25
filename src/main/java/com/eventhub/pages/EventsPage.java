package com.eventhub.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Page object for /events.
 *
 * Real locators captured from live DOM:
 *   Search input  → input[placeholder="Search events, venues…"]
 *   Category      → select (first select on page)
 *   City/Location → select (second select on page)
 *   Event cards   → article[data-testid="event-card"]
 *   Card titles   → article h3
 *   No-results    → p.text-center or .no-events
 */
public class EventsPage extends BasePage {

    private static final By SEARCH_INPUT         = By.cssSelector("input[placeholder='Search events, venues…']");
    // Static fallbacks for MCP healing — tried before calling Claude API
    private static final By SEARCH_INPUT_TYPE    = By.cssSelector("input[type='search']");
    private static final By SEARCH_INPUT_GENERIC = By.cssSelector("input[name='search'], input[name='q'], input[placeholder*='Search']");
    private static final By CATEGORY_DROPDOWN = By.cssSelector("select:first-of-type");
    private static final By LOCATION_DROPDOWN = By.cssSelector("select + select");
    private static final By EVENT_CARDS       = By.cssSelector("article[data-testid='event-card']");
    private static final By EVENT_TITLES      = By.cssSelector(
            "article[data-testid='event-card'] h3, article[data-testid='event-card'] a h3");
    private static final By NO_RESULTS_MSG    = By.cssSelector(
            ".no-events, .empty-state, p.text-center, [data-testid='no-results']");
    private static final By SKELETON          = By.cssSelector(
            ".skeleton, .loading-placeholder, .animate-pulse");

    public EventsPage(WebDriver driver) {
        super(driver);
    }

    public void search(String keyword) {
        // MCP healing: if placeholder text changes or input is restructured, Claude finds it.
        // Static fallbacks tried first (no API cost); Claude API called only when all fail.
        WebElement input = waitForVisibleWithMcpHealing(
                "search text input for finding events by keyword",
                SEARCH_INPUT,
                SEARCH_INPUT_TYPE,
                SEARCH_INPUT_GENERIC);
        actionUtils.clearAndType(input, keyword);
        waitForSearchResults();
    }

    /** Waits for the search debounce + any loading spinner to settle. */
    public void waitForSearchResults() {
        try {
            waitForInvisibility(SKELETON);
        } catch (Exception ignored) {}
        try { Thread.sleep(800); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public void filterByCategory(String category) {
        new Select(driver.findElement(CATEGORY_DROPDOWN)).selectByVisibleText(category);
        waitForSearchResults();
    }

    public void filterByLocation(String location) {
        new Select(driver.findElement(LOCATION_DROPDOWN)).selectByVisibleText(location);
        waitForSearchResults();
    }

    public int getResultCount() {
        try { return driver.findElements(EVENT_CARDS).size(); }
        catch (Exception e) { return 0; }
    }

    public List<String> getAllEventTitles() {
        return waitForAllVisible(EVENT_TITLES)
                .stream().map(WebElement::getText).map(String::trim)
                .collect(Collectors.toList());
    }

    public boolean isNoResultsShown() {
        try { return waitForVisible(NO_RESULTS_MSG).isDisplayed(); }
        catch (Exception e) { return false; }
    }

    public void scrollPageToBottom() {
        scrollToBottom();
    }

    public boolean areEventCardsAbsent() {
        return driver.findElements(EVENT_CARDS).isEmpty();
    }

    public void waitForSkeletonToDisappear() {
        waitForInvisibility(SKELETON);
    }

    public EventDetailPage openEventByTitle(String title) {
        List<WebElement> cards = waitForAllVisible(EVENT_CARDS);
        for (WebElement card : cards) {
            if (title == null || title.isBlank() || card.getText().contains(title)) {
                WebElement link = card.findElement(By.cssSelector("a[data-testid='book-now-btn']"));
                // aria-disabled="true" means sold-out — waitForClickable still passes because
                // isEnabled() returns true for <a> tags regardless of aria-disabled, so we
                // check the attribute explicitly and skip to the next card.
                if ("true".equalsIgnoreCase(link.getAttribute("aria-disabled"))) continue;
                String beforeUrl = driver.getCurrentUrl();
                // jsClick bypasses pointer-events:none and overlay interception that
                // causes ElementClickIntercepted on cards near the bottom of the viewport.
                jsClick(link);
                // Wait for URL to change so the events-list h1 is gone before
                // any caller tries to read the event-detail h1 (avoids stale element)
                try { wait.until(d -> !d.getCurrentUrl().equals(beforeUrl)); }
                catch (Exception ignored) {}
                return new EventDetailPage(driver);
            }
        }
        throw new RuntimeException("No bookable event found"
                + (title != null && !title.isBlank() ? " with title: " + title : " — all visible events may be sold out"));
    }
}
