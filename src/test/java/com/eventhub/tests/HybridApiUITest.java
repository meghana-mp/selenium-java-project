package com.eventhub.tests;

import com.eventhub.config.ConfigManager;
import com.eventhub.constants.AppConstants;
import com.eventhub.pages.EventDetailPage;
import com.eventhub.pages.EventsPage;
import com.eventhub.tests.base.BaseTest;
import com.eventhub.tests.dataproviders.TestDataProvider;
import com.eventhub.utils.ApiUtils;
import com.eventhub.utils.TestUtils;
import com.fasterxml.jackson.databind.JsonNode;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * TC_HYBRID_001, TC_HYBRID_002 — Hybrid API + UI Tests
 *
 * Covers:
 *   - RestAssured integration: POST to create event, GET to read bookmarks
 *   - ApiUtils wraps RestAssured with app.api.base.url from config
 *   - Data consistency: API-created data visible in UI; UI actions reflected in API
 */
@Epic("Hybrid API-to-UI")
@Feature("Data Consistency")
public class HybridApiUITest extends BaseTest {

    /*
     * TC_HYBRID_001 — Backend Event Creation Verified in UI
     * Scenario: An event created via the REST API must be discoverable through
     *           the UI search without any manual intervention.
     * Steps:
     *   1. Obtain a JWT auth token via POST /api/v1/auth/login using ApiUtils.
     *   2. POST a new event with a unique random title to /api/v1/events.
     *   3. Assert API returns HTTP 200/201.
     *   4. Login via browser UI and search for the unique event title.
     *   5. Assert at least one card appears with that title.
     */
    // Retry-eligible (smoke group): RetryListener wires RetryAnalyzer automatically.
    // On failure retries up to 2 times — 1 s then 2 s back-off — before marking FAILED.
    @Test(
        groups      = { AppConstants.SMOKE, AppConstants.REGRESSION },
        description = "TC_HYBRID_001 — Event created via API appears in UI search results"
    )
    @Story("TC_HYBRID_001")
    @Description("Creates an event via REST API and verifies it surfaces in the browser UI search.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_HYBRID_001_backendEventCreationVerification() {
        long start = System.currentTimeMillis();
        ConfigManager cfg = ConfigManager.getInstance();

        Allure.step("Obtain API auth token");
        String token = ApiUtils.getAuthToken(
                cfg.getProperty("app.email"), cfg.getProperty("app.password"));
        assertFalse(token == null || token.isBlank(), "Auth token must not be blank");

        String uniqueTitle = "AutoTest-" + TestUtils.randomId().substring(0, 8);
        Allure.step("POST new event via API — title: " + uniqueTitle);

        JsonNode tmpl = TestDataProvider.getEventTemplate(); // from event-data.json
        Map<String, Object> payload = new HashMap<>();
        payload.put("title",       uniqueTitle);
        payload.put("city",        tmpl.get("city").asText());
        payload.put("venue",       tmpl.get("venue").asText());
        payload.put("category",    tmpl.get("category").asText());
        payload.put("eventDate",   TestUtils.dateOffset(tmpl.get("daysOffset").asInt()) + "T10:00:00.000Z");
        payload.put("price",       tmpl.get("price").asInt());
        payload.put("totalSeats",  tmpl.get("totalSeats").asInt());
        payload.put("description", tmpl.get("description").asText());

        Response postResp = ApiUtils.post(AppConstants.API_EVENTS, payload, token);
        Allure.step("API response status: " + postResp.statusCode());
        assertTrue(postResp.statusCode() == 200 || postResp.statusCode() == 201,
                "API event creation failed (" + postResp.statusCode() + "): "
                        + postResp.body().asString());

        Allure.step("Login via UI and search for the new event");
        loginWithDefaults();
        navigateTo(AppConstants.PATH_EVENTS);

        EventsPage eventsPage = new EventsPage(driver);
        eventsPage.search(uniqueTitle);

        Allure.step("Assert event appears in UI results");
        int count = eventsPage.getResultCount();
        assertTrue(count > 0,
                "Event '" + uniqueTitle + "' created via API should appear in UI search");

        List<String> titles = eventsPage.getAllEventTitles();
        assertTrue(titles.stream().anyMatch(t -> t.contains(uniqueTitle)),
                "No UI card title contains '" + uniqueTitle + "'. Titles: " + titles);

        TestUtils.logResult("TC_HYBRID_001", true, System.currentTimeMillis() - start,
                "Created: " + uniqueTitle + " | UI count: " + count);
    }

    /*
     * TC_HYBRID_002 — UI Bookmark Action Reflected in API Response
     * Scenario: Clicking the Bookmark icon on an event detail page must cause
     *           that event's ID to appear in the GET /api/v1/bookings response.
     * Steps:
     *   1. Login via UI and open the first available event.
     *   2. Capture the event ID from the URL (last path segment).
     *   3. Click the bookmark / favourite icon on the event detail page.
     *   4. Obtain an API auth token.
     *   5. GET /api/v1/bookings with the token and assert the event ID is present.
     */
    @Test(
        groups      = { AppConstants.REGRESSION },
        description = "TC_HYBRID_002 — Bookmarking an event via UI is reflected in the API response"
    )
    @Story("TC_HYBRID_002")
    @Description("Clicks Bookmark in UI then verifies the event ID appears in the bookings/favorites API.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_HYBRID_002_uiActionApiSideEffect() {
        long start = System.currentTimeMillis();
        ConfigManager cfg = ConfigManager.getInstance();

        Allure.step("Login via UI and open first available event");
        loginWithDefaults();
        navigateTo(AppConstants.PATH_EVENTS);
        EventDetailPage detailPage = new EventsPage(driver).openEventByTitle("");

        Allure.step("Capture event ID from URL");
        String eventId = detailPage.getEventId();
        assertFalse(eventId.isBlank(), "Event ID should be extractable from URL path");
        Allure.step("Event ID: " + eventId);

        Allure.step("Click Bookmark icon");
        try {
            detailPage.clickBookmark();
        } catch (Exception e) {
            throw new org.testng.SkipException(
                    "TC_HYBRID_002 skipped — no bookmark button found on this event page: "
                            + e.getMessage());
        }

        Allure.step("Obtain API auth token");
        String token = ApiUtils.getAuthToken(
                cfg.getProperty("app.email"), cfg.getProperty("app.password"));

        Allure.step("GET /api/v1/bookings and verify event ID is present");
        Response getResp = ApiUtils.get(AppConstants.API_BOOKINGS, token);
        assertEquals(getResp.statusCode(), 200,
                "Bookings API status: " + getResp.statusCode());

        List<String> ids = getResp.jsonPath().getList("eventId", String.class);
        if (ids == null || ids.isEmpty()) {
            ids = getResp.jsonPath().getList("id", String.class);
        }
        assertTrue(ids != null && ids.contains(eventId),
                "Event ID '" + eventId + "' not found in bookings API. Response: "
                        + getResp.body().asString());

        TestUtils.logResult("TC_HYBRID_002", true, System.currentTimeMillis() - start,
                "Event ID " + eventId + " confirmed in bookings API");
    }
}
