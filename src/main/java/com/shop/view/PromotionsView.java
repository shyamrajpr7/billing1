package com.shop.view;

import com.shop.dao.PromotionDAO;
import com.shop.model.Promotion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.time.LocalDate;

public class PromotionsView {
    private final PromotionDAO promotionDAO = new PromotionDAO();
    private final ObservableList<Promotion> promotionList = FXCollections.observableArrayList();
    private final TableView<Promotion> table = new TableView<>();
    private final Label statsLabel = new Label();

    private final TextField nameField = new TextField();
    private final TextField codeField = new TextField();
    private final TextField percentField = new TextField();
    private final TextField minPurchaseField = new TextField("0");
    private final DatePicker startPicker = new DatePicker(LocalDate.now());
    private final DatePicker endPicker = new DatePicker(LocalDate.now().plusDays(30));
    private final TextField categoryField = new TextField();
    private final TextField usageLimitField = new TextField("0");

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label title = new Label("🎯 Promotions & Campaigns");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Create discount campaigns with promo codes for your store.");
        subtitle.getStyleClass().add("sub-label");
        statsLabel.getStyleClass().add("sub-label");
        headerCard.getChildren().addAll(title, subtitle, statsLabel);

        VBox newCard = new VBox(12);
        newCard.getStyleClass().add("card");
        Label newTitle = new Label("➕ New Promotion");
        newTitle.getStyleClass().add("section-title");

        nameField.setPromptText("Campaign name (e.g. Independence Sale)");
        nameField.setPrefWidth(220);

        codeField.setPromptText("Promo code (auto if empty)");
        codeField.setPrefWidth(160);

        percentField.setPromptText("Discount %");
        percentField.setPrefWidth(90);

        minPurchaseField.setPromptText("Min purchase (₹)");
        minPurchaseField.setPrefWidth(100);

        usageLimitField.setPromptText("Usage limit (0 = unlimited)");
        usageLimitField.setPrefWidth(140);

        HBox row1 = new HBox(10, nameField, codeField, percentField);
        row1.setAlignment(Pos.CENTER_LEFT);

        categoryField.setPromptText("Applicable category (optional)");
        categoryField.setPrefWidth(180);

        Button createBtn = new Button("🎯  Create Promotion");
        createBtn.getStyleClass().add("btn-primary");
        createBtn.setOnAction(e -> createPromotion());

        HBox row2 = new HBox(10, startPicker, endPicker, categoryField, minPurchaseField, usageLimitField, createBtn);
        row2.setAlignment(Pos.CENTER_LEFT);

        newCard.getChildren().addAll(newTitle, row1, row2);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Promotion, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("code"));
        codeCol.setCellFactory(col -> new TableCell<Promotion, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                if (!empty && item != null) getStyleClass().add("cell-accent");
            }
        });

        TableColumn<Promotion, String> nameCol = new TableColumn<>("Campaign");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Promotion, String> discountCol = new TableColumn<>("Discount");
        discountCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getDiscountPercent() + "%"));

        TableColumn<Promotion, String> minCol = new TableColumn<>("Min Purchase");
        minCol.setCellValueFactory(p -> new SimpleStringProperty(
                p.getValue().getMinPurchase() > 0 ? String.format("₹%.2f", p.getValue().getMinPurchase()) : "—"));

        TableColumn<Promotion, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getApplicableCategory() != null
                && !p.getValue().getApplicableCategory().isEmpty()
                ? p.getValue().getApplicableCategory() : "All"));

        TableColumn<Promotion, String> datesCol = new TableColumn<>("Valid Period");
        datesCol.setCellValueFactory(new PropertyValueFactory<>("dateRangeLabel"));

        TableColumn<Promotion, String> usageCol = new TableColumn<>("Usage");
        usageCol.setCellValueFactory(p -> new SimpleStringProperty(
                p.getValue().getUsageLimit() > 0
                        ? p.getValue().getUsageCount() + " / " + p.getValue().getUsageLimit()
                        : String.valueOf(p.getValue().getUsageCount())));

        TableColumn<Promotion, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().isValidNow(0) ? "ACTIVE" : "INACTIVE"));
        statusCol.setCellFactory(col -> new TableCell<Promotion, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("ACTIVE".equals(item) ? "badge-active" : "badge-inactive");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Promotion, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(160);
        actionCol.setCellFactory(col -> new TableCell<Promotion, Void>() {
            private final Button toggleBtn = new Button("⏸ Toggle");
            private final Button deleteBtn = new Button("🗑️");
            private final HBox box = new HBox(6, toggleBtn, deleteBtn);
            {
                toggleBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                deleteBtn.getStyleClass().addAll("btn-danger", "btn-small");
                toggleBtn.setOnAction(e -> {
                    promotionDAO.toggleActive(getTableRow().getItem().getId());
                    refresh();
                });
                deleteBtn.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Delete promotion " + getTableRow().getItem().getCode() + "?",
                            ButtonType.YES, ButtonType.NO);
                    confirm.showAndWait().ifPresent(resp -> {
                        if (resp == ButtonType.YES) {
                            promotionDAO.delete(getTableRow().getItem().getId());
                            refresh();
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    toggleBtn.setText(getTableRow().getItem().isActive() ? "⏸ Pause" : "▶ Resume");
                    setGraphic(box);
                }
            }
        });

        table.getColumns().addAll(codeCol, nameCol, discountCol, minCol, categoryCol, datesCol, usageCol, statusCol, actionCol);
        table.setItems(promotionList);
        table.setPlaceholder(new Label("No promotions yet."));

        root.getChildren().addAll(headerCard, newCard, table);
        refresh();
        return root;
    }

    private void createPromotion() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showAlert("Please enter a campaign name.");
            return;
        }
        double percent;
        try {
            percent = Double.parseDouble(percentField.getText().trim());
        } catch (NumberFormatException ex) {
            showAlert("Enter a valid discount percentage.");
            return;
        }
        if (percent <= 0 || percent > 100) {
            showAlert("Discount must be between 0 and 100.");
            return;
        }
        double minPurchase;
        try {
            minPurchase = Double.parseDouble(minPurchaseField.getText().trim());
        } catch (NumberFormatException ex) {
            minPurchase = 0;
        }
        int usageLimit;
        try {
            usageLimit = Integer.parseInt(usageLimitField.getText().trim());
        } catch (NumberFormatException ex) {
            usageLimit = 0;
        }
        Promotion p = new Promotion();
        p.setName(name);
        p.setCode(codeField.getText().trim().toUpperCase());
        p.setDescription("Promo campaign " + name);
        p.setDiscountPercent(percent);
        p.setMinPurchase(minPurchase);
        p.setStartDate(startPicker.getValue());
        p.setEndDate(endPicker.getValue());
        p.setApplicableCategory(categoryField.getText().trim());
        p.setUsageLimit(usageLimit);
        if (promotionDAO.create(p)) {
            showAlert("Promotion created!\n\nCode: " + p.getCode()
                    + "\nDiscount: " + p.getDiscountPercent() + "%"
                    + "\nValid: " + p.getDateRangeLabel());
            nameField.clear();
            codeField.clear();
            percentField.clear();
            categoryField.clear();
            minPurchaseField.setText("0");
            usageLimitField.setText("0");
            refresh();
        } else {
            showAlert("Failed to create promotion. Code may already exist.");
        }
    }

    private void refresh() {
        promotionList.clear();
        promotionList.addAll(promotionDAO.findAll());
        statsLabel.setText("Active Campaigns: " + promotionDAO.countActive()
                + "  •  Total Promotions: " + promotionDAO.count());
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Promotions");
        alert.showAndWait();
    }
}
