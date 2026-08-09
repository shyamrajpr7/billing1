package com.shop.ai;

import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.io.File;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Offline voice recognition powered by Vosk. Captures microphone audio and
 * turns speech into text, delivered through {@link #onResult}.
 */
public class VoiceRecognizer {

    private static final float SAMPLE_RATE = 16000f;
    private static final Pattern TEXT_PATTERN = Pattern.compile("\"text\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern PARTIAL_PATTERN = Pattern.compile("\"partial\"\\s*:\\s*\"([^\"]*)\"");

    private Model model;
    private volatile boolean running;
    private Thread listenerThread;
    private volatile TargetDataLine micLine;
    private String lastError;
    private final Consumer<String> onResult;
    private final Consumer<String> onPartial;
    private final Consumer<String> onError;

    public VoiceRecognizer(String modelPath, Consumer<String> onResult, Consumer<String> onError) {
        this(modelPath, onResult, null, onError);
    }

    public VoiceRecognizer(String modelPath, Consumer<String> onResult, Consumer<String> onPartial, Consumer<String> onError) {
        this.onResult = onResult;
        this.onPartial = onPartial;
        this.onError = onError;
        String resolved = resolveModelPath(modelPath);
        if (resolved == null) {
            lastError = "Speech model not found (looked for \"" + modelPath + "\" and common locations). Voice mode disabled.";
            return;
        }
        try {
            model = new Model(resolved);
        } catch (Exception e) {
            lastError = "Could not load speech model: " + e.getMessage();
        }
    }

    /**
     * Finds the speech model even when the app is launched from another
     * directory: honours the explicit path first, then falls back to the
     * working directory and the application directory.
     */
    private String resolveModelPath(String modelPath) {
        if (modelPath != null && new File(modelPath).exists()) return modelPath;
        String[] candidates = {
                "models/vosk-model-small-en-us-0.15",
                System.getProperty("user.dir") + "/models/vosk-model-small-en-us-0.15",
                new File(".").getAbsoluteFile().getParent() + "/models/vosk-model-small-en-us-0.15"
        };
        for (String candidate : candidates) {
            if (new File(candidate).exists()) return candidate;
        }
        return null;
    }

    public boolean isAvailable() {
        return model != null;
    }

    public String getLastError() {
        return lastError;
    }

    public boolean isListening() {
        return running;
    }

    public void start() {
        if (model == null || running) return;
        running = true;
        listenerThread = new Thread(this::listen, "vosk-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void stop() {
        running = false;
        TargetDataLine line = micLine;
        if (line != null) {
            try {
                line.stop();
                line.close();
            } catch (Exception ignored) {
            }
        }
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }

    private void listen() {
        TargetDataLine line = null;
        Recognizer recognizer = null;
        String lastFinal = null;
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                notifyError("Microphone is not supported or access is denied.");
                running = false;
                return;
            }

            line = (TargetDataLine) AudioSystem.getLine(info);
            micLine = line;
            line.open(format);
            line.start();
            recognizer = new Recognizer(model, SAMPLE_RATE);

            byte[] buffer = new byte[4096];
            while (running) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                if (bytesRead <= 0) continue;
                if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                    String result = recognizer.getResult();
                    String text = extractText(result);
                    if (text != null && !text.isBlank()) {
                        lastFinal = text;
                        onResult.accept(text);
                    }
                } else if (onPartial != null) {
                    String partial = extractPartial(recognizer.getPartialResult());
                    if (partial != null && !partial.isBlank()) {
                        onPartial.accept(partial);
                    }
                }
            }
        } catch (Exception e) {
            if (running) {
                notifyError("Microphone error: " + e.getMessage());
            }
        } finally {
            // Pull whatever was still being spoken so nothing is lost on stop.
            if (recognizer != null) {
                try {
                    String text = extractText(recognizer.getFinalResult());
                    if (text != null && !text.isBlank() && !text.equals(lastFinal)) {
                        onResult.accept(text);
                    }
                } catch (Exception ignored) {
                }
                try {
                    recognizer.close();
                } catch (Exception ignored) {
                }
            }
            if (line != null) {
                try {
                    line.stop();
                    line.close();
                } catch (Exception ignored) {
                }
            }
            micLine = null;
            running = false;
        }
    }

    private void notifyError(String message) {
        if (onError != null) onError.accept(message);
    }

    private String extractText(String json) {
        Matcher m = TEXT_PATTERN.matcher(json);
        if (m.find()) {
            String text = m.group(1).trim();
            return text.isEmpty() ? null : text;
        }
        return null;
    }

    private String extractPartial(String json) {
        Matcher m = PARTIAL_PATTERN.matcher(json);
        if (m.find()) {
            String text = m.group(1).trim();
            return text.isEmpty() ? null : text;
        }
        return null;
    }

    public void close() {
        stop();
        if (model != null) {
            model.close();
            model = null;
        }
    }
}
