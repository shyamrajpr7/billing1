package com.shop.view;

import com.shop.dao.CustomerDAO;
import com.shop.dao.ProductDAO;
import com.shop.dao.QuotationDAO;
import com.shop.model.Customer;
import com.shop.model.Product;
import com.shop.model.Quotation;
import com.shop.model.QuotationItem;
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

public class QuotationsView {
    private final QuotationDAO quotationDAO = new QuotationDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final ObservableList<Quotation> quotationList = FXCollections.observableArrayList();
    private final TableView<Quotation> table = new TableView<>();
    private final Label statsLabel = new Label();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label title = new Label("📄 Quotations & Estimates");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Create price quotes for customers before they commit to a purchase.");
        subtitle.getStyleClass().add("sub-label");
        statsLabel.getStyleClass().add("sub-label");
        headerCard.getChildren().addAll(title, subtitle, statsLabel);

        Button newQuoteBtn = new Button("➕  New Quotation");
        newQuoteBtn.getStyleClass().add("btn-primary");
        newQuoteBtn.setMaxWidth(Double.MAX_VALUE);
        newQuoteBtn.setOnAction(e -> showNewQuotationDialog());

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Quotation, String> numCol = new TableColumn<>("Quote No.");
        numCol.setCellValueFactory(new PropertyValueFactory<>("quoteNumber"));

        TableColumn<Quotation, String> custCol = new TableColumn<>("Customer");
        custCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getCustomerName() != null
                ? p.getValue().getCustomerName() : "—"));

        TableColumn<Quotation, String> itemsCol = new TableColumn<>("Items");
        itemsCol.setCellValueFactory(p -> new SimpleStringProperty(String.valueOf(p.getValue().getItemCount())));

        TableColumn<Quotation, String> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("formattedTotal"));
        totalCol.setCellFactory(col -> new TableCell<Quotation, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                if (!empty && item != null) getStyleClass().add("cell-accent");
            }
        });

        TableColumn<Quotation, String> validCol = new TableColumn<>("Valid Until");
        validCol.setCellValueFactory(new PropertyValueFactory<>("validUntilLabel"));

        TableColumn<Quotation, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<Quotation, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add(switch (item) {
                        case Quotation.STATUS_ACCEPTED -> "badge-active";
                        case Quotation.STATUS_REJECTED, Quotation.STATUS_EXPIRED -> "badge-inactive";
                        default -> "badge-warning";
                    });
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Quotation, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(240);
        actionCol.setCellFactory(col -> new TableCell<Quotation, Void>() {
            private final Button viewBtn = new Button("👁 View");
            private final Button acceptBtn = new Button("✅ Accept");
            private final Button rejectBtn = new Button("🚫 Reject");
            private final HBox box = new HBox(6, viewBtn, acceptBtn, rejectBtn);
            {
                viewBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                acceptBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                rejectBtn.getStyleClass().addAll("btn-danger", "btn-small");
                viewBtn.setOnAction(e -> showDetails(getTableRow().getItem()));
                acceptBtn.setOnAction(e -> setStatus(getTableRow().getItem(), Quotation.STATUS_ACCEPTED));
                rejectBtn.setOnAction(e -> setStatus(getTableRow().getItem(), Quotation.STATUS_REJECTED));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    String status = getTableRow().getItem().getStatus();
                    boolean open = Quotation.STATUS_PENDING.equals(status) || Quotation.STATUS_DRAFT.equals(status);
                    acceptBtn.setDisable(!open);
                    rejectBtn.setDisable(!open);
                    setGraphic(box);
                }
            }
        });

        table.getColumns().addAll(numCol, custCol, itemsCol, totalCol, validCol, statusCol, actionCol);
        table.setItems(quotationList);
        table.setPlaceholder(new Label("No quotations yet."));

        root.getChildren().addAll(headerCard, newQuoteBtn, table);
        refresh();
        return root;
    }

    private void showNewQuotationDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("New Quotation");

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

        ObservableList<QuotationItem> cartItems = FXCollections.observableArrayList();
        TableView<QuotationItem> itemTable = new TableView<>();
        itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        itemTable.setPrefHeight(200);

        TableColumn<QuotationItem, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

        TableColumn<QuotationItem, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<QuotationItem, Double> priceCol = new TableColumn<>("Unit Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));

        TableColumn<QuotationItem, String> totalCol = new TableColumn<>("Line Total");
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
            cartItems.add(new QuotationItem(p.getId(), p.getName(), p.getBarcode(), qty, price));
            productCombo.setValue(null);
            qtyField.setText("1");
            priceField.clear();
        });

        addRow.getChildren().addAll(productCombo, qtyField, priceField, addItemBtn);

        Label totalLabel = new Label("Total: ₹0.00");
        totalLabel.getStyleClass().add("section-title");
        cartItems.addListener((javafx.collections.ListChangeListener<QuotationItem>) c -> {
            double total = 0;
            for (QuotationItem item : cartItems) total += item.getTotal();
            totalLabel.setText("Total: ₹" + String.format("%.2f", total));
        });

        HBox detailsRow = new HBox(12);
        detailsRow.setAlignment(Pos.CENTER_LEFT);

        TextField discountField = new TextField("0");
        discountField.setPromptText("Discount (₹)");
        discountField.setPrefWidth(110);

        TextField taxField = new TextField("0");
        taxField.setPromptText("Tax (₹)");
        taxField.setPrefWidth(110);

        DatePicker validPicker = new DatePicker(LocalDate.now().plusDays(7));
        validPicker.setPrefWidth(140);

        TextField noteField = new TextField();
        noteField.setPromptText("Note (optional)");
        noteField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(noteField, Priority.ALWAYS);

        detailsRow.getChildren().addAll(discountField, taxField, validPicker);
        form.getChildren().addAll(
                new Label("👤 Customer"), customerCombo, phoneLabel,
                new Label("🛒 Items"), addRow, itemTable, detailsRow, noteField, totalLabel);

        Button saveBtn = new Button("💾  Save Quotation");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> {
            if (cartItems.isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "Add at least one item!").showAndWait();
                return;
            }
            Quotation q = new Quotation();
            Customer c = customerCombo.getValue();
            if (c != null) {
                q.setCustomerId(c.getId());
                q.setCustomerName(c.getName());
                q.setCustomerPhone(c.getPhone());
            } else {
                String typed = customerCombo.getEditor().getText();
                q.setCustomerName(typed != null && !typed.trim().isEmpty() ? typed.trim() : "Walk-in");
            }
            q.setItems(cartItems);
            try {
                q.setDiscountAmount(Double.parseDouble(discountField.getText().trim()));
                q.setTax(Double.parseDouble(taxField.getText().trim()));
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.ERROR, "Enter valid discount and tax amounts!").showAndWait();
                return;
            }
            q.setValidUntil(validPicker.getValue());
            q.setNote(noteField.getText().trim());
            if (quotationDAO.create(q)) {
                new Alert(Alert.AlertType.INFORMATION, "Quotation " + q.getQuoteNumber() + " created successfully!\n\n"
                        + "Customer: " + q.getCustomerName() + "\n"
                        + "Total: " + q.getFormattedTotal() + "\n"
                        + "Valid until: " + q.getValidUntilLabel()).showAndWait();
                dialog.close();
                refresh();
            } else {
                new Alert(Alert.AlertType.ERROR, "Failed to create quotation.").showAndWait();
            }
        });
        form.getChildren().add(saveBtn);

        Scene scene = new Scene(form, 640, 640);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void setStatus(Quotation q, String status) {
        if (q == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Mark quotation " + q.getQuoteNumber() + " as " + status + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                quotationDAO.updateStatus(q.getId(), status);
                refresh();
            }
        });
    }

    private void showDetails(Quotation q) {
        if (q == null) return;
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Quotation " + q.getQuoteNumber());

        VBox form = new VBox(14);
        form.setPadding(new Insets(20));
        form.getStyleClass().add("card");

        Label header = new Label("📄 Quotation " + q.getQuoteNumber() + "  •  " + q.getStatus());
        header.getStyleClass().add("section-title");

        Label info = new Label("Customer: " + q.getCustomerName()
                + (q.getCustomerPhone() != null && !q.getCustomerPhone().isEmpty() ? "  •  📞 " + q.getCustomerPhone() : "")
                + "\nCreated: " + q.getCreatedAtLabel()
                + "\nValid until: " + q.getValidUntilLabel()
                + (q.getNote() != null && !q.getNote().isEmpty() ? "\nNote: " + q.getNote() : ""));
        info.setWrapText(true);
        info.getStyleClass().add("sub-label");

        TableView<QuotationItem> itemTable = new TableView<>();
        itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        itemTable.setPrefHeight(220);

        TableColumn<QuotationItem, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

        TableColumn<QuotationItem, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<QuotationItem, Double> priceCol = new TableColumn<>("Unit Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));

        TableColumn<QuotationItem, String> totalCol = new TableColumn<>("Line Total");
        totalCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().getTotal())));

        itemTable.getColumns().addAll(nameCol, qtyCol, priceCol, totalCol);
        itemTable.setItems(FXCollections.observableArrayList(q.getItems()));

        Label totals = new Label(String.format("Subtotal: ₹%.2f\nDiscount: ₹%.2f\nTax: ₹%.2f",
                q.getSubtotal(), q.getDiscountAmount(), q.getTax())
                + "\n\nTotal: " + q.getFormattedTotal());
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
        quotationList.clear();
        quotationList.addAll(quotationDAO.findAll());
        statsLabel.setText("Pending Quotes: " + quotationDAO.countPending()
                + "  •  Total Quotes: " + quotationDAO.count()
                + "  •  Open Value: " + String.format("₹%.2f", quotationDAO.totalPendingValue()));
    }
}
