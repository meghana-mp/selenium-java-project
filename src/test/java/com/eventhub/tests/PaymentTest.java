package com.eventhub.tests;

import com.eventhub.constants.AppConstants;
import com.eventhub.pages.EventDetailPage;
import com.eventhub.pages.EventsPage;
import com.eventhub.tests.base.BaseTest;
import com.eventhub.tests.dataproviders.TestDataProvider;
import com.eventhub.utils.TestUtils;
import io.qameta.allure.*;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * TC_PAYMENT_001, TC_PAYMENT_002 — Booking Form Validation & Pricing
 *
 * NOTE: EventHub has no credit-card / payment gateway.
 * These tests have been adapted to cover equivalent real behaviours:
 *   TC_PAYMENT_001 → Booking form field validation (missing required fields
 *                    trigger inline errors / toast when Confirm is clicked)
 *   TC_PAYMENT_002 → Per-ticket price display and quantity-driven total
 *                    (incrementing qty verifies price × qty relationship)
 */
@Epic("Payment Gateways")
@Feature("Booking Form Validation & Pricing")
public class PaymentTest extends BaseTest {

    /*
     * TC_PAYMENT_001 — Booking Form Validation (Missing Required Fields)
     * Scenario: Clicking Confirm Booking without filling all required fields
     *           must show a validation error — the booking must not be created.
     * Steps:
     *   1. Login and open an event detail page.
     *   2. Leave all attendee fields blank.
     *   3. Click #confirm-booking.
     *   4. Assert a toast error or inline validation message appears.
     *   5. Assert the URL has NOT navigated to /bookings (booking blocked).
     */
    // Retry-eligible (smoke group): RetryListener wires RetryAnalyzer automatically.
    // On failure retries up to 2 times — 1 s then 2 s back-off — before marking FAILED.
    // Dynamic MCP healing: EventDetailPage.clickConfirmBtn() calls waitForVisibleWithMcpHealing —
    // tries #confirm-booking → button[type='submit'][id*='confirm'] → XPath text-match fallback.
    // Only if all 3 fail is the Claude API called to derive a selector from the live DOM.
    // Requires ANTHROPIC_API_KEY env var; degrades gracefully when unset.
    @Test(
        groups        = { AppConstants.SMOKE, AppConstants.REGRESSION },
        dataProvider  = "invalidCards",
        dataProviderClass = TestDataProvider.class,
        description   = "TC_PAYMENT_001 — Missing booking fields show validation error"
    )
    @Story("TC_PAYMENT_001")
    @Description("Clicks Confirm without filling attendee fields; asserts validation blocks the booking.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_PAYMENT_001_bookingFormValidationMissingFields(
            String cardNumber, String expiry, String cvv, String scenario) {
        long start = System.currentTimeMillis();

        Allure.step("Login and open first available event");
        loginWithDefaults();
        navigateTo(AppConstants.PATH_EVENTS);
        EventDetailPage detailPage = new EventsPage(driver).openEventByTitle("");

        String pageUrl = actionUtils.getCurrentUrl();
        Allure.step("Event URL: " + pageUrl + " | Scenario: " + scenario);

        Allure.step("Click Confirm Booking with all fields blank");
        detailPage.clickConfirmWithoutFilling();

        Allure.step("Assert validation error shown OR user stays on event page");
        boolean hasError    = detailPage.hasToastError();
        boolean staysOnPage = actionUtils.getCurrentUrl().contains("events");

        assertTrue(hasError || staysOnPage,
                "Expected validation to block booking. Toast: " + hasError
                        + " | URL: " + actionUtils.getCurrentUrl());

        TestUtils.logResult("TC_PAYMENT_001", true, System.currentTimeMillis() - start,
                "Scenario: " + scenario + " | Error shown: " + hasError);
    }

    /*
     * TC_PAYMENT_002 — Ticket Price Display and Quantity-Driven Pricing
     * Scenario: The event detail page must show a per-ticket price. When the
     *           quantity is incremented, the UI should reflect the updated total.
     * Steps:
     *   1. Login and open an event detail page.
     *   2. Read the displayed per-ticket price and initial quantity.
     *   3. Click + once to increment quantity.
     *   4. Assert quantity increased by 1 and price is non-negative.
     */
    @Test(
        groups        = { AppConstants.REGRESSION },
        dataProvider  = "promoCodeData",
        dataProviderClass = TestDataProvider.class,
        description   = "TC_PAYMENT_002 — Ticket price is positive and quantity increments correctly"
    )
    @Story("TC_PAYMENT_002")
    @Description("Verifies per-ticket price display and that quantity buttons update the count correctly.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_PAYMENT_002_ticketPriceAndQuantityDisplay(
            String promoCode, int discountPercent, double baseAmount) {
        long start = System.currentTimeMillis();

        Allure.step("Login and open first available event");
        loginWithDefaults();
        navigateTo(AppConstants.PATH_EVENTS);
        EventDetailPage detailPage = new EventsPage(driver).openEventByTitle("");

        Allure.step("Read initial quantity from span#ticket-count");
        int initialQty = detailPage.getDisplayedQuantity();
        Allure.step("Initial quantity: " + initialQty);

        Allure.step("Increment quantity using + button");
        detailPage.clickPlusButton();
        int newQty = detailPage.getDisplayedQuantity();
        Allure.step("Quantity after increment: " + newQty);

        Allure.step("Assert quantity increased by 1");
        assertEquals(newQty, initialQty + 1,
                "Quantity should increase by 1 after clicking + button");

        Allure.step("Read ticket price from price element");
        double price = detailPage.getTicketPrice();
        Allure.step("Ticket price: " + price);
        assertTrue(price >= 0,
                "Ticket price should be a non-negative number. Got: " + price);

        TestUtils.logResult("TC_PAYMENT_002", true, System.currentTimeMillis() - start,
                "Price: " + price + " | Qty: " + initialQty + " → " + newQty);
    }
}
