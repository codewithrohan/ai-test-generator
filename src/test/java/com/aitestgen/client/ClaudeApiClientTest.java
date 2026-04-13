package com.aitestgen.client;

import com.aitestgen.model.GeneratedTestResult;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Tests for ClaudeApiClient HTTP behavior and response parsing.
 * Uses OkHttp MockWebServer to simulate the Claude API.
 */
public class ClaudeApiClientTest {

    private MockWebServer server;
    private ClaudeApiClient client;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        OkHttpClient httpClient = new OkHttpClient();
        client = new ClaudeApiClient(httpClient);
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    // --- extractTestCode tests (static, no HTTP) ---

    @Test
    public void shouldExtractCodeFromJavaFences() {
        String response = "Here is the test:\n```java\npublic class FooTest {\n    @Test\n    void test() {}\n}\n```\n";
        GeneratedTestResult result = ClaudeApiClient.extractTestCode(response);

        assertTrue(result.isSuccess());
        assertEquals("FooTest", result.getTestClassName());
        assertTrue(result.getTestCode().contains("class FooTest"));
        assertTrue(result.getTestCode().contains("@Test"));
    }

    @Test
    public void shouldFallbackToRawTextWhenNoFences() {
        String response = "public class BarTest { @Test void test() {} }";
        GeneratedTestResult result = ClaudeApiClient.extractTestCode(response);

        assertTrue(result.isSuccess());
        assertEquals("BarTest", result.getTestClassName());
        assertTrue(result.getTestCode().contains("BarTest"));
    }

    @Test
    public void shouldCountTestMethods() {
        String code = "```java\npublic class MyTest {\n    @Test void a() {}\n    @Test void b() {}\n    @Test void c() {}\n}\n```";
        GeneratedTestResult result = ClaudeApiClient.extractTestCode(code);

        assertTrue(result.isSuccess());
        assertTrue(result.getExplanation().contains("3 test method(s)"));
    }

    @Test
    public void shouldUseDefaultNameWhenNoClassFound() {
        String code = "```java\n@Test void orphan() {}\n```";
        GeneratedTestResult result = ClaudeApiClient.extractTestCode(code);

        assertTrue(result.isSuccess());
        assertEquals("GeneratedTest", result.getTestClassName());
    }

    @Test
    public void shouldHandleCodeWithoutTestAnnotations() {
        String code = "```java\npublic class EmptyTest {}\n```";
        GeneratedTestResult result = ClaudeApiClient.extractTestCode(code);

        assertTrue(result.isSuccess());
        assertEquals("EmptyTest", result.getTestClassName());
        assertTrue(result.getExplanation().contains("0 test method(s)"));
    }

    @Test
    public void shouldExtractFirstCodeBlock() {
        String response = "```java\npublic class FirstTest { @Test void a() {} }\n```\n\n"
                + "```java\npublic class SecondTest { @Test void b() {} }\n```";
        GeneratedTestResult result = ClaudeApiClient.extractTestCode(response);

        assertTrue(result.isSuccess());
        assertEquals("FirstTest", result.getTestClassName());
    }

    // --- handleResponse tests (via MockWebServer) ---

    @Test
    public void shouldParseSuccessfulApiResponse() throws Exception {
        String json = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"```java\\npublic class SvcTest { @Test void t() {} }\\n```\"}}]}";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(json));

        okhttp3.Response response = client_call(server);
        GeneratedTestResult result = client.handleResponse(response);

        assertTrue(result.isSuccess());
        assertEquals("SvcTest", result.getTestClassName());
        assertTrue(result.getTestCode().contains("SvcTest"));
    }

    @Test
    public void shouldHandleRateLimitResponse() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(429).setBody("{\"error\":{\"code\":429,\"message\":\"rate limited\",\"status\":\"RESOURCE_EXHAUSTED\"}}"));

        okhttp3.Response response = client_call(server);
        GeneratedTestResult result = client.handleResponse(response);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Rate limited"));
    }

    @Test
    public void shouldHandleAuthErrorResponse() throws Exception {
        String json = "{\"error\":{\"message\":\"invalid x-api-key\"}}";
        server.enqueue(new MockResponse().setResponseCode(401).setBody(json));

        okhttp3.Response response = client_call(server);
        GeneratedTestResult result = client.handleResponse(response);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("401"));
        assertTrue(result.getErrorMessage().contains("invalid x-api-key"));
    }

    @Test
    public void shouldHandleServerErrorWithNonJsonBody() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

        okhttp3.Response response = client_call(server);
        GeneratedTestResult result = client.handleResponse(response);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("500"));
    }

    @Test
    public void shouldHandleEmptyContentArray() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"choices\":[]}"));

        okhttp3.Response response = client_call(server);
        GeneratedTestResult result = client.handleResponse(response);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Empty choices"));
    }

    /** Helper: sends a GET to MockWebServer and returns the raw OkHttp Response. */
    private okhttp3.Response client_call(MockWebServer server) throws IOException {
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(server.url("/v1/messages"))
                .build();
        return new OkHttpClient().newCall(request).execute();
    }
}
