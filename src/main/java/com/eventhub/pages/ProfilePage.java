package com.eventhub.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Models the Admin Create-Event page (/admin/events).
 *
 * Real locators from live DOM:
 *   Form           → data-testid="admin-event-form"
 *   Title          → data-testid="event-title-input"   id="event-title-input"
 *   City           → id="city"
 *   Venue          → id="venue"
 *   Date-time      → id="event-date-&-time"
 *   Price          → id="price-($)"
 *   Total seats    → id="total-seats"
 *   Image URL      → input[type='url']                 id="image-url-(optional)"
 *   Submit         → data-testid="add-event-btn"
 *   Event rows     → data-testid="event-table-row"     (async-loaded from API)
 *   Toast          → div[aria-live='polite'] div
 */
public class ProfilePage extends BasePage {

    private static final By TITLE_INPUT     = By.cssSelector("[data-testid='event-title-input']");
    private static final By IMAGE_URL_INPUT = By.cssSelector("input[type='url']");
    private static final By ADD_EVENT_BTN   = By.cssSelector("[data-testid='add-event-btn']");
    private static final By EVENT_ROWS      = By.cssSelector("[data-testid='event-table-row']");
    private static final By TOAST           = By.cssSelector("div[aria-live='polite'] div");

    public ProfilePage(WebDriver driver) {
        super(driver);
    }

    /** Returns true when the async-loaded event table has at least one row. */
    public boolean areEventRowsLoaded() {
        try {
            waitForAllVisible(EVENT_ROWS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int getEventRowCount() {
        return driver.findElements(EVENT_ROWS).size();
    }

    public void enterImageUrl(String url) {
        clearAndType(IMAGE_URL_INPUT, url);
    }

    public String getImageUrlValue() {
        try { return driver.findElement(IMAGE_URL_INPUT).getAttribute("value"); }
        catch (Exception e) { return ""; }
    }

    public boolean isFormPresent() {
        return !driver.findElements(TITLE_INPUT).isEmpty();
    }

    public String getToastText() {
        return waitForVisible(TOAST).getText().trim();
    }

    public boolean isToastVisible() {
        try { return waitForVisible(TOAST).isDisplayed(); }
        catch (Exception e) { return false; }
    }

    public void waitForToastToDisappear() { waitForInvisibility(TOAST); }

    public boolean isToastGone() {
        try {
            return driver.findElements(TOAST).isEmpty()
                    || !driver.findElement(TOAST).isDisplayed();
        } catch (Exception e) { return true; }
    }

    public void save() {
        try { waitForClickable(ADD_EVENT_BTN).click(); }
        catch (Exception ignored) {}
    }
}
