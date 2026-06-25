package com.eventhub.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Page object for /events/[id].
 *
 * The booking form lives DIRECTLY on this page (no separate booking/payment page).
 *
 * Real locators from live DOM:
 *   Event title   → h1
 *   Ticket count  → span#ticket-count  (display span, NOT an input)
 *   Decrement (−) → first  button[type='button'] sibling of #ticket-count
 *   Increment (+) → second button[type='button'] sibling of #ticket-count
 *   Name field    → #customerName
 *   Email field   → #customer-email
 *   Phone field   → #phone
 *   Confirm btn   → #confirm-booking
 *   Twitter share → a[href*='twitter'] or a[href*='x.com']
 *   Toast/error   → div[aria-live='polite'] div
 */
public class EventDetailPage extends BasePage {

    private static final By EVENT_TITLE       = By.tagName("h1");
    private static final By TICKET_COUNT      = By.id("ticket-count");
    private static final By CUSTOMER_NAME     = By.id("customerName");
    private static final By CUSTOMER_EMAIL    = By.id("customer-email");
    private static final By PHONE             = By.id("phone");
    private static final By CONFIRM_BOOKING         = By.id("confirm-booking");
    // Static fallbacks for MCP healing — tried before calling Claude API
    private static final By CONFIRM_BOOKING_SUBMIT  = By.cssSelector("button[type='submit'][id*='confirm'], button[id*='booking']");
    private static final By CONFIRM_BOOKING_XPATH   = By.xpath("//button[contains(normalize-space(),'Confirm') or contains(normalize-space(),'Book Now')]");
    private static final By SHARE_TWITTER_BTN = By.cssSelector(
            "a[href*='twitter.com'], a[href*='x.com'], a[data-testid='share-twitter']");
    private static final By PRICE_TEXT        = By.cssSelector(
            "[data-testid='ticket-price'], .ticket-price, .price, span.price");
    private static final By TOAST_MSG         = By.cssSelector("div[aria-live='polite'] div");
    private static final By DATE_TIME_EL      = By.cssSelector(
            "time, [data-testid='event-date'], .event-date, .event-time, p.date");
    // +/- buttons sit inside the same parent as span#ticket-count
    private static final By QTY_BUTTONS       = By.xpath(
            "//span[@id='ticket-count']/..//button[@type='button']");

    public EventDetailPage(WebDriver driver) {
        super(driver);
    }

    public String getEventTitle() {
        return waitForVisible(EVENT_TITLE).getText().trim();
    }

    public String getEventTime() {
        try { return waitForVisible(DATE_TIME_EL).getText().trim(); }
        catch (Exception e) { return ""; }
    }

    public int getDisplayedQuantity() {
        return Integer.parseInt(waitForVisible(TICKET_COUNT).getText().trim());
    }

    public void clickPlusButton() {
        List<WebElement> btns = driver.findElements(QTY_BUTTONS);
        if (btns.size() >= 2) waitForClickable(btns.get(1)).click();
    }

    public void clickMinusButton() {
        List<WebElement> btns = driver.findElements(QTY_BUTTONS);
        // Only click if enabled — the app disables the − button at qty=1
        if (!btns.isEmpty() && btns.get(0).isEnabled()) {
            btns.get(0).click();
        }
    }

    public boolean isMinusButtonDisabled() {
        List<WebElement> btns = driver.findElements(QTY_BUTTONS);
        if (btns.isEmpty()) return true;
        WebElement minus = btns.get(0);
        return !minus.isEnabled() || minus.getAttribute("disabled") != null;
    }

    public void setQuantity(int target) {
        int current = getDisplayedQuantity();
        while (current < target) { clickPlusButton(); current++; }
        while (current > Math.max(target, 1)) { clickMinusButton(); current--; }
    }

    public String getQuantityValue() { return String.valueOf(getDisplayedQuantity()); }
    public String getQuantityMin()   { return "1"; }
    public String getQuantityMax()   { return ""; }

    public void fillBookingForm(String name, String email, String phone) {
        clearAndType(CUSTOMER_NAME, name);
        clearAndType(CUSTOMER_EMAIL, email);
        clearAndType(PHONE, phone);
    }

    public BookingPage confirmBooking(String name, String email, String phone) {
        fillBookingForm(name, email, phone);
        clickConfirmBtn();
        return new BookingPage(driver);
    }

    public BookingPage confirmBooking() {
        clickConfirmBtn();
        return new BookingPage(driver);
    }

    public void clickConfirmWithoutFilling() {
        clickConfirmBtn();
    }

    private void clickConfirmBtn() {
        // MCP healing: if #confirm-booking id is renamed, Claude reads the live DOM
        // and returns the new locator. Static fallbacks tried first at no API cost.
        WebElement btn = waitForVisibleWithMcpHealing(
                "confirm booking submit button on event detail page",
                CONFIRM_BOOKING,
                CONFIRM_BOOKING_SUBMIT,
                CONFIRM_BOOKING_XPATH);
        scrollToElement(btn);
        try {
            btn.click();
        } catch (Exception e) {
            jsClick(btn);
        }
        // App stays on /events/{id} after booking — no redirect occurs.
        // Brief pause lets the API call complete before the caller reads /bookings.
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public boolean hasToastError() {
        try { return waitForVisible(TOAST_MSG).isDisplayed(); }
        catch (Exception e) { return false; }
    }

    public String getToastText() {
        return waitForVisible(TOAST_MSG).getText().trim();
    }

    public boolean isSoldOut() {
        try {
            WebElement btn = driver.findElement(CONFIRM_BOOKING);
            return btn.getText().toLowerCase().contains("sold") || !btn.isEnabled();
        } catch (Exception e) { return false; }
    }

    public boolean isBookNowDisabled() {
        try {
            WebElement btn = driver.findElement(CONFIRM_BOOKING);
            return !btn.isEnabled() || btn.getAttribute("disabled") != null;
        } catch (Exception e) { return true; }
    }

    public double getTicketPrice() {
        try {
            String raw = waitForVisible(PRICE_TEXT).getText().trim();
            return Double.parseDouble(raw.replaceAll("[^0-9.]", ""));
        } catch (Exception e) { return 0.0; }
    }

    public String clickShareTwitterAndGetOriginalHandle() {
        waitForClickable(SHARE_TWITTER_BTN).click();
        return switchToNewTab();
    }

    public void clickBookmark() {
        waitForClickable(By.cssSelector(
                ".bookmark-btn, button[aria-label*='bookmark'], button[aria-label*='favorite'], [data-testid='bookmark']"))
                .click();
    }

    public String getEventId() {
        String url = getCurrentUrl();
        String[] parts = url.split("/");
        return parts[parts.length - 1];
    }

    public BookingPage clickBookNow() {
        return confirmBooking();
    }
}
