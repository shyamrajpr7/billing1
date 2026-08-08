package com.shop.view;

import com.shop.dao.SaleDAO;
import com.shop.model.Sale;
import com.shop.model.SaleItem;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class ReportsView {
    private final SaleDAO saleDAO = new SaleDAO();
    private final ObservableList<Sale> salesList = FXCollections.observableArrayList();
    private final TableView<Sale> table = new TableView<>();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        // Metric Summary Cards
        HBox metricsGrid = new HBox(16);

        double totalRevenue = saleDAO.getTotalRevenueThisMonth();
        double todayRevenue = saleDAO.getTotalRevenueToday();
        int totalSales = saleDAO.findAll().size();

        VBox card1 = createMetricCard("📅", String.format("₹%.2f", todayRevenue), "Today's Revenue");
        VBox card2 = createMetricCard("🗓️", String.format("₹%.2f", totalRevenue), "This Month's Revenue");
        VBox card3 = createMetricCard("🧾", String.valueOf(totalSales), "Total Transactions");

        metricsGrid.getChildren().addAll(card1, card2, card3);
        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);

        // Sales History Section
        VBox tableCard = new VBox(12);
        tableCard.getStyleClass().add("card");
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        Label title = new Label("📊 Recent Sales History");
        title.getStyleClass().add("section-title");

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Sale, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setMaxWidth(60);

        TableColumn<Sale, String> invCol = new TableColumn<>("Invoice #");
        invCol.setCellValueFactory(new PropertyValueFactory<>("invoiceNumber"));

        TableColumn<Sale, String> dateCol = new TableColumn<>("Date & Time");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("formattedDate"));

        TableColumn<Sale, String> custCol = new TableColumn<>("Customer");
        custCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));

        TableColumn<Sale, String> cashierCol = new TableColumn<>("Cashier");
        cashierCol.setCellValueFactory(new PropertyValueFactory<>("userName"));

        TableColumn<Sale, String> payCol = new TableColumn<>("Payment");
        payCol.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));

        TableColumn<Sale, String> totalCol = new TableColumn<>("Total Amount");
        totalCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getFormattedTotal()));

        TableColumn<Sale, Void> actionCol = new TableColumn<>("View");
        actionCol.setCellFactory(col -> new TableCell<Sale, Void>() {
            private final Button viewBtn = new Button("👁️ Details");
            {
                viewBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                viewBtn.setOnAction(e -> {
                    Sale sale = getTableView().getItems().get(getIndex());
                    showSaleDetails(sale.getId());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(viewBtn);
            }
        });

        table.getColumns().addAll(idCol, invCol, dateCol, custCol, cashierCol, payCol, totalCol, actionCol);
        table.setItems(salesList);
        table.setPlaceholder(new Label("No sales completed yet. Completed POS transactions will appear here."));

        tableCard.getChildren().addAll(title, table);

        root.getChildren().addAll(metricsGrid, tableCard);
        loadSales();
        return root;
    }

    private void loadSales() {
        salesList.clear();
        salesList.addAll(saleDAO.findAll());
    }

    private VBox createMetricCard(String icon, String value, String label) {
        VBox card = new VBox(6);
        card.getStyleClass().add("stat-card");

        Label iconL = new Label(icon);
        iconL.getStyleClass().add("stat-icon");

        Label valL = new Label(value);
        valL.getStyleClass().addAll("stat-value", "text-accent");

        Label lbl = new Label(label);
        lbl.getStyleClass().add("stat-label");

        card.getChildren().addAll(iconL, valL, lbl);
        return card;
    }

    private void showSaleDetails(int saleId) {
        Sale sale = saleDAO.findById(saleId);
        if (sale == null) return;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Sale Details - " + sale.getInvoiceNumber());

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("card");

        Label header = new Label("Invoice Details: " + sale.getInvoiceNumber());
        header.getStyleClass().add("section-title");

        VBox meta = new VBox(4);
        meta.getChildren().addAll(
                new Label("Customer: " + sale.getCustomerName()),
                new Label("Processed By: " + sale.getUserName()),
                new Label("Date: " + sale.getFormattedDate()),
                new Label("Payment Mode: " + sale.getPaymentMethod())
        );

        TableView<SaleItem> itemTable = new TableView<>(FXCollections.observableArrayList(sale.getItems()));
        itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        itemTable.setPrefHeight(180);

        TableColumn<SaleItem, String> pCol = new TableColumn<>("Product");
        pCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

        TableColumn<SaleItem, Integer> qCol = new TableColumn<>("Qty");
        qCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<SaleItem, String> uCol = new TableColumn<>("Unit Price");
        uCol.setCellValueFactory(i -> new SimpleStringProperty(i.getValue().getFormattedUnitPrice()));

        TableColumn<SaleItem, String> tCol = new TableColumn<>("Total");
        tCol.setCellValueFactory(i -> new SimpleStringProperty(i.getValue().getFormattedTotal()));

        itemTable.getColumns().addAll(pCol, qCol, uCol, tCol);

        VBox summary = new VBox(4);
        summary.getChildren().addAll(
                new Label(String.format("Subtotal: ₹%.2f", sale.getSubtotal())),
                new Label(String.format("Discount: -₹%.2f", sale.getDiscountAmount())),
                new Label(String.format("Tax: ₹%.2f", sale.getTax())),
                new Label(String.format("Grand Total: ₹%.2f", sale.getTotal())) {{ setStyle("-fx-font-weight: bold; -fx-text-fill: #16c79a;"); }}
        );

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-secondary");
        closeBtn.setOnAction(e -> dialog.close());

        root.getChildren().addAll(header, meta, itemTable, summary, closeBtn);

        Scene scene = new Scene(root, 480, 520);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.show();
    }
}
