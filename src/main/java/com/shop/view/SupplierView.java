package com.shop.view;

import com.shop.dao.SupplierDAO;
import com.shop.model.Supplier;
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

public class SupplierView {
    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final ObservableList<Supplier> supplierList = FXCollections.observableArrayList();
    private final TableView<Supplier> table = new TableView<>();
    private final Label countLabel = new Label();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        HBox controlBar = new HBox(12);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search suppliers by company name, contact, or phone...");
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadSuppliers(newVal.trim()));

        countLabel.getStyleClass().add("sub-label");

        Button addSupplierBtn = new Button("➕  Add Supplier");
        addSupplierBtn.getStyleClass().add("btn-primary");
        addSupplierBtn.setOnAction(e -> showSupplierDialog(null));

        controlBar.getChildren().addAll(searchField, addSupplierBtn, countLabel);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Supplier, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setMaxWidth(60);

        TableColumn<Supplier, String> companyCol = new TableColumn<>("Company Name");
        companyCol.setCellValueFactory(new PropertyValueFactory<>("companyName"));

        TableColumn<Supplier, String> contactCol = new TableColumn<>("Contact Person");
        contactCol.setCellValueFactory(new PropertyValueFactory<>("contactPerson"));

        TableColumn<Supplier, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));

        TableColumn<Supplier, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<Supplier, String> addressCol = new TableColumn<>("Address");
        addressCol.setCellValueFactory(new PropertyValueFactory<>("address"));

        TableColumn<Supplier, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<Supplier, Void>() {
            private final Button editBtn = new Button("✏️");
            private final Button delBtn = new Button("🗑️");
            private final HBox btnBox = new HBox(6, editBtn, delBtn);
            {
                editBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                delBtn.getStyleClass().addAll("btn-danger", "btn-small");

                editBtn.setOnAction(e -> {
                    Supplier s = getTableView().getItems().get(getIndex());
                    showSupplierDialog(s);
                });

                delBtn.setOnAction(e -> {
                    Supplier s = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete supplier '" + s.getCompanyName() + "'?", ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(resp -> {
                        if (resp == ButtonType.YES) {
                            supplierDAO.delete(s.getId());
                            loadSuppliers("");
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

        table.getColumns().addAll(idCol, companyCol, contactCol, phoneCol, emailCol, addressCol, actionCol);
        table.setItems(supplierList);
        table.setPlaceholder(new Label("No supplier companies added yet."));

        root.getChildren().addAll(controlBar, table);
        loadSuppliers("");
        return root;
    }

    private void loadSuppliers(String query) {
        supplierList.clear();
        if (query.isEmpty()) {
            supplierList.addAll(supplierDAO.findAll());
        } else {
            supplierList.addAll(supplierDAO.search(query));
        }
        countLabel.setText(supplierList.size() + " supplier(s)");
    }

    private void showSupplierDialog(Supplier supplierToEdit) {
        boolean isEdit = (supplierToEdit != null);
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(isEdit ? "Edit Supplier" : "Add Supplier");

        VBox form = new VBox(14);
        form.setPadding(new Insets(20));
        form.getStyleClass().add("card");

        TextField companyField = new TextField(isEdit ? supplierToEdit.getCompanyName() : "");
        companyField.setPromptText("Company Name");

        TextField contactField = new TextField(isEdit ? supplierToEdit.getContactPerson() : "");
        contactField.setPromptText("Contact Person");

        TextField phoneField = new TextField(isEdit ? supplierToEdit.getPhone() : "");
        phoneField.setPromptText("Phone Number");

        TextField emailField = new TextField(isEdit ? supplierToEdit.getEmail() : "");
        emailField.setPromptText("Email Address");

        TextArea addressArea = new TextArea(isEdit ? supplierToEdit.getAddress() : "");
        addressArea.setPromptText("Office Address");
        addressArea.setPrefRowCount(3);

        Button saveBtn = new Button(isEdit ? "Update Supplier" : "Save Supplier");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setMaxWidth(Double.MAX_VALUE);

        saveBtn.setOnAction(e -> {
            String company = companyField.getText().trim();
            if (company.isEmpty()) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Company name is required!");
                a.showAndWait();
                return;
            }

            Supplier s = isEdit ? supplierToEdit : new Supplier();
            s.setCompanyName(company);
            s.setContactPerson(contactField.getText().trim());
            s.setPhone(phoneField.getText().trim());
            s.setEmail(emailField.getText().trim());
            s.setAddress(addressArea.getText().trim());

            if (isEdit) supplierDAO.update(s);
            else supplierDAO.insert(s);

            dialog.close();
            loadSuppliers("");
        });

        form.getChildren().addAll(
                new Label("Company Name"), companyField,
                new Label("Contact Person"), contactField,
                new Label("Phone Number"), phoneField,
                new Label("Email"), emailField,
                new Label("Address"), addressArea,
                saveBtn
        );

        Scene scene = new Scene(form, 400, 520);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.show();
    }
}
