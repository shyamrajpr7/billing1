package com.shop.ai;

import org.bson.Document;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

/**
 * Conversational AI backend. OpenAI-compatible, auto-detects the provider from
 * the API key format:
 * <ul>
 *   <li>{@code gsk_...} → Groq ({@code api.groq.com}, default model {@code llama-3.3-70b-versatile})</li>
 *   <li>{@code xai-...} → xAI / Grok ({@code api.x.ai}, default model {@code grok-4.5})</li>
 * </ul>
 *
 * <p>The key is read from the environment variable {@code GROQ_API_KEY} or
 * {@code GROK_API_KEY}, or from {@code ~/.groq_key} / {@code ~/.grok_key}
 * (first line). The model can be overridden with {@code LLM_MODEL}.</p>
 */
public class LLMAssistant {

    private static final String GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private static final String XAI_ENDPOINT = "https://api.x.ai/v1/chat/completions";

    private static LLMAssistant instance;

    private final String apiKey;
    private final String provider;
    private final String endpoint;
    private final String model;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private LLMAssistant() {
        this.apiKey = resolveKey();
        boolean groq = !apiKey.isEmpty() && apiKey.startsWith("gsk_");
        boolean xai = !apiKey.isEmpty() && apiKey.startsWith("xai-");

        if (xai) {
            provider = "xAI Grok";
            endpoint = XAI_ENDPOINT;
            model = env("LLM_MODEL", env("GROK_MODEL", "grok-4.5"));
        } else if (groq) {
            provider = "Groq";
            endpoint = GROQ_ENDPOINT;
            model = env("LLM_MODEL", env("GROQ_MODEL", "llama-3.3-70b-versatile"));
        } else {
            provider = "Groq";
            endpoint = GROQ_ENDPOINT;
            model = env("LLM_MODEL", env("GROQ_MODEL", "llama-3.3-70b-versatile"));
        }
    }

    public static synchronized LLMAssistant getInstance() {
        if (instance == null) {
            instance = new LLMAssistant();
        }
        return instance;
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? fallback : v.trim();
    }

    private static String resolveKey() {
        String key = System.getenv("GROQ_API_KEY");
        if (key != null && !key.isBlank()) return key.trim();
        key = System.getenv("GROK_API_KEY");
        if (key != null && !key.isBlank()) return key.trim();
        key = readFirstLine("~/.groq_key");
        if (key != null && !key.isEmpty()) return key;
        key = readFirstLine("~/.grok_key");
        return key == null ? "" : key;
    }

    private static String readFirstLine(String path) {
        try {
            Path p = Paths.get(System.getProperty("user.home"), path.replaceFirst("^~/", ""));
            if (Files.exists(p)) {
                List<String> lines = Files.readAllLines(p);
                if (!lines.isEmpty()) return lines.get(0).trim();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    /**
     * Sends a single user message to the LLM and returns its reply.
     *
     * @throws IllegalStateException if the key is not configured
     * @throws RuntimeException      on network / API errors
     */
    public String chat(String systemPrompt, String userText) {
        if (!isConfigured()) {
            throw new IllegalStateException("No API key set. Set GROQ_API_KEY, or put your key in ~/.groq_key.");
        }

        Document payload = new Document()
                .append("model", model)
                .append("stream", false)
                .append("messages", List.of(
                        new Document("role", "system").append("content", systemPrompt),
                        new Document("role", "user").append("content", userText)
                ));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toJson()))
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                throw new RuntimeException(provider + " API error " + response.statusCode() + ": "
                        + truncate(response.body(), 300));
            }

            Document root = Document.parse(response.body());
            Document message = root.getList("choices", Document.class)
                    .get(0)
                    .get("message", Document.class);
            String content = message.getString("content");
            return content == null ? "" : content.trim();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request interrupted.", e);
        } catch (java.net.ConnectException e) {
            throw new RuntimeException("Could not reach the " + provider + " API. Check your internet connection.", e);
        } catch (Exception e) {
            throw new RuntimeException(provider + " request failed: " + e.getMessage(), e);
        }
    }

    private String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s == null ? "" : s;
        return s.substring(0, max) + "...";
    }
}
