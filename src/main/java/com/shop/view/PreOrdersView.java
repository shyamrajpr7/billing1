package com.shop.view;

import com.shop.dao.CustomerDAO;
import com.shop.dao.PreOrderDAO;
import com.shop.dao.ProductDAO;
import com.shop.model.Customer;
import com.shop.model.PreOrder;
import com.shop.model.PreOrderItem;
import com.shop.model.Product;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PreOrdersView {
    private final PreOrderDAO preOrderDAO = new PreOrderDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final ObservableList<PreOrder> poList = FXCollections.observableArrayList();
    private final TableView<PreOrder> table = new TableView<>();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        HBox controlBar = new HBox(12);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        Label infoLabel = new Label("Take advance bookings for items before stock arrives.");
        infoLabel.getStyleClass().add("sub-label");
        HBox.setHgrow(infoLabel, Priority.ALWAYS);

        Button newPoBtn = new Button("➕  New Pre-Order");
        newPoBtn.getStyleClass().add("btn-primary");

        controlBar.getChildren().addAll(infoLabel, newPoBtn);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<PreOrder, String> numberCol = new TableColumn<>("Order #");
        numberCol.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));

        TableColumn<PreOrder, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));

        TableColumn<PreOrder, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<PreOrder, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    switch (item) {
                        case PreOrder.STATUS_FULFILLED -> badge.getStyleClass().add("badge-active");
                        case PreOrder.STATUS_CANCELLED -> badge.getStyleClass().add("badge-inactive");
                        case PreOrder.STATUS_PENDING -> badge.getStyleClass().add("badge-warning");
                        default -> badge.getStyleClass().add("badge-info");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<PreOrder, String> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(p -> new SimpleStringProperty(String.valueOf(p.getValue().getItemCount())));

        TableColumn<PreOrder, String> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().getTotalAmount())));

        TableColumn<PreOrder, String> advanceCol = new TableColumn<>("Advance");
        advanceCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().getAdvancePaid())));

        TableColumn<PreOrder, String> dueCol = new TableColumn<>("Due");
        dueCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().getBalanceDue())));

        TableColumn<PreOrder, String> pickupCol = new TableColumn<>("Pickup");
        pickupCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getPickupDateLabel()));

        TableColumn<PreOrder, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<PreOrder, Void>() {
            private final Button viewBtn = new Button("👁️");
            private final Button fulfillBtn = new Button("✅");
            private final Button cancelBtn = new Button("❌");
            private final HBox btnBox = new HBox(6, viewBtn, fulfillBtn, cancelBtn);

            {
                viewBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                fulfillBtn.getStyleClass().addAll("btn-primary", "btn-small");
                cancelBtn.getStyleClass().addAll("btn-danger", "btn-small");
                fulfillBtn.setTooltip(new Tooltip("Fulfill — collect balance & record sale"));
                cancelBtn.setTooltip(new Tooltip("Cancel Pre-Order"));

                viewBtn.setOnAction(e -> showDetails(getTableView().getItems().get(getIndex())));
                fulfillBtn.setOnAction(e -> fulfill(getTableView().getItems().get(getIndex())));
                cancelBtn.setOnAction(e -> cancelOrder(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    PreOrder po = getTableView().getItems().get(getIndex());
                    boolean pending = po != null && PreOrder.STATUS_PENDING.equals(po.getStatus());
                    fulfillBtn.setDisable(!pending);
                    cancelBtn.setDisable(!pending);
                    setGraphic(btnBox);
                }
            }
        });

        table.getColumns().addAll(numberCol, customerCol, statusCol, qtyCol, totalCol, advanceCol, dueCol, pickupCol, actionCol);
        table.setItems(poList);
        table.setPlaceholder(new Label("No pre-orders yet. Take your first advance booking!"));

        newPoBtn.setOnAction(e -> showNewOrderDialog());

        root.getChildren().addAll(controlBar, table);
        loadOrders();
        return root;
    }

    private void loadOrders() {
        poList.clear();
        poList.addAll(preOrderDAO.findAll());
    }

    private void fulfill(PreOrder po) {
        String msg = "Collect ₹" + String.format("%.2f", po.getBalanceDue())
                + " balance and fulfill order " + po.getOrderNumber() + "?\n\n"
                + "This records the sale (" + po.getItems().size() + " items), updates stock "
                + "and marks the pre-order as Fulfilled.";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                boolean ok = preOrderDAO.markAsFulfilled(po.getId());
                Alert info = new Alert(ok ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                        ok ? "Pre-order fulfilled and sale recorded!" : "Could not fulfill. Make sure enough stock is available.");
                info.showAndWait();
                loadOrders();
            }
        });
    }

    private void cancelOrder(PreOrder po) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Cancel pre-order " + po.getOrderNumber() + "?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                preOrderDAO.cancel(po.getId());
                loadOrders();
            }
        });
    }

    private void showNewOrderDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("New Pre-Order");

        VBox form = new VBox(14);
        form.setPadding(new Insets(20));
        form.getStyleClass().add("card");

        ComboBox<Customer> customerCombo = new ComboBox<>();
        List<Customer> customers = customerDAO.findAll();
        customerCombo.setItems(FXCollections.observableArrayList(customers));
        customerCombo.setEditable(true);
        customerCombo.setPromptText("Select customer (or type a walk-in name)");
        customerCombo.setMaxWidth(Double.MAX_VALUE);

        Label phoneLabel = new Label();
        phoneLabel.getStyleClass().add("sub-label");
        customerCombo.valueProperty().addListener((obs, o, n) -> {
            Customer c = n;
            if (c != null) {
                phoneLabel.setText(c.getPhone() != null && !c.getPhone().isEmpty() ? "📞 " + c.getPhone() : "");
            } else {
                phoneLabel.setText("");
            }
        });

        // Items table
        ObservableList<PreOrderItem> cartItems = FXCollections.observableArrayList();
        TableView<PreOrderItem> itemTable = new TableView<>();
        itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        itemTable.setPrefHeight(200);

        TableColumn<PreOrderItem, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

        TableColumn<PreOrderItem, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<PreOrderItem, Double> priceCol = new TableColumn<>("Unit Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));

        TableColumn<PreOrderItem, String> totalCol = new TableColumn<>("Line Total");
        totalCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().getTotal())));

        itemTable.getColumns().addAll(nameCol, qtyCol, priceCol, totalCol);
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

        TextField priceField = new TextField();
        priceField.setPromptText("Unit price");
        priceField.setPrefWidth(100);

        productCombo.valueProperty().addListener((obs, o, n) -> {
            if (n != null) priceField.setText(String.valueOf(n.getSellPrice()));
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
            double price;
            try {
                qty = Integer.parseInt(qtyField.getText().trim());
                price = Double.parseDouble(priceField.getText().trim());
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.ERROR, "Enter a valid quantity and unit price!").showAndWait();
                return;
            }
            if (qty <= 0 || price < 0) {
                new Alert(Alert.AlertType.ERROR, "Quantity must be positive and price non-negative!").showAndWait();
                return;
            }
            cartItems.add(new PreOrderItem(p.getId(), p.getName(), p.getBarcode(), qty, price));
            productCombo.setValue(null);
            qtyField.setText("1");
            priceField.clear();
            updateTotalLabel(itemTable, form);
        });
        addRow.getChildren().addAll(productCombo, qtyField, priceField, addItemBtn);

        Label totalLabel = new Label();
        totalLabel.getStyleClass().add("section-title");
        updateTotalLabel(itemTable, totalLabel);

        HBox advancePickupRow = new HBox(12);
        TextField advanceField = new TextField("0");
        advanceField.setPromptText("Advance (₹)");
        advanceField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(advanceField, Priority.ALWAYS);

        DatePicker pickupPicker = new DatePicker(LocalDate.now().plusDays(7));
        pickupPicker.setPromptText("Expected pickup");
        pickupPicker.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(pickupPicker, Priority.ALWAYS);

        advancePickupRow.getChildren().addAll(advanceField, pickupPicker);

        TextArea noteArea = new TextArea();
        noteArea.setPromptText("Notes (optional)");
        noteArea.setPrefRowCount(2);

        Button createBtn = new Button("Create Pre-Order");
        createBtn.getStyleClass().add("btn-primary");
        createBtn.setMaxWidth(Double.MAX_VALUE);
        createBtn.setOnAction(e -> {
            if (cartItems.isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "Add at least one product!").showAndWait();
                return;
            }
            double advance;
            try {
                advance = Double.parseDouble(advanceField.getText().trim());
            } catch (NumberFormatException ex) {
                advance = 0;
            }
            if (advance < 0) {
                new Alert(Alert.AlertType.ERROR, "Advance cannot be negative!").showAndWait();
                return;
            }

            PreOrder po = new PreOrder();
            Customer selected = customerCombo.getValue();
            if (selected != null) {
                po.setCustomerId(selected.getId());
                po.setCustomerName(selected.getName());
                po.setCustomerPhone(selected.getPhone() != null ? selected.getPhone() : "");
            } else {
                String name = customerCombo.getEditor().getText().trim();
                if (name.isEmpty()) name = "Walk-in Customer";
                po.setCustomerId(0);
                po.setCustomerName(name);
                po.setCustomerPhone("");
            }
            po.setItems(new ArrayList<>(cartItems));
            po.computeTotal();
            po.setAdvancePaid(advance);
            po.setPickupDate(pickupPicker.getValue());
            po.setNote(noteArea.getText().trim());
            po.setStatus(PreOrder.STATUS_PENDING);

            boolean ok = preOrderDAO.create(po);
            Alert info = new Alert(ok ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                    ok ? "Pre-order " + po.getOrderNumber() + " created for ₹"
                            + String.format("%.2f", po.getTotalAmount()) : "Could not create pre-order.");
            info.showAndWait();
            dialog.close();
            loadOrders();
        });

        form.getChildren().addAll(
                new Label("Customer"), customerCombo, phoneLabel,
                new Label("Items"), itemTable,
                addRow, totalLabel,
                new Label("Advance & Pickup"), advancePickupRow,
                new Label("Notes"), noteArea,
                createBtn);

        Scene scene = new Scene(form, 680, 720);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.show();
    }

    private void updateTotalLabel(TableView<PreOrderItem> itemTable, Label totalLabel) {
        double total = 0;
        for (PreOrderItem item : itemTable.getItems()) total += item.getTotal();
        totalLabel.setText("Total: ₹" + String.format("%.2f", total));
    }

    private void updateTotalLabel(TableView<PreOrderItem> itemTable, VBox form) {
        for (Node child : form.getChildren()) {
            if (child instanceof Label label && label.getText().startsWith("Total:")) {
                updateTotalLabel(itemTable, label);
                return;
            }
        }
    }

    private void showDetails(PreOrder po) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Pre-Order " + po.getOrderNumber());

        VBox form = new VBox(12);
        form.setPadding(new Insets(20));
        form.getStyleClass().add("card");

        Label header = new Label(po.getOrderNumber() + " — " + po.getCustomerName());
        header.getStyleClass().add("section-title");

        String phone = po.getCustomerPhone() != null && !po.getCustomerPhone().isEmpty()
                ? "Phone: " + po.getCustomerPhone() : "";
        Label customerLabel = new Label("Customer: " + po.getCustomerName()
                + (phone.isEmpty() ? "" : "  •  " + phone));
        Label statusLabel = new Label("Status: " + po.getStatus());
        Label createdLabel = new Label("Created: " + po.getCreatedAtLabel());
        Label pickupLabel = new Label("Expected Pickup: " + po.getPickupDateLabel());
        Label invoiceLabel = po.getSaleInvoiceNumber() != null
                ? new Label("Sale Invoice: " + po.getSaleInvoiceNumber()) : new Label();

        TableView<PreOrderItem> itemTable = new TableView<>();
        itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        itemTable.setPrefHeight(220);

        TableColumn<PreOrderItem, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

        TableColumn<PreOrderItem, String> barcodeCol = new TableColumn<>("Barcode");
        barcodeCol.setCellValueFactory(new PropertyValueFactory<>("barcode"));

        TableColumn<PreOrderItem, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<PreOrderItem, Double> priceCol = new TableColumn<>("Unit Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));

        TableColumn<PreOrderItem, String> totalCol = new TableColumn<>("Line Total");
        totalCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().getTotal())));

        itemTable.getColumns().addAll(nameCol, barcodeCol, qtyCol, priceCol, totalCol);
        itemTable.setItems(FXCollections.observableArrayList(po.getItems()));

        Label totalLabel = new Label("Total: ₹" + String.format("%.2f", po.getTotalAmount())
                + "   •   Advance: ₹" + String.format("%.2f", po.getAdvancePaid())
                + "   •   Balance Due: ₹" + String.format("%.2f", po.getBalanceDue()));
        totalLabel.getStyleClass().add("section-title");

        if (po.getNote() != null && !po.getNote().isEmpty()) {
            form.getChildren().add(new Label("Note: " + po.getNote()));
        }

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-secondary");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> dialog.close());

        form.getChildren().addAll(header, customerLabel, statusLabel, createdLabel, pickupLabel,
                invoiceLabel, itemTable, totalLabel, closeBtn);

        Scene scene = new Scene(form, 640, 540);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.show();
    }
}
