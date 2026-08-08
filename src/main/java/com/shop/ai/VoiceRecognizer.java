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

    private Model model;
    private volatile boolean running;
    private Thread listenerThread;
    private String lastError;
    private final Consumer<String> onResult;
    private final Consumer<String> onError;

    public VoiceRecognizer(String modelPath, Consumer<String> onResult, Consumer<String> onError) {
        this.onResult = onResult;
        this.onError = onError;
        try {
            if (modelPath == null || !new File(modelPath).exists()) {
                lastError = "Speech model not found at \"" + modelPath + "\". Voice mode disabled.";
                return;
            }
            model = new Model(modelPath);
        } catch (Exception e) {
            lastError = "Could not load speech model: " + e.getMessage();
        }
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
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }

    private void listen() {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                notifyError("Microphone is not supported or access is denied.");
                running = false;
                return;
            }

            try (TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
                 Recognizer recognizer = new Recognizer(model, SAMPLE_RATE)) {
                line.open(format);
                line.start();

                byte[] buffer = new byte[4096];
                while (running) {
                    int bytesRead = line.read(buffer, 0, buffer.length);
                    if (bytesRead > 0 && recognizer.acceptWaveForm(buffer, bytesRead)) {
                        String result = recognizer.getResult();
                        String text = extractText(result);
                        if (text != null && !text.isBlank()) {
                            onResult.accept(text);
                        }
                    }
                }
                line.stop();
            } catch (Exception e) {
                notifyError("Microphone error: " + e.getMessage());
            }
        } catch (Exception e) {
            notifyError("Could not open microphone: " + e.getMessage());
        } finally {
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

    public void close() {
        stop();
        if (model != null) {
            model.close();
            model = null;
        }
    }
}
