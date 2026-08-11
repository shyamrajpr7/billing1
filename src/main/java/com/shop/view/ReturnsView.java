package com.shop.view;

import com.shop.dao.ReturnDAO;
import com.shop.dao.SaleDAO;
import com.shop.model.Return;
import com.shop.model.Sale;
import com.shop.model.SaleItem;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReturnsView {
    private final SaleDAO saleDAO = new SaleDAO();
    private final ReturnDAO returnDAO = new ReturnDAO();
    private final ObservableList<SaleItem> cartItems = FXCollections.observableArrayList();
    private final ObservableList<Return> returnList = FXCollections.observableArrayList();
    private final Map<Integer, Integer> returnQuantities = new HashMap<>();
    private final Map<Integer, Integer> maxReturnable = new HashMap<>();

    private Sale loadedSale;
    private Label invoiceInfoLabel = new Label();
    private Label refundTotalLabel = new Label("₹0.00");
    private Label statusLabel = new Label();
    private Label todayLabel = new Label();

    private final TextField invoiceField = new TextField();
    private final TextField reasonField = new TextField();

    private TableView<SaleItem> itemsTable = new TableView<>();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");

        Label title = new Label("↩️ Returns & Refunds");
        title.getStyleClass().add("section-title");
        todayLabel.getStyleClass().add("sub-label");
        headerCard.getChildren().addAll(title, todayLabel);
        updateTodayLabel();

        VBox processCard = new VBox(14);
        processCard.getStyleClass().add("card");

        HBox invoiceBar = new HBox(12);
        invoiceBar.setAlignment(Pos.CENTER_LEFT);
        invoiceField.setPromptText("Enter invoice number, e.g. INV-000001");
        invoiceField.getStyleClass().add("search-field");
        HBox.setHgrow(invoiceField, Priority.ALWAYS);
        Button loadBtn = new Button("🔎  Load Sale");
        loadBtn.getStyleClass().add("btn-primary");
        loadBtn.setOnAction(e -> loadSale(invoiceField.getText().trim()));
        invoiceBar.getChildren().addAll(invoiceField, loadBtn);

        invoiceInfoLabel.getStyleClass().add("sub-label");
        statusLabel.getStyleClass().add("badge-info");

        Label itemsTitle = new Label("🛒 Items from the Sale");
        itemsTitle.getStyleClass().add("section-title");

        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        itemsTable.setPrefHeight(260);
        VBox.setVgrow(itemsTable, Priority.ALWAYS);

        TableColumn<SaleItem, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

        TableColumn<SaleItem, Integer> soldCol = new TableColumn<>("Sold");
        soldCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<SaleItem, String> alreadyCol = new TableColumn<>("Already Returned");
        alreadyCol.setCellValueFactory(p -> new SimpleStringProperty(String.valueOf(getReturnedQty(p.getValue()))));

        TableColumn<SaleItem, String> availableCol = new TableColumn<>("Available");
        availableCol.setCellValueFactory(p -> new SimpleStringProperty(String.valueOf(getAvailableQty(p.getValue()))));

        TableColumn<SaleItem, Void> qtyCol = new TableColumn<>("Return Qty");
        qtyCol.setPrefWidth(110);
        qtyCol.setCellFactory(col -> new TableCell<>() {
            private final IntegerSpinnerValueFactory valueFactory =
                    new IntegerSpinnerValueFactory(0, Integer.MAX_VALUE, 0);
            private final Spinner<Integer> spinner = new Spinner<>(valueFactory);
            {
                spinner.setEditable(true);
                spinner.setPrefWidth(80);
                spinner.valueProperty().addListener((obs, o, n) -> {
                    SaleItem item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null) {
                        int val = n == null ? 0 : n;
                        int max = getAvailableQty(item);
                        if (val > max) {
                            valueFactory.setValue(max);
                            return;
                        }
                        returnQuantities.put(item.getProductId(), val);
                        updateRefundTotal();
                    }
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                SaleItem item = getTableRow() != null ? getTableRow().getItem() : null;
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    int max = getAvailableQty(item);
                    valueFactory.setMax(max);
                    valueFactory.setValue(Math.min(returnQuantities.getOrDefault(item.getProductId(), 0), max));
                    setGraphic(spinner);
                }
            }
        });

        TableColumn<SaleItem, String> priceCol = new TableColumn<>("Unit Price");
        priceCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getFormattedUnitPrice()));

        itemsTable.getColumns().addAll(nameCol, soldCol, alreadyCol, availableCol, qtyCol, priceCol);
        itemsTable.setItems(cartItems);
        itemsTable.setPlaceholder(new Label("Load a sale to view its items."));

        HBox refundRow = new HBox(14);
        refundRow.setAlignment(Pos.CENTER_LEFT);
        reasonField.setPromptText("Reason for return (e.g. damaged, wrong size)");
        HBox.setHgrow(reasonField, Priority.ALWAYS);
        Label refundLabel = new Label("Refund Amount:");
        refundLabel.getStyleClass().add("sub-label");
        refundTotalLabel.getStyleClass().add("stat-value");
        Button processBtn = new Button("💸  Process Refund");
        processBtn.getStyleClass().add("btn-danger");
        processBtn.setOnAction(e -> processRefund());
        refundRow.getChildren().addAll(reasonField, refundLabel, refundTotalLabel, processBtn);

        processCard.getChildren().addAll(invoiceBar, invoiceInfoLabel, statusLabel, itemsTitle, itemsTable, refundRow);

        VBox historyCard = new VBox(14);
        historyCard.getStyleClass().add("card");
        Label historyTitle = new Label("📜 Refund History");
        historyTitle.getStyleClass().add("section-title");

        TableView<Return> historyTable = new TableView<>();
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        historyTable.setPrefHeight(220);

        TableColumn<Return, String> hDateCol = new TableColumn<>("Date");
        hDateCol.setCellValueFactory(new PropertyValueFactory<>("formattedDate"));
        hDateCol.setPrefWidth(110);

        TableColumn<Return, String> hInvCol = new TableColumn<>("Invoice");
        hInvCol.setCellValueFactory(new PropertyValueFactory<>("invoiceNumber"));

        TableColumn<Return, String> hCustCol = new TableColumn<>("Customer");
        hCustCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));

        TableColumn<Return, String> hAmtCol = new TableColumn<>("Refunded");
        hAmtCol.setCellValueFactory(new PropertyValueFactory<>("formattedRefund"));
        hAmtCol.setCellFactory(col -> new TableCell<Return, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                if (!empty && item != null) getStyleClass().add("cell-danger");
            }
        });

        TableColumn<Return, String> hReasonCol = new TableColumn<>("Reason");
        hReasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));

        TableColumn<Return, String> hItemsCol = new TableColumn<>("Items");
        hItemsCol.setCellValueFactory(p -> new SimpleStringProperty(String.valueOf(p.getValue().getItems().size())));

        historyTable.getColumns().addAll(hDateCol, hInvCol, hCustCol, hAmtCol, hReasonCol, hItemsCol);
        historyTable.setItems(returnList);
        historyTable.setPlaceholder(new Label("No refunds processed yet."));

        historyCard.getChildren().addAll(historyTitle, historyTable);

        root.getChildren().addAll(headerCard, processCard, historyCard);
        refreshHistory();
        return new ScrollPane(root) {{ setFitToWidth(true); setStyle("-fx-background-color: transparent;"); }};
    }

    private void loadSale(String invoice) {
        if (invoice.isEmpty()) {
            statusLabel.setText("⚠️ Please enter an invoice number.");
            return;
        }
        Sale sale = saleDAO.findByInvoiceNumber(invoice);
        if (sale == null) {
            statusLabel.setText("❌ No sale found with invoice number " + invoice + ".");
            invoiceInfoLabel.setText("");
            cartItems.clear();
            returnQuantities.clear();
            maxReturnable.clear();
            loadedSale = null;
            updateRefundTotal();
            return;
        }
        loadedSale = sale;
        returnQuantities.clear();
        maxReturnable.clear();
        cartItems.clear();
        cartItems.addAll(sale.getItems());

        double remaining = 0;
        for (SaleItem item : sale.getItems()) {
            int avail = Math.max(item.getQuantity() - getReturnedQty(item), 0);
            maxReturnable.put(item.getProductId(), avail);
            remaining += avail;
        }
        invoiceInfoLabel.setText("Invoice " + sale.getInvoiceNumber() + " • " + sale.getCustomerName()
                + " • " + sale.getItems().size() + " item(s) • " + sale.getFormattedTotal()
                + " • " + sale.getFormattedDate());
        statusLabel.setText(remaining <= 0 ? "⛔ All items from this sale have already been returned."
                : "✅ Sale loaded. Set quantities above and click 'Process Refund'.");
        updateRefundTotal();
    }

    private void processRefund() {
        if (loadedSale == null) {
            showAlert("Please load a sale first.");
            return;
        }
        List<SaleItem> toReturn = new java.util.ArrayList<>();
        double rawSum = 0;
        for (SaleItem item : loadedSale.getItems()) {
            int qty = returnQuantities.getOrDefault(item.getProductId(), 0);
            if (qty <= 0) continue;
            int avail = getAvailableQty(item);
            if (qty > avail) {
                showAlert("Cannot return more than " + avail + " of " + item.getProductName() + ".");
                return;
            }
            SaleItem ri = new SaleItem(item.getProductId(), item.getProductName(), qty, item.getUnitPrice());
            ri.setDiscount(0);
            toReturn.add(ri);
            rawSum += ri.getTotal();
        }
        if (toReturn.isEmpty()) {
            showAlert("Enter a quantity greater than zero for at least one item.");
            return;
        }
        if (reasonField.getText().trim().isEmpty()) {
            showAlert("Please enter a reason for the return.");
            return;
        }

        double saleSubtotal = Math.max(loadedSale.getSubtotal(), 0.0001);
        double refund = rawSum
                - (loadedSale.getDiscountAmount() * rawSum / saleSubtotal)
                + (loadedSale.getTax() * rawSum / saleSubtotal);
        refund = Math.round(refund * 100.0) / 100.0;

        Return ret = new Return();
        ret.setSaleId(loadedSale.getId());
        ret.setInvoiceNumber(loadedSale.getInvoiceNumber());
        ret.setCustomerId(loadedSale.getCustomerId());
        ret.setCustomerName(loadedSale.getCustomerName());
        ret.setRefundAmount(refund);
        ret.setReason(reasonField.getText().trim());
        ret.setItems(toReturn);

        if (returnDAO.create(ret, loadedSale)) {
            showAlert("✅ Refund of ₹" + String.format("%.2f", refund)
                    + " processed and items restocked.");
            reasonField.clear();
            loadSale(loadedSale.getInvoiceNumber());
            refreshHistory();
            updateTodayLabel();
        } else {
            showAlert("❌ Failed to process refund. Please try again.");
        }
    }

    private void refreshHistory() {
        returnList.clear();
        returnList.addAll(returnDAO.findAll());
    }

    private void updateTodayLabel() {
        todayLabel.setText("Today: ₹" + String.format("%.2f", returnDAO.getTotalRefundsToday())
                + "  •  This Month: ₹" + String.format("%.2f", returnDAO.getTotalRefundsThisMonth())
                + "  •  Total Returns: " + returnDAO.count());
    }

    private void updateRefundTotal() {
        if (loadedSale == null) {
            refundTotalLabel.setText("₹0.00");
            return;
        }
        double rawSum = 0;
        for (SaleItem item : loadedSale.getItems()) {
            int qty = returnQuantities.getOrDefault(item.getProductId(), 0);
            rawSum += qty * item.getUnitPrice();
        }
        double saleSubtotal = Math.max(loadedSale.getSubtotal(), 0.0001);
        double refund = rawSum
                - (loadedSale.getDiscountAmount() * rawSum / saleSubtotal)
                + (loadedSale.getTax() * rawSum / saleSubtotal);
        refundTotalLabel.setText(String.format("₹%.2f", Math.max(refund, 0)));
    }

    private int getReturnedQty(SaleItem item) {
        if (loadedSale == null) return 0;
        return returnDAO.getReturnedQuantity(loadedSale.getId(), item.getProductId());
    }

    private int getAvailableQty(SaleItem item) {
        return Math.max(item.getQuantity() - getReturnedQty(item), 0);
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Returns & Refunds");
        alert.showAndWait();
    }
}
