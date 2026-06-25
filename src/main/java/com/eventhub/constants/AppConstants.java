package com.eventhub.constants;

/** Central store for all framework-level constants. No instantiation. */
public final class AppConstants {

    private AppConstants() {}

    // ── TestNG group names ──────────────────────────────────────────────────
    public static final String SMOKE      = "smoke";
    public static final String REGRESSION = "regression";

    // ── Page paths (appended to app.base.url from config) ──────────────────
    public static final String PATH_LOGIN        = "/login";
    public static final String PATH_REGISTER     = "/register";
    public static final String PATH_HOME         = "/";
    public static final String PATH_EVENTS       = "/events";
    public static final String PATH_MY_BOOKINGS  = "/bookings";
    public static final String PATH_ADMIN_EVENTS = "/admin/events";
    public static final String PATH_CREATE_EVENT  = "/admin/events"; // alias used by test classes
    public static final String PATH_PROFILE       = "/bookings";     // /profile 404s; tests go to /bookings

    // ── API endpoint paths (relative to app.api.base.url) ─────────────────
    public static final String API_EVENTS        = "/api/events";
    public static final String API_BOOKINGS      = "/api/bookings";
    public static final String API_AUTH_LOGIN    = "/api/auth/login";

    // ── Test data file paths (classpath-relative) ──────────────────────────
    public static final String TESTDATA_AUTH    = "testdata/auth-data.json";
    public static final String TESTDATA_SEARCH  = "testdata/search-data.json";
    public static final String TESTDATA_BOOKING = "testdata/booking-data.json";
    public static final String TESTDATA_PAYMENT = "testdata/payment-data.json";
    public static final String TESTDATA_FORM    = "testdata/form-data.json";
    public static final String TESTDATA_EVENT   = "testdata/event-data.json";

    // ── Timeouts ───────────────────────────────────────────────────────────
    public static final int DEFAULT_EXPLICIT_WAIT = 15;
    public static final int SHORT_WAIT            = 5;
    public static final int LONG_WAIT             = 30;

    // ── Mobile viewport ────────────────────────────────────────────────────
    public static final int MOBILE_WIDTH  = 375;
    public static final int MOBILE_HEIGHT = 812;
}
