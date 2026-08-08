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
import java.util.regex.Pattern;

/**
 * Conversational AI backend. OpenAI-compatible, auto-detects the provider from
 * the API key format:
 * <ul>
 *   <li>{@code gsk_...} → Groq ({@code api.groq.com}, default model {@code llama-3.3-70b-versatile})</li>
 *   <li>{@code xai-...} → xAI / Grok ({@code api.x.ai}, default model {@code grok-4.5})</li>
 * </ul>
 *
 * <p>Live web search is available through {@link #chatWithWeb}: it calls a
 * search-capable model (default {@code openai/gpt-oss-20b} with the
 * {@code browser_search} tool, or {@code groq/compound-mini} which searches
 * automatically) so the assistant answers current, factual questions from the
 * live web — no buttons or extra setup. If the search model fails or is rate
 * limited it falls back to the base chat model.</p>
 *
 * <p>The key is read from the environment variable {@code GROQ_API_KEY} or
 * {@code GROK_API_KEY}, or from {@code ~/.groq_key} / {@code ~/.grok_key}
 * (first line). Models can be overridden with {@code LLM_MODEL},
 * {@code LLM_SEARCH_MODEL}.</p>
 */
public class LLMAssistant {

    private static final String GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private static final String XAI_ENDPOINT = "https://api.x.ai/v1/chat/completions";
    private static final Pattern CITATION_MARKERS = Pattern.compile("【[^】]*】");

    private static LLMAssistant instance;

    private final String apiKey;
    private final String provider;
    private final String endpoint;
    private final String model;
    private final String searchModel;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private LLMAssistant() {
        this.apiKey = resolveKey();
        boolean xai = !apiKey.isEmpty() && apiKey.startsWith("xai-");

        if (xai) {
            provider = "xAI Grok";
            endpoint = XAI_ENDPOINT;
            model = env("LLM_MODEL", env("GROK_MODEL", "grok-4.5"));
            searchModel = env("LLM_SEARCH_MODEL", env("GROK_SEARCH_MODEL", "grok-4.5"));
        } else {
            provider = "Groq";
            endpoint = GROQ_ENDPOINT;
            model = env("LLM_MODEL", env("GROQ_MODEL", "llama-3.3-70b-versatile"));
            searchModel = env("LLM_SEARCH_MODEL", env("GROQ_SEARCH_MODEL", "openai/gpt-oss-20b"));
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

    public String getSearchModel() {
        return searchModel;
    }

    /**
     * Plain chat reply (no web search).
     */
    public String chat(String systemPrompt, String userText) {
        return chat(systemPrompt, userText, false);
    }

    /**
     * Chat with automatic live web search for up-to-date, accurate answers.
     * Retries once on rate limits and falls back to the base model if the
     * search model is unavailable.
     */
    public String chatWithWeb(String systemPrompt, String userText) {
        return chat(systemPrompt, userText, true);
    }

    private String chat(String systemPrompt, String userText, boolean webSearch) {
        if (!isConfigured()) {
            throw new IllegalStateException("No API key set. Set GROQ_API_KEY, or put your key in ~/.groq_key.");
        }
        if (!webSearch) {
            return doChat(systemPrompt, userText, model, false);
        }

        try {
            return doChat(systemPrompt, userText, searchModel, true);
        } catch (ApiException e) {
            if (e.status == 429) {
                sleepQuietly(3000);
                try {
                    return doChat(systemPrompt, userText, searchModel, true);
                } catch (ApiException ignored) {
                }
            }
            if (!searchModel.equals(model)) {
                try {
                    return doChat(systemPrompt, userText, model, false);
                } catch (RuntimeException ignored) {
                }
            }
            throw e;
        } catch (RuntimeException e) {
            if (!searchModel.equals(model)) {
                try {
                    return doChat(systemPrompt, userText, model, false);
                } catch (RuntimeException ignored) {
                }
            }
            throw e;
        }
    }

    private String doChat(String systemPrompt, String userText, String chosenModel, boolean webSearch) {
        Document payload = new Document()
                .append("model", chosenModel)
                .append("stream", false)
                .append("messages", List.of(
                        new Document("role", "system").append("content", systemPrompt),
                        new Document("role", "user").append("content", userText)
                ));

        if (webSearch && chosenModel.startsWith("openai/")) {
            payload.append("tools", List.of(new Document("type", "browser_search")));
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(150))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toJson()))
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                throw new ApiException(response.statusCode(), provider + " API error "
                        + response.statusCode() + ": " + truncate(response.body(), 300));
            }

            Document root = Document.parse(response.body());
            Document message = root.getList("choices", Document.class)
                    .get(0)
                    .get("message", Document.class);
            return extractContent(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request interrupted.", e);
        } catch (java.net.ConnectException e) {
            throw new RuntimeException("Could not reach the " + provider + " API. Check your internet connection.", e);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(provider + " request failed: " + e.getMessage(), e);
        }
    }

    private String extractContent(Document message) {
        Object content = message.get("content");
        String text;
        if (content instanceof String) {
            text = (String) content;
        } else if (content instanceof List<?> parts) {
            StringBuilder sb = new StringBuilder();
            for (Object part : parts) {
                if (part instanceof Document d) {
                    Object t = d.get("text");
                    if (t instanceof String) sb.append((String) t);
                }
            }
            text = sb.toString();
        } else {
            text = "";
        }
        return CITATION_MARKERS.matcher(text).replaceAll("").trim();
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s == null ? "" : s;
        return s.substring(0, max) + "...";
    }

    private static class ApiException extends RuntimeException {
        final int status;

        ApiException(int status, String message) {
            super(message);
            this.status = status;
        }
    }
}
