package com.shop.view;

import com.shop.dao.ProductDAO;
import com.shop.dao.PurchaseOrderDAO;
import com.shop.dao.SupplierDAO;
import com.shop.model.Product;
import com.shop.model.PurchaseOrder;
import com.shop.model.PurchaseOrderItem;
import com.shop.model.Supplier;
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

import java.util.ArrayList;
import java.util.List;

public class PurchaseOrdersView {
    private final PurchaseOrderDAO purchaseOrderDAO = new PurchaseOrderDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final ObservableList<PurchaseOrder> poList = FXCollections.observableArrayList();
    private final TableView<PurchaseOrder> table = new TableView<>();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        HBox controlBar = new HBox(12);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        Label infoLabel = new Label("Create purchase orders to restock inventory from suppliers.");
        infoLabel.getStyleClass().add("sub-label");
        HBox.setHgrow(infoLabel, Priority.ALWAYS);

        Button newPoBtn = new Button("➕  New Purchase Order");
        newPoBtn.getStyleClass().add("btn-primary");

        Button lowStockBtn = new Button("📉  Generate from Low Stock");
        lowStockBtn.getStyleClass().add("btn-secondary");

        controlBar.getChildren().addAll(infoLabel, lowStockBtn, newPoBtn);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<PurchaseOrder, String> numberCol = new TableColumn<>("Order #");
        numberCol.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));

        TableColumn<PurchaseOrder, String> supplierCol = new TableColumn<>("Supplier");
        supplierCol.setCellValueFactory(new PropertyValueFactory<>("supplierName"));

        TableColumn<PurchaseOrder, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<PurchaseOrder, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    switch (item) {
                        case PurchaseOrder.STATUS_RECEIVED -> badge.getStyleClass().add("badge-active");
                        case PurchaseOrder.STATUS_CANCELLED -> badge.getStyleClass().add("badge-inactive");
                        case PurchaseOrder.STATUS_PENDING -> badge.getStyleClass().add("badge-warning");
                        default -> badge.getStyleClass().add("badge-info");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<PurchaseOrder, String> qtyCol = new TableColumn<>("Total Qty");
        qtyCol.setCellValueFactory(p -> new SimpleStringProperty(String.valueOf(p.getValue().getItemCount())));

        TableColumn<PurchaseOrder, String> totalCol = new TableColumn<>("Total Cost");
        totalCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().getTotalCost())));

        TableColumn<PurchaseOrder, String> createdCol = new TableColumn<>("Created");
        createdCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getCreatedAtLabel()));

        TableColumn<PurchaseOrder, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<PurchaseOrder, Void>() {
            private final Button viewBtn = new Button("👁️");
            private final Button receiveBtn = new Button("📦");
            private final Button cancelBtn = new Button("❌");
            private final HBox btnBox = new HBox(6, viewBtn, receiveBtn, cancelBtn);

            {
                viewBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                receiveBtn.getStyleClass().addAll("btn-primary", "btn-small");
                cancelBtn.getStyleClass().addAll("btn-danger", "btn-small");
                receiveBtn.setTooltip(new Tooltip("Mark as Received"));
                cancelBtn.setTooltip(new Tooltip("Cancel Order"));

                viewBtn.setOnAction(e -> showDetails(getTableView().getItems().get(getIndex())));
                receiveBtn.setOnAction(e -> {
                    PurchaseOrder po = getTableView().getItems().get(getIndex());
                    markReceived(po);
                });
                cancelBtn.setOnAction(e -> {
                    PurchaseOrder po = getTableView().getItems().get(getIndex());
                    cancelOrder(po);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    PurchaseOrder po = getTableView().getItems().get(getIndex());
                    boolean pending = po != null && PurchaseOrder.STATUS_PENDING.equals(po.getStatus());
                    receiveBtn.setDisable(!pending);
                    cancelBtn.setDisable(!pending);
                    setGraphic(btnBox);
                }
            }
        });

        table.getColumns().addAll(numberCol, supplierCol, statusCol, qtyCol, totalCol, createdCol, actionCol);
        table.setItems(poList);
        table.setPlaceholder(new Label("No purchase orders yet. Create one to start restocking!"));

        newPoBtn.setOnAction(e -> showNewOrderDialog(new PurchaseOrder()));
        lowStockBtn.setOnAction(e -> generateFromLowStock());

        root.getChildren().addAll(controlBar, table);
        loadOrders();
        return root;
    }

    private void loadOrders() {
        poList.clear();
        poList.addAll(purchaseOrderDAO.findAll());
    }

    private void markReceived(PurchaseOrder po) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Mark order " + po.getOrderNumber() + " as received?\nStock quantities will be updated and buy prices refreshed.",
                ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                boolean ok = purchaseOrderDAO.markAsReceived(po.getId());
                Alert info = new Alert(ok ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                        ok ? "Order marked as received. Stock updated!" : "Could not process order. Please try again.");
                info.showAndWait();
                loadOrders();
            }
        });
    }

    private void cancelOrder(PurchaseOrder po) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Cancel order " + po.getOrderNumber() + "?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                purchaseOrderDAO.cancel(po.getId());
                loadOrders();
            }
        });
    }

    private void generateFromLowStock() {
        List<Product> lowStock = productDAO.findLowStock();
        List<Product> withSupplier = lowStock.stream()
                .filter(p -> p.getSupplierId() > 0)
                .toList();
        if (withSupplier.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION,
                    "No low-stock products with an assigned supplier. Assign suppliers to products first.");
            a.showAndWait();
            return;
        }

        List<Supplier> suppliers = supplierDAO.findAll();
        java.util.Map<Integer, Supplier> supplierById = new java.util.HashMap<>();
        for (Supplier s : suppliers) supplierById.put(s.getId(), s);

        java.util.Map<Integer, PurchaseOrder> ordersBySupplier = new java.util.LinkedHashMap<>();
        for (Product p : withSupplier) {
            int sid = p.getSupplierId();
            PurchaseOrder po = ordersBySupplier.computeIfAbsent(sid, k -> {
                PurchaseOrder npo = new PurchaseOrder();
                Supplier s = supplierById.get(k);
                npo.setSupplierId(k);
                npo.setSupplierName(s != null ? s.getCompanyName() : "");
                npo.setStatus(PurchaseOrder.STATUS_PENDING);
                return npo;
            });
            int qty = Math.max(p.getMinStockLevel() * 2 - p.getQuantity(), 10);
            po.getItems().add(new PurchaseOrderItem(p.getId(), p.getName(), p.getBarcode(), qty, p.getBuyPrice()));
            po.computeTotal();
        }

        StringBuilder msg = new StringBuilder();
        msg.append("Generated purchase suggestions for ").append(ordersBySupplier.size())
                .append(" supplier(s):\n\n");
        for (PurchaseOrder po : ordersBySupplier.values()) {
            msg.append("• ").append(po.getSupplierName()).append(" — ")
                    .append(po.getItemCount()).append(" units, ₹")
                    .append(String.format("%.2f", po.getTotalCost())).append("\n");
        }
        msg.append("\nReview each order before confirming.");

        Alert info = new Alert(Alert.AlertType.INFORMATION, msg.toString());
        info.showAndWait();

        // Open new order dialog for the first generated order so the user can review it
        showNewOrderDialog(ordersBySupplier.values().iterator().next());
    }

    private void showNewOrderDialog(PurchaseOrder prefill) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("New Purchase Order");

        VBox form = new VBox(14);
        form.setPadding(new Insets(20));
        form.getStyleClass().add("card");

        ComboBox<Supplier> supplierCombo = new ComboBox<>();
        List<Supplier> suppliers = supplierDAO.findAll();
        supplierCombo.setItems(FXCollections.observableArrayList(suppliers));
        supplierCombo.setPromptText("Select Supplier");
        supplierCombo.setMaxWidth(Double.MAX_VALUE);
        if (prefill.getSupplierId() > 0) {
            suppliers.stream().filter(s -> s.getId() == prefill.getSupplierId()).findFirst()
                    .ifPresent(supplierCombo::setValue);
        }

        // Items table
        ObservableList<PurchaseOrderItem> cartItems = FXCollections.observableArrayList(prefill.getItems());
        TableView<PurchaseOrderItem> itemTable = new TableView<>();
        itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        itemTable.setPrefHeight(200);

        TableColumn<PurchaseOrderItem, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

        TableColumn<PurchaseOrderItem, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<PurchaseOrderItem, Double> costCol = new TableColumn<>("Unit Cost");
        costCol.setCellValueFactory(new PropertyValueFactory<>("unitCost"));

        TableColumn<PurchaseOrderItem, String> totalCol = new TableColumn<>("Line Total");
        totalCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().getTotal())));

        itemTable.getColumns().addAll(nameCol, qtyCol, costCol, totalCol);
        itemTable.setItems(cartItems);

        // Add item row
        HBox addRow = new HBox(8);
        ComboBox<Product> productCombo = new ComboBox<>();
        productCombo.setItems(FXCollections.observableArrayList(productDAO.findAll()));
        productCombo.setPromptText("Product");
        productCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(productCombo, Priority.ALWAYS);

        TextField qtyField = new TextField("1");
        qtyField.setPromptText("Qty");
        qtyField.setPrefWidth(70);

        TextField costField = new TextField();
        costField.setPromptText("Unit cost");
        costField.setPrefWidth(100);

        productCombo.valueProperty().addListener((obs, o, n) -> {
            if (n != null) costField.setText(String.valueOf(n.getBuyPrice()));
        });

        Button addItemBtn = new Button("➕ Add");
        addItemBtn.getStyleClass().add("btn-secondary");
        addItemBtn.setOnAction(e -> {
            Product p = productCombo.getValue();
            if (p == null) {
                new Alert(Alert.AlertType.ERROR, "Select a product!").showAndWait();
                return;
            }
            int qty;
            double cost;
            try {
                qty = Integer.parseInt(qtyField.getText().trim());
                cost = Double.parseDouble(costField.getText().trim());
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.ERROR, "Enter a valid quantity and unit cost!").showAndWait();
                return;
            }
            if (qty <= 0 || cost < 0) {
                new Alert(Alert.AlertType.ERROR, "Quantity must be positive and cost non-negative!").showAndWait();
                return;
            }
            cartItems.add(new PurchaseOrderItem(p.getId(), p.getName(), p.getBarcode(), qty, cost));
            productCombo.setValue(null);
            qtyField.setText("1");
            costField.clear();
            updateTotalLabel(itemTable, form);
        });
        addRow.getChildren().addAll(productCombo, qtyField, costField, addItemBtn);

        Label totalLabel = new Label();
        totalLabel.getStyleClass().add("section-title");
        updateTotalLabel(itemTable, totalLabel);

        TextArea noteArea = new TextArea(prefill.getNote() != null ? prefill.getNote() : "");
        noteArea.setPromptText("Notes (optional)");
        noteArea.setPrefRowCount(2);

        Button createBtn = new Button("Create & Place Order");
        createBtn.getStyleClass().add("btn-primary");
        createBtn.setMaxWidth(Double.MAX_VALUE);
        createBtn.setOnAction(e -> {
            Supplier s = supplierCombo.getValue();
            if (s == null) {
                new Alert(Alert.AlertType.ERROR, "Select a supplier!").showAndWait();
                return;
            }
            if (cartItems.isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "Add at least one product!").showAndWait();
                return;
            }
            PurchaseOrder po = new PurchaseOrder();
            po.setSupplierId(s.getId());
            po.setSupplierName(s.getCompanyName());
            po.setItems(new ArrayList<>(cartItems));
            po.computeTotal();
            po.setStatus(PurchaseOrder.STATUS_PENDING);
            po.setNote(noteArea.getText().trim());
            purchaseOrderDAO.create(po);
            dialog.close();
            loadOrders();
        });

        form.getChildren().addAll(
                new Label("Supplier"), supplierCombo,
                new Label("Order Items"), itemTable,
                addRow, totalLabel,
                new Label("Notes"), noteArea,
                createBtn);

        Scene scene = new Scene(form, 640, 620);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.show();
    }

    private void updateTotalLabel(TableView<PurchaseOrderItem> itemTable, Label totalLabel) {
        double total = 0;
        for (PurchaseOrderItem item : itemTable.getItems()) total += item.getTotal();
        totalLabel.setText("Total: ₹" + String.format("%.2f", total));
    }

    private void updateTotalLabel(TableView<PurchaseOrderItem> itemTable, VBox form) {
        for (Node child : form.getChildren()) {
            if (child instanceof Label label && label.getText().startsWith("Total:")) {
                updateTotalLabel(itemTable, label);
                return;
            }
        }
    }

    private void showDetails(PurchaseOrder po) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Purchase Order " + po.getOrderNumber());

        VBox form = new VBox(12);
        form.setPadding(new Insets(20));
        form.getStyleClass().add("card");

        Label header = new Label(po.getOrderNumber() + " — " + po.getSupplierName());
        header.getStyleClass().add("section-title");

        Label statusLabel = new Label("Status: " + po.getStatus());
        Label createdLabel = new Label("Created: " + po.getCreatedAtLabel());
        Label receivedLabel = new Label("Received: " + po.getReceivedAtLabel());

        TableView<PurchaseOrderItem> itemTable = new TableView<>();
        itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        itemTable.setPrefHeight(220);

        TableColumn<PurchaseOrderItem, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

        TableColumn<PurchaseOrderItem, String> barcodeCol = new TableColumn<>("Barcode");
        barcodeCol.setCellValueFactory(new PropertyValueFactory<>("barcode"));

        TableColumn<PurchaseOrderItem, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<PurchaseOrderItem, Double> costCol = new TableColumn<>("Unit Cost");
        costCol.setCellValueFactory(new PropertyValueFactory<>("unitCost"));

        TableColumn<PurchaseOrderItem, String> totalCol = new TableColumn<>("Line Total");
        totalCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().getTotal())));

        itemTable.getColumns().addAll(nameCol, barcodeCol, qtyCol, costCol, totalCol);
        itemTable.setItems(FXCollections.observableArrayList(po.getItems()));

        Label totalLabel = new Label("Total Cost: ₹" + String.format("%.2f", po.getTotalCost()));
        totalLabel.getStyleClass().add("section-title");

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-secondary");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> dialog.close());

        form.getChildren().addAll(header, statusLabel, createdLabel, receivedLabel,
                itemTable, totalLabel, closeBtn);

        Scene scene = new Scene(form, 620, 480);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.show();
    }
}
