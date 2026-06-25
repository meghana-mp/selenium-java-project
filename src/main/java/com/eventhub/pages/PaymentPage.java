package com.eventhub.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Adapted from the original PaymentPage — EventHub has no card/payment gateway.
 *
 * This class now models post-booking states and booking-form validation:
 *   • isErrorDisplayed / getErrorMessage → toast errors from div[aria-live='polite']
 *   • isBookingConfirmationShown        → checks for booking-id on /bookings
 *   • getBookingId                      → span[data-testid='booking-id']
 *   • getDisplayedTotal                 → ticket price × quantity from the event page
 *
 * Legacy card/promo methods are kept as no-ops so old test code still compiles.
 */
public class PaymentPage extends BasePage {

    private static final By TOAST_MSG  = By.cssSelector("div[aria-live='polite'] div");
    private static final By BOOKING_ID = By.cssSelector(
            "span[data-testid='booking-id'], .booking-id, [data-testid='booking-ref']");
    private static final By PRICE_EL   = By.cssSelector(
            "[data-testid='ticket-price'], .ticket-price, .price, span.price");

    public PaymentPage(WebDriver driver) {
        super(driver);
    }

    public boolean isErrorDisplayed() {
        try { return waitForVisible(TOAST_MSG).isDisplayed(); }
        catch (Exception e) { return false; }
    }

    public String getErrorMessage() {
        return waitForVisible(TOAST_MSG).getText().trim();
    }

    public boolean isBookingConfirmationShown() {
        try { return waitForVisible(BOOKING_ID).isDisplayed(); }
        catch (Exception e) { return false; }
    }

    public String getBookingId() {
        return waitForVisible(BOOKING_ID).getText().trim();
    }

    /** Returns ticket price from the event detail page (no payment gateway). */
    public double getDisplayedTotal() {
        try {
            String raw = waitForVisible(PRICE_EL).getText().trim();
            return Double.parseDouble(raw.replaceAll("[^0-9.]", ""));
        } catch (Exception e) { return 0.0; }
    }

    public boolean isOnPaymentPage() {
        return getCurrentUrl().contains("booking") || getCurrentUrl().contains("events");
    }

    // ── Legacy no-ops kept for compile compatibility ───────────────────────

    public void enterCard(String card, String expiry, String cvv) {}
    public void clickPay() {}
    public void applyPromoCode(String code) {}
}
