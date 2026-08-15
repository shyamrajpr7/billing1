package com.shop.view;

import com.shop.dao.CustomerDAO;
import com.shop.dao.FeedbackDAO;
import com.shop.model.Customer;
import com.shop.model.Feedback;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

public class FeedbackView {
    private final FeedbackDAO feedbackDAO = new FeedbackDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final ObservableList<Feedback> feedbackList = FXCollections.observableArrayList();
    private final TableView<Feedback> table = new TableView<>();
    private final Label statsLabel = new Label();

    private final ComboBox<Customer> customerCombo = new ComboBox<>();
    private final ComboBox<String> ratingCombo = new ComboBox<>();
    private final ComboBox<String> categoryCombo = new ComboBox<>();
    private final TextArea commentArea = new TextArea();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label title = new Label("💬 Customer Feedback & Reviews");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Collect ratings and reviews to understand customer satisfaction.");
        subtitle.getStyleClass().add("sub-label");
        statsLabel.getStyleClass().add("sub-label");
        headerCard.getChildren().addAll(title, subtitle, statsLabel);

        VBox addCard = new VBox(12);
        addCard.getStyleClass().add("card");
        Label addTitle = new Label("➕ Log Feedback");
        addTitle.getStyleClass().add("section-title");

        customerCombo.setPromptText("Customer (optional)");
        customerCombo.setPrefWidth(240);
        customerCombo.getItems().addAll(customerDAO.findAll());

        ratingCombo.getItems().addAll("5", "4", "3", "2", "1");
        ratingCombo.setValue("5");
        ratingCombo.setPrefWidth(80);

        categoryCombo.getItems().addAll(
                Feedback.CATEGORY_PRODUCT, Feedback.CATEGORY_SERVICE,
                Feedback.CATEGORY_STORE, Feedback.CATEGORY_OTHER);
        categoryCombo.setValue(Feedback.CATEGORY_PRODUCT);
        categoryCombo.setPrefWidth(110);

        Button addBtn = new Button("💬  Save Feedback");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> addFeedback());

        HBox row1 = new HBox(10, customerCombo, ratingCombo, categoryCombo, addBtn);
        row1.setAlignment(Pos.CENTER_LEFT);

        commentArea.setPromptText("Comments / review...");
        commentArea.setPrefRowCount(2);
        commentArea.setPrefHeight(55);

        addCard.getChildren().addAll(addTitle, row1, commentArea);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Feedback, String> custCol = new TableColumn<>("Customer");
        custCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getCustomerName() != null
                ? p.getValue().getCustomerName() : "—"));

        TableColumn<Feedback, String> ratingCol = new TableColumn<>("Rating");
        ratingCol.setCellValueFactory(new PropertyValueFactory<>("ratingLabel"));
        ratingCol.setCellFactory(col -> new TableCell<Feedback, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                if (!empty && item != null) getStyleClass().add("cell-accent");
            }
        });

        TableColumn<Feedback, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<Feedback, String> commentCol = new TableColumn<>("Comment");
        commentCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getComment() != null
                ? p.getValue().getComment() : ""));

        TableColumn<Feedback, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("createdAtLabel"));

        TableColumn<Feedback, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setMaxWidth(70);
        actionCol.setCellFactory(col -> new TableCell<Feedback, Void>() {
            private final Button delBtn = new Button("🗑️");
            {
                delBtn.getStyleClass().addAll("btn-danger", "btn-small");
                delBtn.setOnAction(e -> {
                    Feedback f = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Delete this feedback entry?", ButtonType.YES, ButtonType.NO);
                    confirm.showAndWait().ifPresent(resp -> {
                        if (resp == ButtonType.YES) {
                            feedbackDAO.delete(f.getId());
                            refresh();
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : delBtn);
            }
        });

        table.getColumns().addAll(custCol, ratingCol, categoryCol, commentCol, dateCol, actionCol);
        table.setItems(feedbackList);
        table.setPlaceholder(new Label("No feedback collected yet."));

        root.getChildren().addAll(headerCard, addCard, table);
        refresh();
        return root;
    }

    private void addFeedback() {
        Feedback f = new Feedback();
        Customer c = customerCombo.getValue();
        if (c != null) {
            f.setCustomerId(c.getId());
            f.setCustomerName(c.getName());
            f.setCustomerPhone(c.getPhone());
        } else {
            f.setCustomerName("Anonymous");
        }
        f.setRating(Integer.parseInt(ratingCombo.getValue()));
        f.setCategory(categoryCombo.getValue());
        f.setComment(commentArea.getText().trim());
        if (feedbackDAO.create(f)) {
            showAlert("Thank you! Feedback saved.\n\nRating: " + f.getRatingLabel()
                    + "  (" + f.getRating() + "/5)\nCategory: " + f.getCategory());
            customerCombo.setValue(null);
            commentArea.clear();
            ratingCombo.setValue("5");
            categoryCombo.setValue(Feedback.CATEGORY_PRODUCT);
            refresh();
        } else {
            showAlert("Failed to save feedback.");
        }
    }

    private void refresh() {
        feedbackList.clear();
        feedbackList.addAll(feedbackDAO.findAll());
        statsLabel.setText("Average Rating: " + String.format("%.2f", feedbackDAO.averageRating())
                + " ⭐  •  Positive (4-5): " + feedbackDAO.countPositive()
                + "  •  Negative (1-2): " + feedbackDAO.countNegative()
                + "  •  Total: " + feedbackDAO.count());
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Feedback");
        alert.showAndWait();
    }
}
