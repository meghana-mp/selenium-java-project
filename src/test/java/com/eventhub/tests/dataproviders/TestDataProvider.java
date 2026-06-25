package com.eventhub.tests.dataproviders;

import com.eventhub.config.ConfigManager;
import com.eventhub.constants.AppConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.DataProvider;

import java.io.InputStream;

/**
 * Centralised TestNG DataProviders. Each provider reads from a JSON file
 * on the test classpath (src/test/resources/testdata/).
 */
public class TestDataProvider {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── JSON loader ──────────────────────────────────────────────────────────

    private static JsonNode load(String classpathPath) {
        try (InputStream is = TestDataProvider.class
                .getClassLoader().getResourceAsStream(classpathPath)) {
            if (is == null) throw new IllegalStateException("Test data file not found: " + classpathPath);
            return MAPPER.readTree(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load test data: " + classpathPath, e);
        }
    }

    // ── Auth ─────────────────────────────────────────────────────────────────

    @DataProvider(name = "validLoginData")
    public static Object[][] validLoginData() {
        JsonNode node = load(AppConstants.TESTDATA_AUTH).get("validUser");
        String email    = node.get("email").asText();
        String password = node.get("password").asText();
        // auth-data.json is committed with placeholder values so it is safe for a public repo.
        // When the file still has placeholders (local Docker build, first-time clone) fall back
        // to config.properties / APP_EMAIL env var so the test can still run.
        if (email.startsWith("YOUR_")) {
            email    = ConfigManager.getInstance().getProperty("app.email");
            password = ConfigManager.getInstance().getProperty("app.password");
        }
        return new Object[][] { { email, password } };
    }

    // ── Search ───────────────────────────────────────────────────────────────

    @DataProvider(name = "searchKeywords")
    public static Object[][] searchKeywords() {
        JsonNode searches = load(AppConstants.TESTDATA_SEARCH).get("validSearches");
        Object[][] data = new Object[searches.size()][3];
        for (int i = 0; i < searches.size(); i++) {
            JsonNode s = searches.get(i);
            data[i][0] = s.get("keyword").asText();
            data[i][1] = s.get("category").asText();
            data[i][2] = s.get("location").asText();
        }
        return data;
    }

    @DataProvider(name = "gibberishQuery")
    public static Object[][] gibberishQuery() {
        String q = load(AppConstants.TESTDATA_SEARCH).get("gibberishQuery").asText();
        return new Object[][] { { q } };
    }

    // ── Booking ──────────────────────────────────────────────────────────────

    /** Attendee for TC_BOOKING_001 — drives the booking form via DataProvider. */
    @DataProvider(name = "bookingAttendeeData")
    public static Object[][] bookingAttendeeData() {
        JsonNode a = load(AppConstants.TESTDATA_BOOKING).get("attendees").get("booking");
        return new Object[][] {
            { a.get("name").asText(), a.get("email").asText(), a.get("phone").asText() }
        };
    }

    /**
     * Static helper for tests where attendee data is a precondition, not a parameter.
     * scenario must match a key under attendees in booking-data.json:
     *   "booking" | "cancellation" | "navigation"
     * Returns [name, email, phone].
     */
    public static String[] getAttendee(String scenario) {
        JsonNode a = load(AppConstants.TESTDATA_BOOKING).get("attendees").get(scenario);
        return new String[] {
            a.get("name").asText(),
            a.get("email").asText(),
            a.get("phone").asText()
        };
    }

    @DataProvider(name = "boundaryQuantities")
    public static Object[][] boundaryQuantities() {
        JsonNode qty = load(AppConstants.TESTDATA_BOOKING).get("ticketQuantities");
        // The +/- button UI enforces qty >= 1 regardless of the target value;
        // one iteration (attempting zero) is sufficient to verify the minimum boundary.
        return new Object[][] {
            { qty.get("zero").asInt(), "zero quantity" }
        };
    }

    // ── Payment ──────────────────────────────────────────────────────────────

    @DataProvider(name = "invalidCards")
    public static Object[][] invalidCards() {
        JsonNode card = load(AppConstants.TESTDATA_PAYMENT).get("declinedCard");
        return new Object[][] {
            {
                card.get("number").asText(),
                card.get("expiry").asText(),
                card.get("cvv").asText(),
                "Declined card"
            }
        };
    }

    @DataProvider(name = "promoCodeData")
    public static Object[][] promoCodeData() {
        JsonNode codes = load(AppConstants.TESTDATA_PAYMENT).get("promoCodes");
        Object[][] data = new Object[codes.size()][3];
        for (int i = 0; i < codes.size(); i++) {
            JsonNode p = codes.get(i);
            data[i][0] = p.get("code").asText();
            data[i][1] = p.get("discountPercent").asInt();
            data[i][2] = p.get("baseAmount").asDouble();
        }
        return data;
    }

    // ── Event (API creation template) ────────────────────────────────────────

    /**
     * Static helper returning the event creation template from event-data.json.
     * Used by TC_HYBRID_001 to build the API payload — only the title is generated
     * dynamically (unique per run); everything else comes from the JSON.
     */
    public static JsonNode getEventTemplate() {
        return load(AppConstants.TESTDATA_EVENT).get("eventTemplate");
    }

    // ── Forms ─────────────────────────────────────────────────────────────────

    @DataProvider(name = "invalidRegistrationData")
    public static Object[][] invalidRegistrationData() {
        JsonNode reg = load(AppConstants.TESTDATA_FORM).get("invalidRegistration");
        return new Object[][] {
            {
                reg.get("email").asText(),
                reg.get("password").asText(),
                reg.get("phone").asText()
            }
        };
    }
}
