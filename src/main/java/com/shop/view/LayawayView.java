package com.shop.view;

import com.shop.dao.CustomerDAO;
import com.shop.dao.LayawayDAO;
import com.shop.dao.ProductDAO;
import com.shop.model.Customer;
import com.shop.model.Layaway;
import com.shop.model.LayawayItem;
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
import java.util.List;
import java.util.Optional;

public class LayawayView {
    private final LayawayDAO layawayDAO = new LayawayDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final ObservableList<Layaway> layawayList = FXCollections.observableArrayList();
    private final TableView<Layaway> table = new TableView<>();
    private final Label statsLabel = new Label();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label title = new Label("🛒 Layaway Plans (Installments)");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Hold items for customers who pay in easy installments.");
        subtitle.getStyleClass().add("sub-label");
        statsLabel.getStyleClass().add("sub-label");
        headerCard.getChildren().addAll(title, subtitle, statsLabel);

        Button newPlanBtn = new Button("➕  New Layaway Plan");
        newPlanBtn.getStyleClass().add("btn-primary");
        newPlanBtn.setMaxWidth(Double.MAX_VALUE);
        newPlanBtn.setOnAction(e -> showNewPlanDialog());

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Layaway, String> numCol = new TableColumn<>("Plan No.");
        numCol.setCellValueFactory(new PropertyValueFactory<>("planNumber"));

        TableColumn<Layaway, String> custCol = new TableColumn<>("Customer");
        custCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getCustomerName() != null
                ? p.getValue().getCustomerName() : "—"));

        TableColumn<Layaway, String> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().getTotalAmount())));

        TableColumn<Layaway, String> paidCol = new TableColumn<>("Paid");
        paidCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().getAmountPaid())));
        paidCol.setCellFactory(col -> new TableCell<Layaway, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                if (!empty && item != null) getStyleClass().add("cell-accent");
            }
        });

        TableColumn<Layaway, String> balanceCol = new TableColumn<>("Balance");
        balanceCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().getBalanceDue())));

        TableColumn<Layaway, String> progressCol = new TableColumn<>("Installments");
        progressCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getInstallmentsPaid()
                + " / " + p.getValue().getInstallmentsCount()));

        TableColumn<Layaway, String> dueCol = new TableColumn<>("Due Date");
        dueCol.setCellValueFactory(new PropertyValueFactory<>("dueDateLabel"));

        TableColumn<Layaway, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<Layaway, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add(Layaway.STATUS_COMPLETED.equals(item)
                            ? "badge-active" : Layaway.STATUS_ACTIVE.equals(item)
                            ? "badge-warning" : "badge-inactive");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Layaway, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(240);
        actionCol.setCellFactory(col -> new TableCell<Layaway, Void>() {
            private final Button payBtn = new Button("💰 Collect");
            private final Button viewBtn = new Button("👁 View");
            private final Button cancelBtn = new Button("🚫 Cancel");
            private final HBox box = new HBox(6, payBtn, viewBtn, cancelBtn);
            {
                payBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                viewBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                cancelBtn.getStyleClass().addAll("btn-danger", "btn-small");
                payBtn.setOnAction(e -> recordPayment(getTableRow().getItem()));
                viewBtn.setOnAction(e -> showDetails(getTableRow().getItem()));
                cancelBtn.setOnAction(e -> cancel(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    boolean active = getTableRow().getItem().isActive();
                    payBtn.setDisable(!active);
                    cancelBtn.setDisable(!active);
                    setGraphic(box);
                }
            }
        });

        table.getColumns().addAll(numCol, custCol, totalCol, paidCol, balanceCol, progressCol, dueCol, statusCol, actionCol);
        table.setItems(layawayList);
        table.setPlaceholder(new Label("No layaway plans yet."));

        root.getChildren().addAll(headerCard, newPlanBtn, table);
        refresh();
        return root;
    }

    private void showNewPlanDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("New Layaway Plan");

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
            if (n != null && n.getPhone() != null && !n.getPhone().isEmpty()) {
                phoneLabel.setText("📞 " + n.getPhone());
            } else {
                phoneLabel.setText("");
            }
        });

        ObservableList<LayawayItem> cartItems = FXCollections.observableArrayList();
        TableView<LayawayItem> itemTable = new TableView<>();
        itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        itemTable.setPrefHeight(180);

        TableColumn<LayawayItem, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

        TableColumn<LayawayItem, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<LayawayItem, Double> priceCol = new TableColumn<>("Unit Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));

        TableColumn<LayawayItem, String> totalCol = new TableColumn<>("Line Total");
        totalCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().getTotal())));

        itemTable.getColumns().addAll(nameCol, qtyCol, priceCol, totalCol);
        itemTable.setItems(cartItems);

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
            cartItems.add(new LayawayItem(p.getId(), p.getName(), p.getBarcode(), qty, price));
            productCombo.setValue(null);
            qtyField.setText("1");
            priceField.clear();
        });
        addRow.getChildren().addAll(productCombo, qtyField, priceField, addItemBtn);

        Label totalLabel = new Label("Total: ₹0.00");
        totalLabel.getStyleClass().add("section-title");
        cartItems.addListener((javafx.collections.ListChangeListener<LayawayItem>) c -> {
            double total = 0;
            for (LayawayItem item : cartItems) total += item.getTotal();
            totalLabel.setText("Total: ₹" + String.format("%.2f", total));
        });

        HBox planRow = new HBox(12);
        planRow.setAlignment(Pos.CENTER_LEFT);

        TextField downField = new TextField("0");
        downField.setPromptText("Down payment (₹)");
        downField.setPrefWidth(140);

        Spinner<Integer> installmentsSpinner = new Spinner<>(1, 24, 4);
        installmentsSpinner.setPrefWidth(90);

        DatePicker duePicker = new DatePicker(LocalDate.now().plusMonths(4));
        duePicker.setPrefWidth(140);

        planRow.getChildren().addAll(downField, installmentsSpinner, duePicker);

        TextField noteField = new TextField();
        noteField.setPromptText("Note (optional)");
        noteField.setMaxWidth(Double.MAX_VALUE);

        form.getChildren().addAll(
                new Label("👤 Customer"), customerCombo, phoneLabel,
                new Label("🛒 Items"), addRow, itemTable,
                new Label("💸 Plan Details"), planRow, noteField, totalLabel);

        Button saveBtn = new Button("💾  Save Layaway Plan");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> {
            if (cartItems.isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "Add at least one item!").showAndWait();
                return;
            }
            double down;
            try {
                down = Double.parseDouble(downField.getText().trim());
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.ERROR, "Enter a valid down payment!").showAndWait();
                return;
            }
            Layaway lay = new Layaway();
            Customer c = customerCombo.getValue();
            if (c != null) {
                lay.setCustomerId(c.getId());
                lay.setCustomerName(c.getName());
                lay.setCustomerPhone(c.getPhone());
            } else {
                String typed = customerCombo.getEditor().getText();
                lay.setCustomerName(typed != null && !typed.trim().isEmpty() ? typed.trim() : "Walk-in");
            }
            lay.setItems(cartItems);
            lay.setDownPayment(Math.round(down * 100.0) / 100.0);
            lay.setAmountPaid(lay.getDownPayment());
            lay.setInstallmentsCount(installmentsSpinner.getValue());
            lay.setInstallmentsPaid(lay.getDownPayment() > 0 ? 1 : 0);
            lay.setInstallmentAmount(Math.max(0, (lay.getTotalAmount() - lay.getDownPayment())
                    / Math.max(1, lay.getInstallmentsCount())));
            lay.setDueDate(duePicker.getValue());
            lay.setNote(noteField.getText().trim());
            if (layawayDAO.create(lay)) {
                new Alert(Alert.AlertType.INFORMATION, "Layaway plan " + lay.getPlanNumber() + " created!\n\n"
                        + "Customer: " + lay.getCustomerName() + "\n"
                        + "Total: ₹" + String.format("%.2f", lay.getTotalAmount()) + "\n"
                        + "Installment: ₹" + String.format("%.2f", lay.getInstallmentAmount()) + " x "
                        + lay.getInstallmentsCount()).showAndWait();
                dialog.close();
                refresh();
            } else {
                new Alert(Alert.AlertType.ERROR, "Failed to create layaway plan.").showAndWait();
            }
        });
        form.getChildren().add(saveBtn);

        Scene scene = new Scene(form, 620, 640);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void recordPayment(Layaway lay) {
        if (lay == null || !lay.isActive()) return;
        TextInputDialog input = new TextInputDialog(String.format("%.2f", lay.getInstallmentAmount()));
        input.setTitle("Collect Installment");
        input.setHeaderText(lay.getCustomerName() + " — balance ₹" + String.format("%.2f", lay.getBalanceDue())
                + " (installment ₹" + String.format("%.2f", lay.getInstallmentAmount()) + ")");
        input.setContentText("Amount received (₹):");
        Optional<String> result = input.showAndWait();
        result.ifPresent(val -> {
            try {
                double amount = Double.parseDouble(val.trim());
                if (amount <= 0) {
                    new Alert(Alert.AlertType.ERROR, "Amount must be positive.").showAndWait();
                    return;
                }
                if (layawayDAO.recordPayment(lay.getId(), amount)) {
                    refresh();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Could not record payment.").showAndWait();
                }
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.ERROR, "Enter a valid amount.").showAndWait();
            }
        });
    }

    private void cancel(Layaway lay) {
        if (lay == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Cancel layaway plan " + lay.getPlanNumber() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                layawayDAO.cancel(lay.getId());
                refresh();
            }
        });
    }

    private void showDetails(Layaway lay) {
        if (lay == null) return;
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Layaway " + lay.getPlanNumber());

        VBox form = new VBox(14);
        form.setPadding(new Insets(20));
        form.getStyleClass().add("card");

        Label header = new Label("🛒 Layaway " + lay.getPlanNumber() + "  •  " + lay.getStatus());
        header.getStyleClass().add("section-title");

        Label info = new Label("Customer: " + lay.getCustomerName()
                + (lay.getCustomerPhone() != null && !lay.getCustomerPhone().isEmpty() ? "  •  📞 " + lay.getCustomerPhone() : "")
                + "\nStarted: " + lay.getCreatedAtLabel()
                + "\nDue date: " + lay.getDueDateLabel()
                + (lay.getNote() != null && !lay.getNote().isEmpty() ? "\nNote: " + lay.getNote() : ""));
        info.setWrapText(true);
        info.getStyleClass().add("sub-label");

        TableView<LayawayItem> itemTable = new TableView<>();
        itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        itemTable.setPrefHeight(200);

        TableColumn<LayawayItem, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

        TableColumn<LayawayItem, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<LayawayItem, Double> priceCol = new TableColumn<>("Unit Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));

        TableColumn<LayawayItem, String> totalCol = new TableColumn<>("Line Total");
        totalCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().getTotal())));

        itemTable.getColumns().addAll(nameCol, qtyCol, priceCol, totalCol);
        itemTable.setItems(FXCollections.observableArrayList(lay.getItems()));

        Label totals = new Label(String.format(
                "Total: ₹%.2f    Down Payment: ₹%.2f    Paid: ₹%.2f\nBalance Due: ₹%.2f\nInstallments: %d / %d (₹%.2f each)",
                lay.getTotalAmount(), lay.getDownPayment(), lay.getAmountPaid(),
                lay.getBalanceDue(), lay.getInstallmentsPaid(), lay.getInstallmentsCount(), lay.getInstallmentAmount()));
        totals.getStyleClass().add("section-title");

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-secondary");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> dialog.close());

        form.getChildren().addAll(header, info, itemTable, totals, closeBtn);

        Scene scene = new Scene(form, 560, 560);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void refresh() {
        layawayList.clear();
        layawayList.addAll(layawayDAO.findAll());
        statsLabel.setText("Active Plans: " + layawayDAO.countActive()
                + "  •  Total Plans: " + layawayDAO.count()
                + "  •  Outstanding: " + String.format("₹%.2f", layawayDAO.totalOutstanding()));
    }
}
