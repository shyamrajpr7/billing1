package com.shop.view;

import com.shop.dao.SupplierDAO;
import com.shop.dao.SupplierPaymentDAO;
import com.shop.model.Supplier;
import com.shop.model.SupplierPayment;
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

public class SupplierPaymentsView {
    private final SupplierPaymentDAO paymentDAO = new SupplierPaymentDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final ObservableList<SupplierPayment> paymentList = FXCollections.observableArrayList();
    private final TableView<SupplierPayment> table = new TableView<>();
    private final Label statsLabel = new Label();

    private final ComboBox<Supplier> supplierCombo = new ComboBox<>();
    private final TextField amountField = new TextField();
    private final ComboBox<String> methodCombo = new ComboBox<>();
    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final TextField referenceField = new TextField();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label title = new Label("💸 Supplier Payments & Bills");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Track payments made to suppliers against their bills.");
        subtitle.getStyleClass().add("sub-label");
        statsLabel.getStyleClass().add("sub-label");
        headerCard.getChildren().addAll(title, subtitle, statsLabel);

        VBox payCard = new VBox(12);
        payCard.getStyleClass().add("card");
        Label payTitle = new Label("➕ Record a Supplier Payment");
        payTitle.getStyleClass().add("section-title");

        supplierCombo.setPromptText("Supplier");
        supplierCombo.setPrefWidth(240);
        supplierCombo.getItems().addAll(supplierDAO.findAll());

        amountField.setPromptText("Amount (₹)");
        amountField.setPrefWidth(110);

        methodCombo.getItems().addAll("Cash", "UPI", "Bank Transfer", "Cheque", "Card");
        methodCombo.setValue("Cash");
        methodCombo.setPrefWidth(130);

        datePicker.setPrefWidth(140);

        referenceField.setPromptText("Bill / reference no. (optional)");
        referenceField.setPrefWidth(160);

        Button recordBtn = new Button("💸  Record Payment");
        recordBtn.getStyleClass().add("btn-primary");
        recordBtn.setOnAction(e -> recordPayment());

        HBox formRow = new HBox(10, supplierCombo, amountField, methodCombo, datePicker, referenceField, recordBtn);
        formRow.setAlignment(Pos.CENTER_LEFT);

        payCard.getChildren().addAll(payTitle, formRow);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<SupplierPayment, String> numCol = new TableColumn<>("Payment No.");
        numCol.setCellValueFactory(new PropertyValueFactory<>("paymentNumber"));

        TableColumn<SupplierPayment, String> supplierCol = new TableColumn<>("Supplier");
        supplierCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getSupplierName() != null
                ? p.getValue().getSupplierName() : "—"));

        TableColumn<SupplierPayment, String> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().getAmount())));
        amountCol.setCellFactory(col -> new TableCell<SupplierPayment, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                if (!empty && item != null) getStyleClass().add("cell-accent");
            }
        });

        TableColumn<SupplierPayment, String> methodCol = new TableColumn<>("Method");
        methodCol.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));

        TableColumn<SupplierPayment, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("paymentDateLabel"));

        TableColumn<SupplierPayment, String> refCol = new TableColumn<>("Reference");
        refCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getReference() != null
                && !p.getValue().getReference().isEmpty() ? p.getValue().getReference() : "—"));

        table.getColumns().addAll(numCol, supplierCol, amountCol, methodCol, dateCol, refCol);
        table.setItems(paymentList);
        table.setPlaceholder(new Label("No supplier payments recorded yet."));

        root.getChildren().addAll(headerCard, payCard, table);
        refresh();
        return root;
    }

    private void recordPayment() {
        Supplier s = supplierCombo.getValue();
        if (s == null) {
            showAlert("Please select a supplier.");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim());
        } catch (NumberFormatException ex) {
            showAlert("Enter a valid amount.");
            return;
        }
        if (amount <= 0) {
            showAlert("Amount must be greater than zero.");
            return;
        }
        SupplierPayment sp = new SupplierPayment();
        sp.setSupplierId(s.getId());
        sp.setSupplierName(s.getCompanyName());
        sp.setAmount(Math.round(amount * 100.0) / 100.0);
        sp.setPaymentMethod(methodCombo.getValue());
        sp.setPaymentDate(datePicker.getValue());
        sp.setReference(referenceField.getText().trim());
        if (paymentDAO.create(sp)) {
            showAlert("Payment recorded!\n\nPayment No: " + sp.getPaymentNumber()
                    + "\nSupplier: " + sp.getSupplierName()
                    + "\nAmount: ₹" + String.format("%.2f", sp.getAmount())
                    + "\nMethod: " + sp.getPaymentMethod());
            amountField.clear();
            referenceField.clear();
            supplierCombo.setValue(null);
            refresh();
        } else {
            showAlert("Failed to record payment.");
        }
    }

    private void refresh() {
        paymentList.clear();
        paymentList.addAll(paymentDAO.findAll());
        statsLabel.setText("Payments This Month: " + String.format("₹%.2f", paymentDAO.totalPaidThisMonth())
                + "  •  All-Time Paid: " + String.format("₹%.2f", paymentDAO.totalPaid())
                + "  •  Transactions: " + paymentDAO.count());
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Supplier Payments");
        alert.showAndWait();
    }
}
