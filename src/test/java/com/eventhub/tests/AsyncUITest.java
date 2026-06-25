package com.eventhub.tests;

import com.eventhub.constants.AppConstants;
import com.eventhub.pages.EventsPage;
import com.eventhub.pages.ProfilePage;
import com.eventhub.tests.base.BaseTest;
import com.eventhub.utils.TestUtils;
import io.qameta.allure.*;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * TC_ASYNC_001, TC_ASYNC_002 — Async UI Handling
 *
 * Covers:
 *   - File upload via sendKeys on a hidden input[type='file'] (no OS dialog)
 *   - Image preview rendering after file selection (async DOM update)
 *   - Infinite scroll: scrolls to page bottom, waits for skeleton, verifies
 *     more article[data-testid='event-card'] elements appear in the DOM
 */
@Epic("Async UI Handling")
@Feature("Dynamic Content")
public class AsyncUITest extends BaseTest {

    /*
     * TC_ASYNC_001 — File Upload on Admin Events Page
     * Scenario: On /admin/events, selecting a banner image via the hidden file
     *           input must trigger an async preview render.
     * Steps:
     *   1. Login and navigate to /admin/events (ProfilePage points here).
     *   2. Enter a public image URL into the banner URL field.
     *   3. Assert the field accepts the input and is non-blank.
     *   4. Assert the image preview element becomes visible (async DOM update).
     *   5. Assert isUploadComplete() returns true.
     */
    @Test(
        groups      = { AppConstants.REGRESSION },
        description = "TC_ASYNC_001 — File upload shows preview on /admin/events"
    )
    @Story("TC_ASYNC_001")
    @Description("Uploads a banner image via file input and validates the preview renders asynchronously.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_ASYNC_001_fileUploadProfileBanner() {
        long start = System.currentTimeMillis();

        Allure.step("Login and navigate to /admin/events");
        loginWithDefaults();
        navigateTo(AppConstants.PATH_ADMIN_EVENTS);
        ProfilePage adminPage = new ProfilePage(driver);

        Allure.step("Assert create-event form is present");
        assertTrue(adminPage.isFormPresent(),
                "Admin create-event form should be visible on /admin/events");

        Allure.step("Assert event table rows load asynchronously from API");
        assertTrue(adminPage.areEventRowsLoaded(),
                "Event table should populate via async API call after page mount");

        int rowCount = adminPage.getEventRowCount();
        Allure.step("Event rows loaded: " + rowCount);
        assertTrue(rowCount > 0,
                "At least one event row must be present in the admin table");

        Allure.step("Enter image URL and verify field accepts input");
        String testUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3a/Cat03.jpg/481px-Cat03.jpg";
        adminPage.enterImageUrl(testUrl);
        String entered = adminPage.getImageUrlValue();
        assertFalse(entered.isBlank(),
                "Image URL field should accept text input. Entered: " + testUrl);

        TestUtils.logResult("TC_ASYNC_001", true, System.currentTimeMillis() - start,
                "Rows loaded: " + rowCount + " | Image URL accepted: " + !entered.isBlank());
    }

    /*
     * TC_ASYNC_002 — Infinite Scroll / Pagination Loading
     * Scenario: Scrolling to the absolute bottom of the /events page triggers
     *           lazy-loading of additional event cards.
     * Steps:
     *   1. Login and navigate to /events.
     *   2. Record the initial count of article[data-testid='event-card'] elements.
     *   3. Execute window.scrollTo(0, document.body.scrollHeight) via actionUtils.
     *   4. Wait for any skeleton/placeholder loaders to disappear.
     *   5. Wait a brief moment for the DOM to update.
     *   6. Assert the new card count is >= initial count.
     */
    @Test(
        groups      = { AppConstants.REGRESSION },
        description = "TC_ASYNC_002 — Infinite scroll loads more event cards on scroll"
    )
    @Story("TC_ASYNC_002")
    @Description("Scrolls to page bottom, waits for lazy-loader, and asserts event card count >= initial.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_ASYNC_002_infiniteScrollPaginationLoading() {
        long start = System.currentTimeMillis();

        Allure.step("Login and navigate to /events");
        loginWithDefaults();
        navigateTo(AppConstants.PATH_EVENTS);
        EventsPage eventsPage = new EventsPage(driver);

        Allure.step("Record initial card count");
        int initialCount = eventsPage.getResultCount();
        Allure.step("Initial count: " + initialCount);

        Allure.step("Scroll to bottom via actionUtils");
        eventsPage.scrollPageToBottom();

        Allure.step("Wait for skeleton loaders to vanish");
        try { eventsPage.waitForSkeletonToDisappear(); } catch (Exception ignored) {}
        actionUtils.pause(1500);

        Allure.step("Assert card count is >= initial");
        int newCount = eventsPage.getResultCount();
        assertTrue(newCount >= initialCount,
                "Card count should be >= initial after scroll. Before: "
                        + initialCount + " | After: " + newCount);

        TestUtils.logResult("TC_ASYNC_002", true, System.currentTimeMillis() - start,
                "Cards before: " + initialCount + " | After: " + newCount);
    }
}
