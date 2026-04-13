package com.aitestgen.client;

import com.aitestgen.model.GeneratedTestResult;
import com.aitestgen.settings.PluginSettings;
import com.aitestgen.settings.PluginSettingsState;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP client for the Gemini generateContent API.
 * Sends prompts and parses responses, extracting generated test code from markdown fences.
 */
public final class ClaudeApiClient {

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile(
            "```java\\s*\\n(.*?)\\n\\s*```", Pattern.DOTALL
    );
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile(
            "(?:public\\s+)?class\\s+(\\w+)"
    );

    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    private static final int READ_TIMEOUT_SECONDS = 60;
    private static final int WRITE_TIMEOUT_SECONDS = 10;

    private final OkHttpClient httpClient;
    private final Gson gson;

    /** Creates a client with default timeout configuration. */
    public ClaudeApiClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    /** Creates a client with a custom OkHttpClient (for testing). */
    ClaudeApiClient(OkHttpClient httpClient) {
        this.httpClient = httpClient;
        this.gson = new Gson();
    }

    /**
     * Sends a test generation request to the Gemini API.
     * This call blocks and should be invoked from a background thread.
     */
    public GeneratedTestResult generate(String systemPrompt, String userPrompt) {
        String apiKey = PluginSettings.getApiKey();
        if (apiKey.isEmpty()) {
            return GeneratedTestResult.failure(
                    "API key not configured. Go to Settings > Tools > AI Test Generator."
            );
        }

        PluginSettingsState settings = PluginSettingsState.getInstance();
        String requestJson = buildRequestJson(systemPrompt, userPrompt, settings);

        Request request = new Request.Builder()
                .url(GROQ_API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("content-type", "application/json")
                .post(RequestBody.create(requestJson, JSON_MEDIA_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            return handleResponse(response);
        } catch (IOException e) {
            return GeneratedTestResult.failure("Network error: " + e.getMessage());
        }
    }

    private String buildRequestJson(String systemPrompt, String userPrompt, PluginSettingsState settings) {
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", systemPrompt);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", userPrompt);

        JsonArray messages = new JsonArray();
        messages.add(systemMessage);
        messages.add(userMessage);

        JsonObject root = new JsonObject();
        root.addProperty("model", settings.getModelName());
        root.add("messages", messages);
        root.addProperty("max_tokens", settings.getMaxTokens());

        return gson.toJson(root);
    }

    /** Parses an HTTP response into a GeneratedTestResult. Package-private for testing. */
    GeneratedTestResult handleResponse(Response response) throws IOException {
        ResponseBody body = response.body();
        if (body == null) {
            return GeneratedTestResult.failure("Empty response from API (HTTP " + response.code() + ")");
        }

        String responseText = body.string();

        if (!response.isSuccessful()) {
            return handleErrorResponse(response.code(), responseText);
        }

        return parseSuccessResponse(responseText);
    }

    private GeneratedTestResult handleErrorResponse(int statusCode, String responseText) {
        if (statusCode == 429) {
            return GeneratedTestResult.failure(
                    "Rate limited by Groq API. Please try again in a moment."
            );
        }

        try {
            JsonObject errorJson = gson.fromJson(responseText, JsonObject.class);
            if (errorJson.has("error")) {
                JsonObject error = errorJson.getAsJsonObject("error");
                String message = error.has("message") ? error.get("message").getAsString() : "Unknown error";
                return GeneratedTestResult.failure("API error (HTTP " + statusCode + "): " + message);
            }
        } catch (JsonSyntaxException ignored) {
            // fall through to generic message
        }

        return GeneratedTestResult.failure("API error (HTTP " + statusCode + "): " + responseText);
    }

    private GeneratedTestResult parseSuccessResponse(String responseText) {
        try {
            JsonObject responseJson = gson.fromJson(responseText, JsonObject.class);
            JsonArray choices = responseJson.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                return GeneratedTestResult.failure("Empty choices in API response");
            }

            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            String text = message.get("content").getAsString();
            return extractTestCode(text);
        } catch (JsonSyntaxException e) {
            return GeneratedTestResult.failure("Failed to parse API response: " + e.getMessage());
        } catch (Exception e) {
            return GeneratedTestResult.failure("Unexpected error parsing response: " + e.getMessage());
        }
    }

    /** Extracts Java code from markdown fences and identifies the test class name. */
    static GeneratedTestResult extractTestCode(String responseText) {
        Matcher codeMatcher = CODE_FENCE_PATTERN.matcher(responseText);
        String testCode;
        if (codeMatcher.find()) {
            testCode = codeMatcher.group(1).trim();
        } else {
            testCode = responseText.trim();
        }

        String testClassName = "GeneratedTest";
        Matcher classMatcher = CLASS_NAME_PATTERN.matcher(testCode);
        if (classMatcher.find()) {
            testClassName = classMatcher.group(1);
        }

        int methodCount = countTestMethods(testCode);
        String explanation = String.format("Generated %d test method(s) in %s", methodCount, testClassName);

        return GeneratedTestResult.success(testClassName, testCode, explanation);
    }

    private static int countTestMethods(String code) {
        int count = 0;
        int index = 0;
        while ((index = code.indexOf("@Test", index)) != -1) {
            count++;
            index += 5;
        }
        return count;
    }
}
