package com.eventhub.tests;

import com.eventhub.constants.AppConstants;
import com.eventhub.pages.BookingPage;
import com.eventhub.pages.EventDetailPage;
import com.eventhub.pages.EventsPage;
import com.eventhub.tests.base.BaseTest;
import com.eventhub.tests.dataproviders.TestDataProvider;
import com.eventhub.utils.TestUtils;
import io.qameta.allure.*;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * TC_BOOKING_001, TC_BOOKING_002 — Ticket Booking
 *
 * Covers:
 *   - Full E2E booking flow: Events list → Event Detail → fill form → confirm
 *   - EventDetailPage: span#ticket-count, #customerName, #customer-email,
 *     #phone, #confirm-booking (booking form is ON the event detail page)
 *   - BookingPage (repurposed /bookings): div[data-testid='booking-card'],
 *     span[data-testid='booking-id']
 *   - Sold-out state: confirm button disabled / text changes to "Sold Out"
 */
@Epic("E-Commerce / Booking")
@Feature("Ticket Booking")
public class BookingTest extends BaseTest {

    /*
     * TC_BOOKING_001 — Standard Ticket Booking (Full E2E)
     * Scenario: A logged-in user books a ticket for an available event and receives
     *           a booking confirmation with a unique booking ID.
     * Steps:
     *   1. Login with default credentials.
     *   2. Navigate to /events and open the first available event card.
     *   3. Fill #customerName, #customer-email, #phone.
     *   4. Click #confirm-booking.
     *   5. App redirects to /bookings — assert at least one booking card is present.
     *   6. Assert booking ID (span[data-testid='booking-id']) is not blank.
     */
    // Retry-eligible (smoke group): RetryListener wires RetryAnalyzer automatically.
    // On failure retries up to 2 times — 1 s then 2 s back-off — before marking FAILED.
    // Static self-healing: BookingPage.getFirstBookingId() calls waitForVisibleWithFallback
    // with 3 locators — span[data-testid='booking-id'] → .booking-id class → XPath on card.
    // Also replaces findElements() (no-wait) with an explicit wait, fixing a React render race.
    // Falls through silently; no API call needed.
    // Attendee data: driven by @DataProvider from booking-data.json → attendees.booking.
    @Test(
        groups            = { AppConstants.SMOKE, AppConstants.REGRESSION },
        dataProvider      = "bookingAttendeeData",
        dataProviderClass = TestDataProvider.class,
        description       = "TC_BOOKING_001 — Standard ticket booking E2E flow"
    )
    @Story("TC_BOOKING_001")
    @Description("Full E2E: select event → fill booking form → confirm → verify booking ID on /bookings.")
    @Severity(SeverityLevel.BLOCKER)
    public void TC_BOOKING_001_standardTicketBookingCheckout(String attendeeName,
                                                              String attendeeEmail,
                                                              String attendeePhone) {
        long start = System.currentTimeMillis();

        Allure.step("Login with default credentials");
        loginWithDefaults();

        Allure.step("Navigate to /events and open first available event");
        navigateTo(AppConstants.PATH_EVENTS);
        EventDetailPage detailPage = new EventsPage(driver).openEventByTitle("");

        String eventTitle = detailPage.getEventTitle();
        Allure.step("Opened event: " + eventTitle);

        Allure.step("Fill booking form and confirm — attendee: " + attendeeName);
        detailPage.confirmBooking(attendeeName, attendeeEmail, attendeePhone);

        // App stays on /events/{id} after booking — navigate to /bookings explicitly
        Allure.step("Navigate to /bookings to verify booking was created");
        navigateTo(AppConstants.PATH_MY_BOOKINGS);
        BookingPage bookingsPage = new BookingPage(driver);
        actionUtils.pause(2000);

        Allure.step("Assert at least one booking card exists on /bookings");
        assertTrue(bookingsPage.hasBookings(),
                "Expected booking cards on /bookings. URL: " + actionUtils.getCurrentUrl());

        Allure.step("Assert a booking card with non-blank ID exists");
        String bookingId = bookingsPage.getFirstBookingId();
        assertFalse(bookingId.isBlank(), "Booking ID should not be blank after successful booking");

        TestUtils.logResult("TC_BOOKING_001", true, System.currentTimeMillis() - start,
                "Event: " + eventTitle + " | Booking ID: " + bookingId);
    }

    /*
     * TC_BOOKING_002 — Sold-Out Event Prevents Booking
     * Scenario: When an event has no remaining seats, the confirm-booking button
     *           must be disabled and the user cannot proceed.
     * Steps:
     *   1. Login and navigate to /events.
     *   2. Open the first available event detail page.
     *   3. Check EventDetailPage.isSoldOut() — if true, assert button is disabled
     *      and clicking it does not navigate away.
     *   4. If no sold-out event is found at runtime, skip the test gracefully.
     */
    @Test(
        groups      = { AppConstants.REGRESSION },
        description = "TC_BOOKING_002 — Sold-out event prevents booking"
    )
    @Story("TC_BOOKING_002")
    @Description("Verifies sold-out state: confirm button is disabled and checkout is blocked.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_BOOKING_002_soldOutPrevention() {
        long start = System.currentTimeMillis();

        Allure.step("Login and open first event");
        loginWithDefaults();
        navigateTo(AppConstants.PATH_EVENTS);
        EventDetailPage detailPage = new EventsPage(driver).openEventByTitle("");

        String currentUrl = actionUtils.getCurrentUrl();

        if (detailPage.isSoldOut()) {
            Allure.step("Event is sold-out — assert confirm button is disabled");
            assertTrue(detailPage.isBookNowDisabled(),
                    "Confirm-booking button should be disabled for a sold-out event");

            Allure.step("Attempt click — URL must not change");
            try { detailPage.clickBookNow(); } catch (Exception ignored) {}
            assertEquals(actionUtils.getCurrentUrl(), currentUrl,
                    "URL should not change when clicking a disabled booking button");
        } else {
            throw new org.testng.SkipException(
                    "TC_BOOKING_002 skipped — no sold-out event found at runtime. "
                    + "Run against an event that has reached its seat capacity.");
        }

        TestUtils.logResult("TC_BOOKING_002", true, System.currentTimeMillis() - start,
                "Sold-out state validated for URL: " + currentUrl);
    }
}
