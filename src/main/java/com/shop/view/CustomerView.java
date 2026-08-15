package com.shop.view;

import com.shop.dao.CustomerDAO;
import com.shop.dao.SaleDAO;
import com.shop.model.Customer;
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

public class CustomerView {
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final SaleDAO saleDAO = new SaleDAO();
    private final ObservableList<Customer> customerList = FXCollections.observableArrayList();
    private final TableView<Customer> table = new TableView<>();
    private final Label countLabel = new Label();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        HBox controlBar = new HBox(12);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search customers by name, phone, or email...");
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadCustomers(newVal.trim()));

        countLabel.getStyleClass().add("sub-label");

        Button addCustomerBtn = new Button("➕  Add Customer");
        addCustomerBtn.getStyleClass().add("btn-primary");
        addCustomerBtn.setOnAction(e -> showCustomerDialog(null));

        controlBar.getChildren().addAll(searchField, addCustomerBtn, countLabel);

        List<Customer> allCustomers = customerDAO.findAll();
        int totalCustomers = allCustomers.size();
        int totalPoints = allCustomers.stream().mapToInt(Customer::getLoyaltyPoints).sum();
        double avgPoints = totalCustomers == 0 ? 0 : (double) totalPoints / totalCustomers;

        HBox statsBar = new HBox(14);
        VBox customersCard = createSummaryCard(String.valueOf(totalCustomers), "Total Customers");
        VBox pointsCard = createSummaryCard(String.valueOf(totalPoints), "Loyalty Points Issued");
        VBox avgCard = createSummaryCard(String.format("%.1f", avgPoints), "Avg Points / Customer");
        HBox.setHgrow(customersCard, Priority.ALWAYS);
        HBox.setHgrow(pointsCard, Priority.ALWAYS);
        HBox.setHgrow(avgCard, Priority.ALWAYS);
        statsBar.getChildren().addAll(customersCard, pointsCard, avgCard);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Customer, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setMaxWidth(60);

        TableColumn<Customer, String> nameCol = new TableColumn<>("Customer Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Customer, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));

        TableColumn<Customer, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<Customer, String> addressCol = new TableColumn<>("Address");
        addressCol.setCellValueFactory(new PropertyValueFactory<>("address"));

        TableColumn<Customer, Integer> pointsCol = new TableColumn<>("Loyalty Points");
        pointsCol.setCellValueFactory(new PropertyValueFactory<>("loyaltyPoints"));

        TableColumn<Customer, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<Customer, Void>() {
            private final Button histBtn = new Button("🧾");
            private final Button editBtn = new Button("✏️");
            private final Button delBtn = new Button("🗑️");
            private final HBox btnBox = new HBox(6, histBtn, editBtn, delBtn);
            {
                histBtn.getStyleClass().addAll("btn-primary", "btn-small");
                editBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                delBtn.getStyleClass().addAll("btn-danger", "btn-small");

                histBtn.setOnAction(e -> {
                    Customer c = getTableView().getItems().get(getIndex());
                    showPurchaseHistory(c);
                });

                editBtn.setOnAction(e -> {
                    Customer c = getTableView().getItems().get(getIndex());
                    showCustomerDialog(c);
                });

                delBtn.setOnAction(e -> {
                    Customer c = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete customer '" + c.getName() + "'?", ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(resp -> {
                        if (resp == ButtonType.YES) {
                            customerDAO.delete(c.getId());
                            loadCustomers("");
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(btnBox);
            }
        });

        table.getColumns().addAll(idCol, nameCol, phoneCol, emailCol, addressCol, pointsCol, actionCol);
        table.setItems(customerList);
        table.setPlaceholder(new Label("No customers registered yet."));

        root.getChildren().addAll(statsBar, controlBar, table);
        loadCustomers("");
        return root;
    }

    private void loadCustomers(String query) {
        customerList.clear();
        if (query.isEmpty()) {
            customerList.addAll(customerDAO.findAll());
        } else {
            customerList.addAll(customerDAO.search(query));
        }
        countLabel.setText(customerList.size() + " customer(s)");
    }

    private void showCustomerDialog(Customer customerToEdit) {
        boolean isEdit = (customerToEdit != null);
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(isEdit ? "Edit Customer" : "Add Customer");

        VBox form = new VBox(14);
        form.setPadding(new Insets(20));
        form.getStyleClass().add("card");

        TextField nameField = new TextField(isEdit ? customerToEdit.getName() : "");
        nameField.setPromptText("Full Name");

        TextField phoneField = new TextField(isEdit ? customerToEdit.getPhone() : "");
        phoneField.setPromptText("Phone Number");

        TextField emailField = new TextField(isEdit ? customerToEdit.getEmail() : "");
        emailField.setPromptText("Email Address");

        TextArea addressArea = new TextArea(isEdit ? customerToEdit.getAddress() : "");
        addressArea.setPromptText("Address");
        addressArea.setPrefRowCount(3);

        Button saveBtn = new Button(isEdit ? "Update Customer" : "Save Customer");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setMaxWidth(Double.MAX_VALUE);

        saveBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Name is required!");
                a.showAndWait();
                return;
            }

            Customer c = isEdit ? customerToEdit : new Customer();
            c.setName(name);
            c.setPhone(phoneField.getText().trim());
            c.setEmail(emailField.getText().trim());
            c.setAddress(addressArea.getText().trim());

            if (isEdit) customerDAO.update(c);
            else customerDAO.insert(c);

            dialog.close();
            loadCustomers("");
        });

        form.getChildren().addAll(
                new Label("Customer Name"), nameField,
                new Label("Phone Number"), phoneField,
                new Label("Email"), emailField,
                new Label("Address"), addressArea,
                saveBtn
        );

        Scene scene = new Scene(form, 400, 480);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.show();
    }

    private void showPurchaseHistory(Customer customer) {
        List<Sale> sales = saleDAO.findByCustomerId(customer.getId());
        sales.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Purchase History - " + customer.getName());

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("card");

        Label header = new Label("🧾 Purchase History: " + customer.getName());
        header.getStyleClass().add("section-title");

        double totalSpent = sales.stream().mapToDouble(Sale::getTotal).sum();

        HBox summaryBar = new HBox(14);
        VBox countCard = createSummaryCard(String.valueOf(sales.size()), "Total Purchases");
        VBox spentCard = createSummaryCard(String.format("₹%.2f", totalSpent), "Total Spent");
        VBox avgCard = createSummaryCard(sales.isEmpty() ? "₹0.00" : String.format("₹%.2f", totalSpent / sales.size()), "Avg Per Purchase");
        HBox.setHgrow(countCard, Priority.ALWAYS);
        HBox.setHgrow(spentCard, Priority.ALWAYS);
        HBox.setHgrow(avgCard, Priority.ALWAYS);
        summaryBar.getChildren().addAll(countCard, spentCard, avgCard);

        TableView<Sale> saleTable = new TableView<>(FXCollections.observableArrayList(sales));
        saleTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(saleTable, Priority.ALWAYS);

        TableColumn<Sale, String> invCol = new TableColumn<>("Invoice #");
        invCol.setCellValueFactory(new PropertyValueFactory<>("invoiceNumber"));

        TableColumn<Sale, String> dateCol = new TableColumn<>("Date & Time");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("formattedDate"));

        TableColumn<Sale, String> payCol = new TableColumn<>("Payment");
        payCol.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));

        TableColumn<Sale, String> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getFormattedTotal()));

        TableColumn<Sale, Void> detailCol = new TableColumn<>("");
        detailCol.setCellFactory(col -> new TableCell<Sale, Void>() {
            private final Button viewBtn = new Button("👁️ Details");
            {
                viewBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                viewBtn.setOnAction(e -> {
                    Sale sale = getTableView().getItems().get(getIndex());
                    showSaleItems(sale);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewBtn);
            }
        });

        saleTable.getColumns().addAll(invCol, dateCol, payCol, totalCol, detailCol);
        saleTable.setPlaceholder(new Label("No purchases found for this customer."));

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-secondary");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> dialog.close());

        root.getChildren().addAll(header, summaryBar, saleTable, closeBtn);

        Scene scene = new Scene(root, 640, 480);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.show();
    }

    private VBox createSummaryCard(String value, String label) {
        VBox card = new VBox(6);
        card.getStyleClass().add("stat-card");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().addAll("stat-value", "text-accent");
        Label labelL = new Label(label);
        labelL.getStyleClass().add("stat-label");
        card.getChildren().addAll(valueLabel, labelL);
        return card;
    }

    private void showSaleItems(Sale sale) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Items - " + sale.getInvoiceNumber());

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("card");

        Label header = new Label("Items in " + sale.getInvoiceNumber());
        header.getStyleClass().add("section-title");

        TableView<SaleItem> itemTable = new TableView<>(FXCollections.observableArrayList(sale.getItems()));
        itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        itemTable.setPrefHeight(220);

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
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> dialog.close());

        root.getChildren().addAll(header, itemTable, summary, closeBtn);

        Scene scene = new Scene(root, 480, 420);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.show();
    }
}
