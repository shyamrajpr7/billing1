package com.shop.view;

import com.shop.dao.AnnouncementDAO;
import com.shop.model.Announcement;
import com.shop.model.User;
import com.shop.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;

public class AnnouncementsView {
    private final AnnouncementDAO announcementDAO = new AnnouncementDAO();
    private final ObservableList<Announcement> announcementList = FXCollections.observableArrayList();
    private final Label statsLabel = new Label();
    private final VBox noticeBoard = new VBox(12);

    private final TextField titleField = new TextField();
    private final TextArea messageArea = new TextArea();
    private final ComboBox<String> priorityCombo = new ComboBox<>();
    private final DatePicker expiresPicker = new DatePicker();

    public Node getView() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label title = new Label("📢 Announcements & Notice Board");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Post messages for your team on the notice board.");
        subtitle.getStyleClass().add("sub-label");
        statsLabel.getStyleClass().add("sub-label");
        headerCard.getChildren().addAll(title, subtitle, statsLabel);

        VBox postCard = new VBox(12);
        postCard.getStyleClass().add("card");
        Label postTitle = new Label("📝 Post a New Announcement");
        postTitle.getStyleClass().add("section-title");

        titleField.setPromptText("Title");
        titleField.setMaxWidth(Double.MAX_VALUE);

        messageArea.setPromptText("Message...");
        messageArea.setPrefRowCount(3);
        messageArea.setPrefHeight(80);

        priorityCombo.getItems().addAll(
                Announcement.PRIORITY_LOW, Announcement.PRIORITY_NORMAL, Announcement.PRIORITY_HIGH);
        priorityCombo.setValue(Announcement.PRIORITY_NORMAL);

        expiresPicker.setPromptText("Expires on (optional)");

        Button postBtn = new Button("📢  Post Announcement");
        postBtn.getStyleClass().add("btn-primary");
        postBtn.setOnAction(e -> post());

        HBox optionsRow = new HBox(12, priorityCombo, expiresPicker);
        optionsRow.setAlignment(Pos.CENTER_LEFT);
        optionsRow.getChildren().add(postBtn);

        postCard.getChildren().addAll(postTitle, titleField, messageArea, optionsRow);

        Label boardTitle = new Label("📋 Notice Board");
        boardTitle.getStyleClass().add("section-title");

        noticeBoard.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(noticeBoard, Priority.ALWAYS);

        root.getChildren().addAll(headerCard, postCard, boardTitle, noticeBoard);
        scrollPane.setContent(root);
        refresh();
        return scrollPane;
    }

    private void post() {
        String title = titleField.getText().trim();
        String message = messageArea.getText().trim();
        if (title.isEmpty() || message.isEmpty()) {
            showAlert("Please enter both a title and a message.");
            return;
        }
        Announcement a = new Announcement();
        a.setTitle(title);
        a.setMessage(message);
        a.setPriority(priorityCombo.getValue());
        a.setExpiresAt(expiresPicker.getValue());
        a.setPinned(false);
        User u = SessionManager.getInstance().getCurrentUser();
        if (u != null) {
            a.setCreatedBy(u.getFullName());
            a.setCreatedById(u.getId());
        }
        if (announcementDAO.create(a)) {
            titleField.clear();
            messageArea.clear();
            expiresPicker.setValue(null);
            priorityCombo.setValue(Announcement.PRIORITY_NORMAL);
            refresh();
        } else {
            showAlert("Failed to post announcement.");
        }
    }

    private void refresh() {
        announcementList.clear();
        announcementList.addAll(announcementDAO.findAll());
        announcementList.sort((a, b) -> {
            if (a.isPinned() != b.isPinned()) return a.isPinned() ? -1 : 1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        noticeBoard.getChildren().clear();
        for (Announcement a : announcementList) {
            noticeBoard.getChildren().add(buildCard(a));
        }
        statsLabel.setText("Active Announcements: " + announcementDAO.countActive()
                + "  •  Total: " + announcementList.size());
        if (announcementList.isEmpty()) {
            Label empty = new Label("No announcements yet. Post the first one! 🎉");
            empty.getStyleClass().add("sub-label");
            noticeBoard.getChildren().add(empty);
        }
    }

    private Node buildCard(Announcement a) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setMaxWidth(Double.MAX_VALUE);

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label priorityBadge = new Label(a.getPriority());
        priorityBadge.getStyleClass().add(Announcement.PRIORITY_HIGH.equals(a.getPriority())
                ? "badge-inactive" : Announcement.PRIORITY_NORMAL.equals(a.getPriority())
                ? "badge-warning" : "badge-active");

        Label titleLabel = new Label((a.isPinned() ? "📌 " : "") + a.getTitle());
        titleLabel.getStyleClass().add("section-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button pinBtn = new Button(a.isPinned() ? "📌 Unpin" : "📌 Pin");
        pinBtn.getStyleClass().addAll("btn-secondary", "btn-small");
        pinBtn.setOnAction(e -> {
            announcementDAO.togglePin(a.getId());
            refresh();
        });

        Button deleteBtn = new Button("🗑️");
        deleteBtn.getStyleClass().addAll("btn-danger", "btn-small");
        deleteBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete this announcement?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(resp -> {
                if (resp == ButtonType.YES) {
                    announcementDAO.delete(a.getId());
                    refresh();
                }
            });
        });

        topRow.getChildren().addAll(priorityBadge, titleLabel, spacer, pinBtn, deleteBtn);

        Label msgLabel = new Label(a.getMessage());
        msgLabel.setWrapText(true);

        Label metaLabel = new Label("By " + (a.getCreatedBy() != null && !a.getCreatedBy().isEmpty()
                ? a.getCreatedBy() : "Unknown")
                + "  •  " + a.getCreatedAtLabel()
                + (a.getExpiresAt() != null ? "  •  Expires: " + a.getExpiresAt() : ""));
        metaLabel.getStyleClass().add("sub-label");

        card.getChildren().addAll(topRow, msgLabel, metaLabel);
        return card;
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Announcements");
        alert.showAndWait();
    }
}
