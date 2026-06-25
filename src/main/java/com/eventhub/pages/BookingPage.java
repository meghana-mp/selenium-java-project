package com.eventhub.pages;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Represents the /bookings page (My Bookings list).
 *
 * The app has no separate booking-form page — the booking form lives inside
 * EventDetailPage. After confirming a booking the app redirects here.
 *
 * Real locators from live DOM (/bookings):
 *   Booking cards  → div[data-testid="booking-card"]
 *   Booking IDs    → span[data-testid="booking-id"]
 *   Cancel button  → #cancel-booking-btn  (within each card)
 *   Toast          → div[aria-live="polite"] div
 */
public class BookingPage extends BasePage {

    private static final By BOOKING_CARDS        = By.cssSelector("div[data-testid='booking-card']");
    private static final By BOOKING_IDS          = By.cssSelector("span[data-testid='booking-id']");
    // Fallback locators for booking ID — used by getFirstBookingId() for self-healing
    private static final By BOOKING_ID_CLASS     = By.cssSelector(".booking-id, span.booking-id");
    private static final By BOOKING_ID_XPATH     = By.xpath("//div[@data-testid='booking-card']//span[contains(@class,'id') or contains(@class,'booking')]");
    private static final By CANCEL_BTN           = By.id("cancel-booking-btn");
    private static final By CONFIRM_CANCEL_BTN  = By.xpath("//button[normalize-space()='Yes, cancel it']");
    private static final By TOAST_MSG           = By.cssSelector("div[aria-live='polite'] div");

    public BookingPage(WebDriver driver) {
        super(driver);
    }

    public boolean hasBookings() {
        return !driver.findElements(BOOKING_CARDS).isEmpty();
    }

    public int getBookingCount() {
        return driver.findElements(BOOKING_CARDS).size();
    }

    public String getFirstBookingId() {
        // Self-healing: wait for and find booking ID via data-testid, class, or xpath fallback
        return waitForVisibleWithFallback(BOOKING_IDS, BOOKING_ID_CLASS, BOOKING_ID_XPATH)
                .getText().trim();
    }

    public boolean isBookingConfirmed() {
        return hasBookings();
    }

    public void cancelFirstBooking() {
        List<WebElement> cards = driver.findElements(BOOKING_CARDS);
        if (!cards.isEmpty()) {
            WebElement cancel = cards.get(0).findElement(CANCEL_BTN);
            waitForClickable(cancel).click();
            // A confirmation modal appears — click "Yes, cancel it" to confirm
            waitForClickable(CONFIRM_CANCEL_BTN).click();
        }
    }

    public String getCancelToastText() {
        WebElement toast = waitForVisible(TOAST_MSG);
        String text = toast.getText().trim();
        if (text.isBlank()) {
            // React toast libraries sometimes render text in deeply nested nodes;
            // textContent captures the full text tree where getText() returns blank.
            Object content = ((JavascriptExecutor) driver)
                    .executeScript("return arguments[0].textContent;", toast);
            text = content != null ? content.toString().trim() : "";
        }
        return text;
    }

    public boolean isCancelToastVisible() {
        try { return waitForVisible(TOAST_MSG).isDisplayed(); }
        catch (Exception e) { return false; }
    }

    public void waitForToastToDisappear() {
        waitForInvisibility(TOAST_MSG);
    }

    public boolean isToastGone() {
        try {
            return driver.findElements(TOAST_MSG).isEmpty()
                    || !driver.findElement(TOAST_MSG).isDisplayed();
        } catch (Exception e) {
            return true;
        }
    }

    public boolean isOnBookingsPage() {
        return getCurrentUrl().contains("booking");
    }

}
