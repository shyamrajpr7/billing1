package com.shop.view;

import com.shop.dao.CustomerDAO;
import com.shop.dao.GiftCardDAO;
import com.shop.model.Customer;
import com.shop.model.GiftCard;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.Optional;

public class GiftCardsView {
    private final GiftCardDAO giftCardDAO = new GiftCardDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final ObservableList<GiftCard> cardList = FXCollections.observableArrayList();
    private final TableView<GiftCard> table = new TableView<>();
    private final Label statsLabel = new Label();

    private final TextField amountField = new TextField();
    private final ComboBox<Customer> customerCombo = new ComboBox<>();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label title = new Label("🎁 Gift Cards");
        title.getStyleClass().add("section-title");
        statsLabel.getStyleClass().add("sub-label");
        headerCard.getChildren().addAll(title, statsLabel);

        VBox issueCard = new VBox(12);
        issueCard.getStyleClass().add("card");
        Label issueTitle = new Label("➕ Issue a New Gift Card");
        issueTitle.getStyleClass().add("section-title");

        HBox issueRow = new HBox(12);
        issueRow.setAlignment(Pos.CENTER_LEFT);

        amountField.setPromptText("Amount (₹)");
        amountField.setPrefWidth(140);

        customerCombo.setPromptText("Assign to customer (optional)");
        customerCombo.setPrefWidth(280);
        customerCombo.getItems().addAll(customerDAO.findAll());

        Button issueBtn = new Button("🎁  Issue Gift Card");
        issueBtn.getStyleClass().add("btn-primary");
        issueBtn.setOnAction(e -> issueCard());

        issueRow.getChildren().addAll(amountField, customerCombo, issueBtn);
        issueCard.getChildren().addAll(issueTitle, issueRow);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<GiftCard, String> numCol = new TableColumn<>("Card Number");
        numCol.setCellValueFactory(new PropertyValueFactory<>("cardNumber"));
        numCol.setPrefWidth(120);

        TableColumn<GiftCard, String> custCol = new TableColumn<>("Customer");
        custCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getCustomerId() > 0
                ? p.getValue().getCustomerName() : "—"));

        TableColumn<GiftCard, String> issuedCol = new TableColumn<>("Issued Value");
        issuedCol.setCellValueFactory(new PropertyValueFactory<>("formattedInitial"));

        TableColumn<GiftCard, String> balanceCol = new TableColumn<>("Remaining Balance");
        balanceCol.setCellValueFactory(new PropertyValueFactory<>("formattedBalance"));
        balanceCol.setCellFactory(col -> new TableCell<GiftCard, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                if (!empty && item != null) getStyleClass().add("cell-accent");
            }
        });

        TableColumn<GiftCard, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<GiftCard, String>() {
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

        TableColumn<GiftCard, String> dateCol = new TableColumn<>("Issued On");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("formattedDate"));

        TableColumn<GiftCard, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(220);
        actionCol.setCellFactory(col -> new TableCell<GiftCard, Void>() {
            private final Button topUpBtn = new Button("💰 Top Up");
            private final Button deactivateBtn = new Button("🚫 Deactivate");
            private final HBox box = new HBox(6, topUpBtn, deactivateBtn);
            {
                topUpBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                deactivateBtn.getStyleClass().addAll("btn-danger", "btn-small");
                topUpBtn.setOnAction(e -> topUp(getTableRow().getItem()));
                deactivateBtn.setOnAction(e -> deactivate(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    GiftCard gc = getTableRow().getItem();
                    boolean active = gc.isActive();
                    topUpBtn.setDisable(!active);
                    deactivateBtn.setDisable(!active);
                    setGraphic(box);
                }
            }
        });

        table.getColumns().addAll(numCol, custCol, issuedCol, balanceCol, statusCol, dateCol, actionCol);
        table.setItems(cardList);
        table.setPlaceholder(new Label("No gift cards issued yet."));

        root.getChildren().addAll(headerCard, issueCard, table);
        refresh();
        return root;
    }

    private void issueCard() {
        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim());
        } catch (NumberFormatException ex) {
            showAlert("Please enter a valid amount.");
            return;
        }
        if (amount <= 0) {
            showAlert("Amount must be greater than zero.");
            return;
        }
        GiftCard gc = new GiftCard();
        gc.setInitialAmount(Math.round(amount * 100.0) / 100.0);
        gc.setBalance(gc.getInitialAmount());
        Customer c = customerCombo.getValue();
        if (c != null) {
            gc.setCustomerId(c.getId());
            gc.setCustomerName(c.getName());
        }
        if (giftCardDAO.create(gc)) {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Gift Card Issued");
            info.setHeaderText(null);
            info.setContentText("Gift card issued!\n\nCard Number: " + gc.getCardNumber()
                    + "\nValue: ₹" + String.format("%.2f", gc.getInitialAmount())
                    + "\n\nShare this number with the customer to redeem at POS.");
            info.showAndWait();
            amountField.clear();
            customerCombo.setValue(null);
            refresh();
        } else {
            showAlert("Failed to issue gift card. Please try again.");
        }
    }

    private void topUp(GiftCard gc) {
        if (gc == null || !gc.isActive()) return;
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Top Up Gift Card");
        dialog.setHeaderText("Gift Card " + gc.getCardNumber() + " — Current balance: " + gc.getFormattedBalance());
        dialog.setContentText("Add amount (₹):");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            try {
                double amt = Double.parseDouble(input.trim());
                if (amt <= 0) {
                    showAlert("Amount must be greater than zero.");
                    return;
                }
                if (giftCardDAO.topUp(gc.getCardNumber(), amt)) {
                    refresh();
                } else {
                    showAlert("Top up failed.");
                }
            } catch (NumberFormatException ex) {
                showAlert("Please enter a valid amount.");
            }
        });
    }

    private void deactivate(GiftCard gc) {
        if (gc == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Deactivate Gift Card");
        confirm.setHeaderText(null);
        confirm.setContentText("Deactivate gift card " + gc.getCardNumber() + "? It can no longer be redeemed.");
        if (confirm.showAndWait().filter(b -> b == ButtonType.OK).isPresent()) {
            giftCardDAO.deactivate(gc.getCardNumber());
            refresh();
        }
    }

    private void refresh() {
        cardList.clear();
        cardList.addAll(giftCardDAO.findAll());
        statsLabel.setText("Active Cards: " + (int) giftCardDAO.findAll().stream().filter(GiftCard::isActive).count()
                + "  •  Total Cards: " + giftCardDAO.count()
                + "  •  Outstanding Balance: ₹" + String.format("%.2f", giftCardDAO.totalActiveBalance()));
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Gift Cards");
        alert.showAndWait();
    }
}
