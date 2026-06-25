package com.eventhub.utils;

import com.eventhub.config.ConfigManager;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Thin RestAssured wrapper used by hybrid tests (TC15, TC16).
 * Encapsulates base-URI setup and bearer-token auth so test classes stay clean.
 */
public final class ApiUtils {

    private static final Logger log = LoggerFactory.getLogger(ApiUtils.class);
    private ApiUtils() {}

    private static RequestSpecification baseSpec(String token) {
        String baseUrl = ConfigManager.getInstance().getProperty("app.api.base.url");
        RequestSpecification spec = RestAssured.given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON);
        if (token != null && !token.isBlank()) {
            spec.header("Authorization", "Bearer " + token);
        }
        return spec;
    }

    /** POST to the given path with the provided body map; returns the full response. */
    public static Response post(String path, Map<String, Object> body, String token) {
        log.debug("POST {}", path);
        return baseSpec(token)
                .body(body)
                .when()
                .post(path)
                .then()
                .extract().response();
    }

    /** GET the given path; returns the full response. */
    public static Response get(String path, String token) {
        log.debug("GET {}", path);
        return baseSpec(token)
                .when()
                .get(path)
                .then()
                .log().ifError()
                .extract().response();
    }

    /**
     * Obtains a Bearer token by posting credentials to the login API.
     * Returns the token string, or throws if login fails.
     */
    public static String getAuthToken(String email, String password) {
        String loginPath = "/api/auth/login";
        Map<String, Object> creds = Map.of("email", email, "password", password);
        Response resp = post(loginPath, creds, null);
        if (resp.statusCode() != 200) {
            throw new RuntimeException("API login failed: " + resp.statusCode() + " " + resp.body().asString());
        }
        String token = resp.jsonPath().getString("token");
        log.info("API auth token obtained");
        return token;
    }
}
