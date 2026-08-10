package com.shop.ai;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * AI assistant as a floating action button (FAB) docked to the bottom-right
 * corner of the dashboard. Clicking the FAB opens the chat panel; closing it
 * returns to the floating button.
 */
public class AssistantPanel {

    private final CommandAssistant assistant = CommandAssistant.getInstance();

    private final StackPane overlay = new StackPane();
    private final Button fabButton = new Button("🤖");
    private final VBox root = new VBox(10);
    private final VBox messagesBox = new VBox(10);
    private final ScrollPane scrollPane = new ScrollPane(messagesBox);
    private final TextField input = new TextField();
    private final ToggleButton micButton = new ToggleButton("🎤 Voice");
    private final Label statusLabel = new Label("Voice: off");
    private final Label partialLabel = new Label();

    private VoiceRecognizer voice;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    public AssistantPanel() {
        buildUi();
        buildFloating();
        initializeVoice();
    }

    private void buildFloating() {
        fabButton.getStyleClass().add("ai-fab");
        fabButton.setTooltip(new Tooltip("🤖 Open AI Assistant — ask questions, manage inventory, checkout by voice"));
        fabButton.setOnAction(e -> openPanel());
        StackPane.setAlignment(fabButton, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(fabButton, new Insets(0, 18, 18, 0));

        StackPane.setAlignment(root, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(root, new Insets(0, 12, 12, 0));
        root.setMaxHeight(560);
        root.setVisible(false);

        overlay.getChildren().addAll(fabButton, root);
        overlay.setPickOnBounds(false);
    }

    private void openPanel() {
        root.setVisible(true);
        fabButton.setVisible(false);
    }

    private void closePanel() {
        root.setVisible(false);
        fabButton.setVisible(true);
    }

    private void buildUi() {
        root.getStyleClass().add("assistant-panel");
        root.setPrefWidth(320);
        root.setMinWidth(280);
        root.setMaxWidth(360);
        root.setPadding(new Insets(12));

        Label title = new Label("🤖 AI Assistant");
        title.getStyleClass().add("assistant-title");

        Button clearBtn = new Button("Clear");
        clearBtn.getStyleClass().addAll("btn-secondary", "btn-small");
        clearBtn.setOnAction(e -> messagesBox.getChildren().removeIf(n -> n != partialLabel));

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().addAll("btn-secondary", "btn-small", "ai-close-btn");
        closeBtn.setTooltip(new Tooltip("Close assistant"));
        closeBtn.setOnAction(e -> closePanel());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox headerRow = new HBox(8, title, spacer, clearBtn, closeBtn);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label subtitle = new Label("Speak or type. Say \"help\" for commands.");
        subtitle.getStyleClass().add("sub-label");

        messagesBox.getStyleClass().add("chat-messages");
        messagesBox.setPadding(new Insets(6));

        scrollPane.getStyleClass().add("chat-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        partialLabel.getStyleClass().add("voice-partial");
        partialLabel.setWrapText(true);
        partialLabel.setVisible(false);
        messagesBox.getChildren().add(partialLabel);

        HBox inputBar = new HBox(8);
        inputBar.setAlignment(Pos.CENTER_LEFT);
        input.setPromptText("Type a message...");
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

        appendMessage("assistant", "Hi! I'm your shop assistant. Click 🎤 Voice to speak, or type below. Say \"help\" to see commands.");

        root.getChildren().addAll(headerRow, subtitle, scrollPane, inputBar, voiceBar);
    }

    private void initializeVoice() {
        String modelPath = System.getProperty("vosk.model", "models/vosk-model-small-en-us-0.15");
        voice = new VoiceRecognizer(modelPath, text -> {
            Platform.runLater(() -> {
                partialLabel.setVisible(false);
                appendMessage("you", text);
                execute(text);
            });
        }, partial -> Platform.runLater(() -> {
            partialLabel.setText("🎤 " + partial + "…");
            partialLabel.setVisible(true);
            scrollToBottom();
        }), error -> Platform.runLater(() -> statusLabel.setText("Voice error: " + error)));

        if (!voice.isAvailable()) {
            statusLabel.setText("Voice off: " + voice.getLastError());
            micButton.setDisable(true);
        }
    }

    private void sendTyped() {
        String text = input.getText().trim();
        if (text.isEmpty()) return;
        input.clear();
        appendMessage("you", text);
        execute(text);
    }

    private void execute(String text) {
        HBox typing = addTypingIndicator();
        assistant.processAsync(text, reply -> {
            removeTypingIndicator(typing);
            appendMessage("assistant", reply);
        });
    }

    private void toggleVoice() {
        if (micButton.isSelected()) {
            if (!voice.isAvailable()) {
                micButton.setSelected(false);
                return;
            }
            partialLabel.setVisible(false);
            voice.start();
            statusLabel.setText("Voice: listening...");
            micButton.setText("🛑 Stop");
            appendMessage("assistant", "Listening... speak now.");
        } else {
            voice.stop();
            statusLabel.setText("Voice: off");
            micButton.setText("🎤 Voice");
            partialLabel.setVisible(false);
        }
    }

    private void appendMessage(String speaker, String text) {
        boolean user = "you".equals(speaker);

        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setMinWidth(0);

        Label time = new Label(LocalTime.now().format(TIME));
        time.getStyleClass().add("chat-time");

        VBox bubble = new VBox(2, msg, time);
        bubble.setMaxWidth(250);
        bubble.setAlignment(user ? Pos.BOTTOM_RIGHT : Pos.BOTTOM_LEFT);
        bubble.getStyleClass().add(user ? "chat-bubble-user" : "chat-bubble-assistant");

        HBox row = new HBox(bubble);
        row.setAlignment(user ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.getStyleClass().add(user ? "chat-row-user" : "chat-row-assistant");

        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private HBox addTypingIndicator() {
        Label msg = new Label("Assistant is typing...");
        msg.setStyle("-fx-font-style: italic;");
        VBox bubble = new VBox(msg);
        bubble.setMaxWidth(250);
        bubble.setAlignment(Pos.BOTTOM_LEFT);
        bubble.getStyleClass().add("chat-bubble-assistant");

        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("chat-row-assistant");

        messagesBox.getChildren().add(row);
        scrollToBottom();
        return row;
    }

    private void removeTypingIndicator(Node typingRow) {
        messagesBox.getChildren().remove(typingRow);
    }

    private void scrollToBottom() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    public Node getView() {
        return overlay;
    }
}
