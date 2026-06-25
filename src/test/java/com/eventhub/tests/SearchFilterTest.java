package com.eventhub.tests;

import com.eventhub.constants.AppConstants;
import com.eventhub.pages.EventsPage;
import com.eventhub.tests.base.BaseTest;
import com.eventhub.tests.dataproviders.TestDataProvider;
import com.eventhub.utils.TestUtils;
import io.qameta.allure.*;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/**
 * TC_SEARCH_001, TC_SEARCH_002 — Search & Filtering
 *
 * Covers:
 *   - EventsPage: real locator input[placeholder='Search events, venues…']
 *   - Select dropdowns for category and city/location filters
 *   - JSON DataProvider: searchKeywords and gibberishQuery from search-data.json
 *   - Graceful empty-state handling ("No events found")
 */
@Epic("Search & Filtering")
@Feature("Event Discovery")
public class SearchFilterTest extends BaseTest {

    /*
     * TC_SEARCH_001 — Real-time Multi-Filter Event Search
     * Scenario: Typing in the search bar combined with category/location selects
     *           must return at least one relevant event card.
     * Steps:
     *   1. Login and navigate to /events.
     *   2. Type the keyword into the search input.
     *   3. Optionally select category and location from the dropdowns.
     *   4. Assert at least one event card is returned.
     */
    // Retry-eligible (smoke group): RetryListener wires RetryAnalyzer automatically.
    // On failure retries up to 2 times — 1 s then 2 s back-off — before marking FAILED.
    // Dynamic MCP healing: EventsPage.search() calls waitForVisibleWithMcpHealing — tries
    // primary CSS locator → input[type='search'] fallback → generic name/placeholder fallback.
    // Only if all 3 fail is the Claude API called to derive a selector from the live DOM.
    // Requires ANTHROPIC_API_KEY env var; degrades gracefully when unset.
    @Test(
        groups        = { AppConstants.SMOKE, AppConstants.REGRESSION },
        dataProvider  = "searchKeywords",
        dataProviderClass = TestDataProvider.class,
        description   = "TC_SEARCH_001 — Real-time multi-filter search returns relevant events"
    )
    @Story("TC_SEARCH_001")
    @Description("Applies keyword + category + location filters and validates at least one result.")
    @Severity(SeverityLevel.CRITICAL)
    public void TC_SEARCH_001_realtimeMultiFilterSearch(String keyword, String category, String location) {
        long start = System.currentTimeMillis();

        Allure.step("Login and navigate to /events");
        loginWithDefaults();
        navigateTo(AppConstants.PATH_EVENTS);
        EventsPage eventsPage = new EventsPage(driver);

        Allure.step("Type keyword: " + keyword);
        eventsPage.search(keyword);

        Allure.step("Select category filter: " + category);
        try { eventsPage.filterByCategory(category); } catch (Exception ignored) {}

        Allure.step("Select location filter: " + location);
        try { eventsPage.filterByLocation(location); } catch (Exception ignored) {}

        Allure.step("Assert at least one event card returned");
        int count = eventsPage.getResultCount();
        assertTrue(count > 0,
                "Expected ≥1 result for keyword/category '" + keyword + "'");

        TestUtils.logResult("TC_SEARCH_001", true, System.currentTimeMillis() - start,
                "Keyword: " + keyword + " | Results: " + count);
    }

    /*
     * TC_SEARCH_002 — No Results Found — Graceful Handling
     * Scenario: Entering random gibberish into the search box should yield
     *           zero cards and/or a user-friendly "no events found" message.
     * Steps:
     *   1. Navigate to /events and type a random gibberish string.
     *   2. Wait for the search debounce to settle (handled inside EventsPage.search).
     *   3. Assert zero cards OR an empty-state message is shown.
     */
    @Test(
        groups        = { AppConstants.REGRESSION },
        dataProvider  = "gibberishQuery",
        dataProviderClass = TestDataProvider.class,
        description   = "TC_SEARCH_002 — Gibberish query shows empty-state message"
    )
    @Story("TC_SEARCH_002")
    @Description("Validates that the app gracefully handles a search with no results.")
    @Severity(SeverityLevel.NORMAL)
    public void TC_SEARCH_002_noResultsFoundGracefulHandling(String gibberish) {
        long start = System.currentTimeMillis();

        Allure.step("Login and navigate to /events");
        loginWithDefaults();
        navigateTo(AppConstants.PATH_EVENTS);
        EventsPage eventsPage = new EventsPage(driver);

        Allure.step("Search gibberish: " + gibberish);
        eventsPage.search(gibberish);

        Allure.step("Assert no event cards rendered or empty-state shown");
        boolean noCards      = eventsPage.areEventCardsAbsent() || eventsPage.getResultCount() == 0;
        boolean emptyMessage = eventsPage.isNoResultsShown();
        assertTrue(noCards || emptyMessage,
                "Expected 0 cards or an empty-state message for gibberish query '" + gibberish + "'"
                        + " — got " + eventsPage.getResultCount() + " cards");

        TestUtils.logResult("TC_SEARCH_002", true, System.currentTimeMillis() - start,
                "Query: " + gibberish);
    }
}
