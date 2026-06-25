package com.eventhub.tests;

import com.eventhub.constants.AppConstants;
import com.eventhub.pages.CreateEventPage;
import com.eventhub.pages.EventDetailPage;
import com.eventhub.pages.EventsPage;
import com.eventhub.tests.base.BaseTest;
import com.eventhub.utils.TestUtils;
import io.qameta.allure.*;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * TC_LOC_001, TC_LOC_002 — Localization & Date Handling
 *
 * Covers:
 *   - CreateEventPage: native date input at /admin/events enforces a 'min'
 *     attribute preventing selection of past dates
 *   - EventDetailPage: event time/date string is consistent across views
 *     (opening the same event from the list yields the same time text)
 */
@Epic("Localization & Dates")
@Feature("Date & Timezone Handling")
public class LocalizationTest extends BaseTest {

    /*
     * TC_LOC_001 — Future Date Picker Constraints on Create Event Page
     * Scenario: The date-and-time input on /admin/events must have a 'min'
     *           attribute set to today's date, preventing selection of past dates.
     * Steps:
     *   1. Login and navigate to /admin/events.
     *   2. Locate the date input (id="event-date-&-time" or input[type='date']).
     *   3. Read the 'min' HTML attribute.
     *   4. Assert the 'min' attribute is present and non-blank.
     */
    @Test(
        groups      = { AppConstants.REGRESSION },
        description = "TC_LOC_001 — Create-event date input enforces past-date restriction via min attribute"
    )
    @Story("TC_LOC_001")
    @Description("Opens the admin event form and verifies the date input has a 'min' attribute blocking past dates.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_LOC_001_futureDatePickerConstraints() {
        long start = System.currentTimeMillis();

        Allure.step("Login and navigate to /admin/events");
        loginWithDefaults();
        navigateTo(AppConstants.PATH_CREATE_EVENT);
        CreateEventPage createPage = new CreateEventPage(driver);

        Allure.step("Click on the date picker input to focus it");
        createPage.openDatePicker();

        Allure.step("Assert past dates are blocked (min attribute is set)");
        assertTrue(createPage.areAllPastDatesDisabled(),
                "Expected the date input to have a 'min' attribute preventing past date selection");

        long disabled = createPage.countDisabledDates();
        Allure.step("countDisabledDates(): " + disabled);
        assertTrue(disabled >= 1,
                "At least the min-attribute check should count as 1 disabled-date constraint");

        TestUtils.logResult("TC_LOC_001", true, System.currentTimeMillis() - start,
                "Past-date constraint enforced. Disabled count: " + disabled);
    }

    /*
     * TC_LOC_002 — Event Time String Consistency Across Views
     * Scenario: The date/time string shown on an event's detail page must be
     *           identical when the same event is opened again from the events list.
     * Steps:
     *   1. Login and navigate to /events.
     *   2. Open the first event card and capture title + time string.
     *   3. Navigate back to /events and reopen the same event by title.
     *   4. Assert both time strings are equal.
     */
    @Test(
        groups      = { AppConstants.REGRESSION },
        description = "TC_LOC_002 — Event time/timezone is identical on detail and re-opened views"
    )
    @Story("TC_LOC_002")
    @Description("Compares event time string across two separate opens of the same event detail page.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_LOC_002_timezoneConsistencyAcrossViews() {
        long start = System.currentTimeMillis();

        Allure.step("Login and open first event from /events");
        loginWithDefaults();
        navigateTo(AppConstants.PATH_EVENTS);
        EventDetailPage detailPage = new EventsPage(driver).openEventByTitle("");

        Allure.step("Capture title and time from first visit");
        String eventTitle     = detailPage.getEventTitle();
        String timeFirstVisit = detailPage.getEventTime();
        Allure.step("Event: " + eventTitle + " | Time: " + timeFirstVisit);

        Allure.step("Navigate back to /events and reopen the same event");
        navigateTo(AppConstants.PATH_EVENTS);
        EventDetailPage detailPageSecond = new EventsPage(driver).openEventByTitle(eventTitle);

        Allure.step("Capture time from second visit");
        String timeSecondVisit = detailPageSecond.getEventTime();
        Allure.step("Second visit time: " + timeSecondVisit);

        Allure.step("Assert both time strings are equal");
        assertEquals(timeSecondVisit, timeFirstVisit,
                "Event time mismatch. First: '" + timeFirstVisit
                        + "' | Second: '" + timeSecondVisit + "'");

        TestUtils.logResult("TC_LOC_002", true, System.currentTimeMillis() - start,
                "Event: " + eventTitle + " | Time consistent: " + timeFirstVisit);
    }
}
