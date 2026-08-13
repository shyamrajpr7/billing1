package com.shop.view;

import com.shop.dao.CustomerDAO;
import com.shop.dao.ProductDAO;
import com.shop.dao.WarrantyDAO;
import com.shop.model.Customer;
import com.shop.model.Product;
import com.shop.model.Warranty;
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
import java.util.List;

public class WarrantiesView {
    private final WarrantyDAO warrantyDAO = new WarrantyDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final ObservableList<Warranty> warrantyList = FXCollections.observableArrayList();
    private final TableView<Warranty> table = new TableView<>();
    private final Label statsLabel = new Label();

    private final ComboBox<Product> productCombo = new ComboBox<>();
    private final ComboBox<Customer> customerCombo = new ComboBox<>();
    private final DatePicker purchasePicker = new DatePicker(LocalDate.now());
    private final Spinner<Integer> monthsSpinner = new Spinner<>(1, 60, 12);
    private final TextField invoiceField = new TextField();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label title = new Label("🛡️ Warranty Management");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Track product warranties issued to customers and expiring claims.");
        subtitle.getStyleClass().add("sub-label");
        statsLabel.getStyleClass().add("sub-label");
        headerCard.getChildren().addAll(title, subtitle, statsLabel);

        VBox registerCard = new VBox(12);
        registerCard.getStyleClass().add("card");
        Label registerTitle = new Label("➕ Register a Warranty");
        registerTitle.getStyleClass().add("section-title");

        productCombo.setPromptText("Product");
        productCombo.setPrefWidth(240);
        productCombo.getItems().addAll(productDAO.findAll());

        customerCombo.setPromptText("Customer (optional)");
        customerCombo.setPrefWidth(220);
        customerCombo.getItems().addAll(customerDAO.findAll());

        monthsSpinner.setPrefWidth(90);

        invoiceField.setPromptText("Invoice no. (optional)");
        invoiceField.setPrefWidth(140);

        Button registerBtn = new Button("🛡️  Register");
        registerBtn.getStyleClass().add("btn-primary");
        registerBtn.setOnAction(e -> registerWarranty());

        HBox formRow = new HBox(10);
        formRow.setAlignment(Pos.CENTER_LEFT);
        formRow.getChildren().addAll(productCombo, customerCombo, purchasePicker, monthsSpinner, invoiceField, registerBtn);

        HBox hintRow = new HBox(10);
        hintRow.setAlignment(Pos.CENTER_LEFT);
        Label hint = new Label("Warranty duration in months • Expiry auto-computed from purchase date");
        hint.getStyleClass().add("sub-label");
        hintRow.getChildren().add(hint);

        registerCard.getChildren().addAll(registerTitle, formRow, hintRow);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Warranty, String> numCol = new TableColumn<>("Warranty No.");
        numCol.setCellValueFactory(new PropertyValueFactory<>("warrantyNumber"));

        TableColumn<Warranty, String> productCol = new TableColumn<>("Product");
        productCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getProductName() != null
                ? p.getValue().getProductName() : "—"));

        TableColumn<Warranty, String> custCol = new TableColumn<>("Customer");
        custCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getCustomerName() != null
                ? p.getValue().getCustomerName() : "—"));

        TableColumn<Warranty, String> invoiceCol = new TableColumn<>("Invoice");
        invoiceCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getPurchaseInvoiceNumber() != null
                && !p.getValue().getPurchaseInvoiceNumber().isEmpty()
                ? p.getValue().getPurchaseInvoiceNumber() : "—"));

        TableColumn<Warranty, String> expiryCol = new TableColumn<>("Expires On");
        expiryCol.setCellValueFactory(new PropertyValueFactory<>("expiryDateLabel"));

        TableColumn<Warranty, String> daysCol = new TableColumn<>("Days Left");
        daysCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().isActive()
                ? String.valueOf(p.getValue().getDaysRemaining()) : "—"));

        TableColumn<Warranty, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<Warranty, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add(Warranty.STATUS_ACTIVE.equals(item) ? "badge-active" : "badge-inactive");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Warranty, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(120);
        actionCol.setCellFactory(col -> new TableCell<Warranty, Void>() {
            private final Button claimBtn = new Button("🔧 Claim");
            {
                claimBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                claimBtn.setOnAction(e -> markClaimed(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    claimBtn.setDisable(!getTableRow().getItem().isActive());
                    setGraphic(claimBtn);
                }
            }
        });

        table.getColumns().addAll(numCol, productCol, custCol, invoiceCol, expiryCol, daysCol, statusCol, actionCol);
        table.setItems(warrantyList);
        table.setPlaceholder(new Label("No warranties registered yet."));

        root.getChildren().addAll(headerCard, registerCard, table);
        refresh();
        return root;
    }

    private void registerWarranty() {
        Product p = productCombo.getValue();
        if (p == null) {
            showAlert("Please select a product.");
            return;
        }
        Warranty w = new Warranty();
        w.setProductId(p.getId());
        w.setProductName(p.getName());
        w.setBarcode(p.getBarcode());
        Customer c = customerCombo.getValue();
        if (c != null) {
            w.setCustomerId(c.getId());
            w.setCustomerName(c.getName());
            w.setCustomerPhone(c.getPhone());
        }
        w.setPurchaseDate(purchasePicker.getValue());
        w.setWarrantyMonths(monthsSpinner.getValue());
        w.setPurchaseInvoiceNumber(invoiceField.getText().trim());
        if (warrantyDAO.create(w)) {
            showAlert("Warranty registered!\n\nWarranty No: " + w.getWarrantyNumber()
                    + "\nProduct: " + w.getProductName()
                    + "\nExpires: " + w.getExpiryDateLabel());
            productCombo.setValue(null);
            customerCombo.setValue(null);
            invoiceField.clear();
            purchasePicker.setValue(LocalDate.now());
            refresh();
        } else {
            showAlert("Failed to register warranty.");
        }
    }

    private void markClaimed(Warranty w) {
        if (w == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Mark warranty " + w.getWarrantyNumber() + " as claimed?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                warrantyDAO.markClaimed(w.getId());
                refresh();
            }
        });
    }

    private void refresh() {
        warrantyList.clear();
        warrantyList.addAll(warrantyDAO.findAll());
        statsLabel.setText("Active Warranties: " + warrantyDAO.countActive()
                + "  •  Expiring in 30 days: " + warrantyDAO.countExpiringSoon(30)
                + "  •  Total: " + warrantyDAO.count());
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Warranties");
        alert.showAndWait();
    }
}
