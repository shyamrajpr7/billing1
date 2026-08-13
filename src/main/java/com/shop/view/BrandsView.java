package com.shop.view;

import com.shop.dao.BrandDAO;
import com.shop.model.Brand;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

public class BrandsView {
    private final BrandDAO brandDAO = new BrandDAO();
    private final ObservableList<Brand> brandList = FXCollections.observableArrayList();
    private final TableView<Brand> table = new TableView<>();
    private final Label statsLabel = new Label();

    private final TextField nameField = new TextField();
    private final TextField manufacturerField = new TextField();
    private final TextField originField = new TextField();
    private final TextField contactField = new TextField();
    private final TextArea descriptionArea = new TextArea();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label title = new Label("🏷️ Brand Management");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Maintain a catalog of product brands and their manufacturers.");
        subtitle.getStyleClass().add("sub-label");
        statsLabel.getStyleClass().add("sub-label");
        headerCard.getChildren().addAll(title, subtitle, statsLabel);

        VBox newCard = new VBox(12);
        newCard.getStyleClass().add("card");
        Label newTitle = new Label("➕ Add a Brand");
        newTitle.getStyleClass().add("section-title");

        nameField.setPromptText("Brand name *");
        nameField.setPrefWidth(200);

        manufacturerField.setPromptText("Manufacturer");
        manufacturerField.setPrefWidth(200);

        originField.setPromptText("Origin country");
        originField.setPrefWidth(130);

        contactField.setPromptText("Contact / website");
        contactField.setPrefWidth(180);

        Button addBtn = new Button("➕  Add Brand");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> addBrand());

        HBox row1 = new HBox(10, nameField, manufacturerField, originField, contactField, addBtn);
        row1.setAlignment(Pos.CENTER_LEFT);

        descriptionArea.setPromptText("Description (optional)");
        descriptionArea.setPrefRowCount(2);
        descriptionArea.setPrefHeight(55);

        newCard.getChildren().addAll(newTitle, row1, descriptionArea);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Brand, String> nameCol = new TableColumn<>("Brand");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setCellFactory(col -> new TableCell<Brand, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                if (!empty && item != null) getStyleClass().add("cell-accent");
            }
        });

        TableColumn<Brand, String> manufacturerCol = new TableColumn<>("Manufacturer");
        manufacturerCol.setCellValueFactory(new PropertyValueFactory<>("manufacturer"));

        TableColumn<Brand, String> originCol = new TableColumn<>("Origin");
        originCol.setCellValueFactory(new PropertyValueFactory<>("originCountry"));

        TableColumn<Brand, String> contactCol = new TableColumn<>("Contact");
        contactCol.setCellValueFactory(new PropertyValueFactory<>("contact"));

        TableColumn<Brand, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<Brand, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(140);
        actionCol.setCellFactory(col -> new TableCell<Brand, Void>() {
            private final Button editBtn = new Button("✏️ Edit");
            private final Button deleteBtn = new Button("🗑️");
            private final HBox box = new HBox(6, editBtn, deleteBtn);
            {
                editBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                deleteBtn.getStyleClass().addAll("btn-danger", "btn-small");
                editBtn.setOnAction(e -> edit(getTableRow().getItem()));
                deleteBtn.setOnAction(e -> delete(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || getTableRow() == null || getTableRow().getItem() == null ? null : box);
            }
        });

        table.getColumns().addAll(nameCol, manufacturerCol, originCol, contactCol, descCol, actionCol);
        table.setItems(brandList);
        table.setPlaceholder(new Label("No brands added yet."));

        root.getChildren().addAll(headerCard, newCard, table);
        refresh();
        return root;
    }

    private void addBrand() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showAlert("Brand name is required.");
            return;
        }
        Brand b = new Brand();
        b.setName(name);
        b.setManufacturer(manufacturerField.getText().trim());
        b.setOriginCountry(originField.getText().trim());
        b.setContact(contactField.getText().trim());
        b.setDescription(descriptionArea.getText().trim());
        if (brandDAO.create(b)) {
            clearForm();
            refresh();
        } else {
            showAlert("Failed to add brand. A brand with this name may already exist.");
        }
    }

    private void edit(Brand b) {
        if (b == null) return;
        TextInputDialog dialog = new TextInputDialog(b.getName());
        dialog.setTitle("Edit Brand");
        dialog.setHeaderText("Rename brand");
        dialog.setContentText("Brand name:");
        dialog.showAndWait().ifPresent(newName -> {
            String trimmed = newName.trim();
            if (trimmed.isEmpty() || trimmed.equals(b.getName())) return;
            if (brandDAO.findByName(trimmed) != null) {
                showAlert("A brand with that name already exists.");
                return;
            }
            brandDAO.delete(b.getId());
            b.setName(trimmed);
            brandDAO.create(b);
            refresh();
        });
    }

    private void delete(Brand b) {
        if (b == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete brand " + b.getName() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                brandDAO.delete(b.getId());
                refresh();
            }
        });
    }

    private void clearForm() {
        nameField.clear();
        manufacturerField.clear();
        originField.clear();
        contactField.clear();
        descriptionArea.clear();
    }

    private void refresh() {
        brandList.clear();
        brandList.addAll(brandDAO.findAll());
        statsLabel.setText("Total Brands: " + brandDAO.count());
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Brands");
        alert.showAndWait();
    }
}
