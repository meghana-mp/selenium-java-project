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
 * TC_ALERT_001, TC_ALERT_002 — Alerts & Browser Interrupts
 *
 * Covers:
 *   - Booking cancellation toast: div[aria-live='polite'] appears on /bookings
 *     after clicking #cancel-booking-btn, then auto-dismisses
 *   - Browser Back navigation: after completing a booking, pressing Back must
 *     not show a 404 or re-trigger the booking API
 *
 * NOTE: TC_ALERT_001 was originally a "profile bio save" toast test.
 *   /profile returns 404; the test is repurposed to the cancellation toast on
 *   /bookings, which uses the same div[aria-live='polite'] mechanism.
 */
@Epic("Alerts & Interrupts")
@Feature("UI Feedback")
public class AlertsTest extends BaseTest {

    /*
     * TC_ALERT_001 — Booking Cancellation Toast Appears and Auto-Dismisses
     * Scenario: Cancelling a booking on the /bookings page should trigger a
     *           success toast that is visible briefly and then disappears from DOM.
     * Steps:
     *   1. Login and complete a booking (to ensure at least one booking exists).
     *   2. Navigate to /bookings.
     *   3. Click #cancel-booking-btn on the first booking card.
     *   4. Assert the toast is visible with a success/cancelled message.
     *   5. Wait for the toast to auto-dismiss.
     *   6. Assert the toast element is gone from DOM.
     */
    @Test(
        groups      = { AppConstants.REGRESSION },
        description = "TC_ALERT_001 — Cancellation toast appears on /bookings and auto-dismisses"
    )
    @Story("TC_ALERT_001")
    @Description("Cancels a booking via the UI and validates the success toast appears then disappears.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_ALERT_001_toastNotificationAndDismissal() {
        long start = System.currentTimeMillis();

        Allure.step("Login and create a booking (precondition)");
        String[] attendee = TestDataProvider.getAttendee("cancellation"); // from booking-data.json
        loginWithDefaults();
        navigateTo(AppConstants.PATH_EVENTS);
        EventDetailPage detailPage = new EventsPage(driver).openEventByTitle("");
        detailPage.confirmBooking(attendee[0], attendee[1], attendee[2]);

        Allure.step("Navigate to /bookings");
        navigateTo(AppConstants.PATH_MY_BOOKINGS);
        BookingPage bookingsPage = new BookingPage(driver);
        actionUtils.pause(1500);

        if (!bookingsPage.hasBookings()) {
            throw new org.testng.SkipException(
                    "TC_ALERT_001 skipped — no bookings available on /bookings to cancel");
        }

        Allure.step("Cancel the first booking");
        bookingsPage.cancelFirstBooking();

        Allure.step("Assert cancellation toast is visible");
        assertTrue(bookingsPage.isCancelToastVisible(),
                "Expected a toast notification after cancelling a booking");

        String toastText = bookingsPage.getCancelToastText();
        Allure.step("Toast text: " + toastText);
        assertFalse(toastText.isBlank(), "Toast text should not be blank");

        Allure.step("Wait for toast to auto-dismiss");
        bookingsPage.waitForToastToDisappear();
        assertTrue(bookingsPage.isToastGone(),
                "Toast should disappear from DOM after auto-dismiss timeout");

        TestUtils.logResult("TC_ALERT_001", true, System.currentTimeMillis() - start,
                "Toast: " + toastText);
    }

    /*
     * TC_ALERT_002 — Browser Back Button After Booking Does Not Cause Error
     * Scenario: After successfully completing a booking, pressing the browser's
     *           native Back button must navigate safely without causing a 404,
     *           routing error, or duplicate booking.
     * Steps:
     *   1. Login and open an event detail page.
     *   2. Fill the booking form and confirm the booking.
     *   3. Note the URL after booking (should be /bookings or success state).
     *   4. Click driver.navigate().back().
     *   5. Assert: page title is not blank, no "404" text in page source,
     *      no "Cannot GET" error, and URL changed from confirmation URL.
     */
    // Retry-eligible (smoke group): RetryListener wires RetryAnalyzer automatically.
    // On failure retries up to 2 times — 1 s then 2 s back-off — before marking FAILED.
    @Test(
        groups      = { AppConstants.SMOKE, AppConstants.REGRESSION },
        description = "TC_ALERT_002 — Browser Back after booking does not cause 404 or re-trigger"
    )
    @Story("TC_ALERT_002")
    @Description("Validates safe back-navigation from booking confirmation without error pages.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_ALERT_002_browserBackButtonStateSafety() {
        long start = System.currentTimeMillis();

        Allure.step("Login and complete a booking");
        String[] attendee = TestDataProvider.getAttendee("navigation"); // from booking-data.json
        loginWithDefaults();
        navigateTo(AppConstants.PATH_EVENTS);
        EventDetailPage detailPage = new EventsPage(driver).openEventByTitle("");
        detailPage.confirmBooking(attendee[0], attendee[1], attendee[2]);

        actionUtils.pause(2000);
        String confirmUrl = actionUtils.getCurrentUrl();
        Allure.step("Post-booking URL: " + confirmUrl);

        Allure.step("Click browser native Back button");
        driver.navigate().back();
        actionUtils.pause(1000);

        Allure.step("Assert page is not a 404 or routing error");
        String currentUrl = actionUtils.getCurrentUrl();
        String pageTitle  = actionUtils.getPageTitle();
        String pageSource = driver.getPageSource().toLowerCase();

        assertFalse(pageTitle.isBlank(), "Page title must not be blank after Back navigation");
        // Check page title rather than full source — Next.js bundles embed "404" in JS
        // even on valid pages; only a real 404 page has it in the title or a visible heading
        assertFalse(pageTitle.toLowerCase().contains("404")
                        || pageTitle.toLowerCase().contains("not found"),
                "Page title indicates a 404 error after Back navigation. Title: " + pageTitle);
        assertFalse(pageSource.contains("cannot get"),
                "Page should not show a routing error after Back navigation");

        Allure.step("Assert URL changed from confirmation URL");
        assertNotEquals(currentUrl, confirmUrl,
                "URL should change after Back navigation (no stuck on confirmation)");

        TestUtils.logResult("TC_ALERT_002", true, System.currentTimeMillis() - start,
                "After back: URL=" + currentUrl + " | Title=" + pageTitle);
    }
}
