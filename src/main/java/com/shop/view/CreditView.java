package com.shop.view;

import com.shop.dao.SaleDAO;
import com.shop.model.Sale;
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

public class CreditView {
    private final SaleDAO saleDAO = new SaleDAO();
    private final ObservableList<Sale> creditList = FXCollections.observableArrayList();
    private final TableView<Sale> table = new TableView<>();
    private final Label statsLabel = new Label();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label title = new Label("💳 Credit Sales & Due Tracking");
        title.getStyleClass().add("section-title");
        Label sub = new Label("Sales made on credit with their outstanding balances. Collect payments to reduce dues.");
        sub.getStyleClass().add("sub-label");
        statsLabel.getStyleClass().add("sub-label");
        headerCard.getChildren().addAll(title, sub, statsLabel);

        Label tableTitle = new Label("📋 Outstanding Credit Sales");
        tableTitle.getStyleClass().add("section-title");

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Sale, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("formattedDate"));
        dateCol.setPrefWidth(100);

        TableColumn<Sale, String> invCol = new TableColumn<>("Invoice");
        invCol.setCellValueFactory(new PropertyValueFactory<>("invoiceNumber"));

        TableColumn<Sale, String> custCol = new TableColumn<>("Customer");
        custCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));

        TableColumn<Sale, String> totalCol = new TableColumn<>("Sale Total");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("formattedTotal"));

        TableColumn<Sale, String> paidCol = new TableColumn<>("Paid");
        paidCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().getAmountPaid())));

        TableColumn<Sale, String> dueCol = new TableColumn<>("Due");
        dueCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().getDueAmount())));
        dueCol.setCellFactory(col -> new TableCell<Sale, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                if (!empty && item != null) getStyleClass().add("cell-danger");
            }
        });

        TableColumn<Sale, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(140);
        actionCol.setCellFactory(col -> new TableCell<Sale, Void>() {
            private final Button collectBtn = new Button("💰 Collect Payment");
            {
                collectBtn.getStyleClass().addAll("btn-primary", "btn-small");
                collectBtn.setOnAction(e -> collectPayment(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || getTableRow() == null ? null : collectBtn);
            }
        });

        table.getColumns().addAll(dateCol, invCol, custCol, totalCol, paidCol, dueCol, actionCol);
        table.setItems(creditList);
        table.setPlaceholder(new Label("No outstanding credit sales. Nice! 🎉"));

        VBox dueCard = new VBox(8);
        dueCard.getStyleClass().add("card");
        Label dueTitle = new Label("👥 Dues by Customer");
        dueTitle.getStyleClass().add("section-title");
        TableView<java.util.Map.Entry<String, Double>> dueTable = new TableView<>();
        dueTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        dueTable.setPrefHeight(180);
        TableColumn<java.util.Map.Entry<String, Double>, String> nameCol = new TableColumn<>("Customer");
        nameCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getKey()));
        TableColumn<java.util.Map.Entry<String, Double>, String> amtCol = new TableColumn<>("Total Due");
        amtCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().getValue())));
        dueTable.getColumns().addAll(nameCol, amtCol);
        dueCard.getChildren().add(dueTable);

        root.getChildren().addAll(headerCard, tableTitle, table, dueCard);

        refresh();
        return root;
    }

    private void collectPayment(Sale sale) {
        if (sale == null) return;
        double due = sale.getDueAmount();
        TextInputDialog dialog = new TextInputDialog(String.format("%.2f", due));
        dialog.setTitle("Collect Payment");
        dialog.setHeaderText("Invoice " + sale.getInvoiceNumber() + " — Due: ₹" + String.format("%.2f", due));
        dialog.setContentText("Amount to collect (₹):");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            try {
                double amount = Double.parseDouble(input.trim());
                if (amount <= 0) {
                    showAlert("Amount must be greater than zero.");
                    return;
                }
                if (amount > due + 0.001) {
                    showAlert("Amount exceeds the remaining due of ₹" + String.format("%.2f", due) + ".");
                    return;
                }
                if (saleDAO.collectPayment(sale.getId(), amount)) {
                    refresh();
                } else {
                    showAlert("Payment collection failed.");
                }
            } catch (NumberFormatException ex) {
                showAlert("Please enter a valid amount.");
            }
        });
    }

    private void refresh() {
        creditList.clear();
        creditList.addAll(saleDAO.getOutstandingCreditSales());
        var dueByCustomer = saleDAO.getCustomerDueTotals();
        double totalDue = dueByCustomer.values().stream().mapToDouble(Double::doubleValue).sum();
        statsLabel.setText("Outstanding invoices: " + creditList.size()
                + "  •  Total due: ₹" + String.format("%.2f", totalDue)
                + "  •  Customers with dues: " + dueByCustomer.size());
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Credit & Dues");
        alert.showAndWait();
    }
}
