package com.shop.ai;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Floating assistant panel: transcript, typed input and a microphone toggle.
 * Always visible on the right side of the dashboard.
 */
public class AssistantPanel {

    private final CommandAssistant assistant = CommandAssistant.getInstance();

    private final VBox root = new VBox(10);
    private final TextArea transcript = new TextArea();
    private final TextField input = new TextField();
    private final ToggleButton micButton = new ToggleButton("🎤 Voice");
    private final Label statusLabel = new Label("Voice: off");

    private VoiceRecognizer voice;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    public AssistantPanel() {
        buildUi();
        initializeVoice();
    }

    private void buildUi() {
        root.getStyleClass().add("assistant-panel");
        root.setPrefWidth(300);
        root.setMinWidth(280);
        root.setMaxWidth(340);
        root.setPadding(new Insets(12));

        Label title = new Label("🤖 AI Assistant");
        title.getStyleClass().add("assistant-title");

        Label subtitle = new Label("Speak or type. Say \"help\" for commands.");
        subtitle.getStyleClass().add("sub-label");

        transcript.setEditable(false);
        transcript.setWrapText(true);
        transcript.setPromptText("Assistant transcript...");
        VBox.setVgrow(transcript, Priority.ALWAYS);

        HBox inputBar = new HBox(8);
        inputBar.setAlignment(Pos.CENTER_LEFT);
        input.setPromptText("Type a command...");
        input.setStyle("-fx-prompt-text-fill: #999999;");
        HBox.setHgrow(input, Priority.ALWAYS);
        input.setOnAction(e -> sendTyped());

        Button sendBtn = new Button("Send");
        sendBtn.getStyleClass().add("btn-primary");
        sendBtn.setOnAction(e -> sendTyped());

        inputBar.getChildren().addAll(input, sendBtn);

        HBox voiceBar = new HBox(8);
        voiceBar.setAlignment(Pos.CENTER_LEFT);
        micButton.getStyleClass().addAll("btn-secondary");
        micButton.setOnAction(e -> toggleVoice());
        statusLabel.getStyleClass().add("sub-label");
        HBox.setHgrow(statusLabel, Priority.ALWAYS);
        voiceBar.getChildren().addAll(micButton, statusLabel);

        append("assistant", "Hi! I'm your shop assistant. Click 🎤 Voice to speak, or type below. Say \"help\" to see commands.");

        root.getChildren().addAll(title, subtitle, transcript, inputBar, voiceBar);
    }

    private void initializeVoice() {
        String modelPath = System.getProperty("vosk.model", "models/vosk-model-small-en-us-0.15");
        voice = new VoiceRecognizer(modelPath, text -> {
            Platform.runLater(() -> {
                append("you", text);
                execute(text);
            });
        }, error -> Platform.runLater(() -> statusLabel.setText("Voice error: " + error)));

        if (!voice.isAvailable()) {
            statusLabel.setText("Voice off: " + voice.getLastError());
            micButton.setDisable(true);
        }
    }

    private void sendTyped() {
        String text = input.getText().trim();
        if (text.isEmpty()) return;
        input.clear();
        append("you", text);
        execute(text);
    }

    private void execute(String text) {
        assistant.processAsync(text, reply -> append("assistant", reply));
    }

    private void toggleVoice() {
        if (micButton.isSelected()) {
            if (!voice.isAvailable()) {
                micButton.setSelected(false);
                return;
            }
            voice.start();
            statusLabel.setText("Voice: listening...");
            micButton.setText("🛑 Stop");
            append("assistant", "Listening... speak now.");
        } else {
            voice.stop();
            statusLabel.setText("Voice: off");
            micButton.setText("🎤 Voice");
            append("assistant", "Voice stopped.");
        }
    }

    private void append(String speaker, String text) {
        String stamp = LocalTime.now().format(TIME);
        String line = (speaker.equals("you") ? "You" : "Assistant") + " (" + stamp + "): " + text;
        transcript.appendText(line + "\n");
        transcript.positionCaret(transcript.getLength());
    }

    public Node getView() {
        return root;
    }
}
