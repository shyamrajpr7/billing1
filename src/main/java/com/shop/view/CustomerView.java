package com.shop.view;

import com.shop.dao.CustomerDAO;
import com.shop.model.Customer;
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

public class CustomerView {
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final ObservableList<Customer> customerList = FXCollections.observableArrayList();
    private final TableView<Customer> table = new TableView<>();

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

        Button addCustomerBtn = new Button("➕  Add Customer");
        addCustomerBtn.getStyleClass().add("btn-primary");
        addCustomerBtn.setOnAction(e -> showCustomerDialog(null));

        controlBar.getChildren().addAll(searchField, addCustomerBtn);

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
            private final Button editBtn = new Button("✏️");
            private final Button delBtn = new Button("🗑️");
            private final HBox btnBox = new HBox(6, editBtn, delBtn);
            {
                editBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                delBtn.getStyleClass().addAll("btn-danger", "btn-small");

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

        root.getChildren().addAll(controlBar, table);
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
}
