package com.shop.view;

import com.shop.dao.DiscountDAO;
import com.shop.model.Discount;
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

public class DiscountView {
    private final DiscountDAO discountDAO = new DiscountDAO();
    private final ObservableList<Discount> discountList = FXCollections.observableArrayList();
    private final TableView<Discount> table = new TableView<>();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        HBox controlBar = new HBox(12);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        Label headerTitle = new Label("Discounts & Promotional Coupons");
        headerTitle.getStyleClass().add("section-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("➕  Create Discount Coupon");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> showDiscountDialog(null));

        controlBar.getChildren().addAll(headerTitle, spacer, addBtn);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Discount, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setMaxWidth(60);

        TableColumn<Discount, String> codeCol = new TableColumn<>("Coupon Code");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("code"));

        TableColumn<Discount, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<Discount, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));

        TableColumn<Discount, String> valueCol = new TableColumn<>("Discount Value");
        valueCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValueDisplay()));

        TableColumn<Discount, String> minPurCol = new TableColumn<>("Min Order");
        minPurCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().getMinPurchase())));

        TableColumn<Discount, LocalDate> endCol = new TableColumn<>("Expires On");
        endCol.setCellValueFactory(new PropertyValueFactory<>("endDate"));

        TableColumn<Discount, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<Discount, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setGraphic(null);
                else {
                    Label badge = new Label(item);
                    if ("Active".equals(item)) badge.getStyleClass().add("badge-active");
                    else if ("Upcoming".equals(item)) badge.getStyleClass().add("badge-info");
                    else badge.getStyleClass().add("badge-inactive");
                    setGraphic(badge);
                }
            }
        });

        TableColumn<Discount, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<Discount, Void>() {
            private final Button editBtn = new Button("✏️");
            private final Button delBtn = new Button("🗑️");
            private final HBox btnBox = new HBox(6, editBtn, delBtn);
            {
                editBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                delBtn.getStyleClass().addAll("btn-danger", "btn-small");

                editBtn.setOnAction(e -> {
                    Discount d = getTableView().getItems().get(getIndex());
                    showDiscountDialog(d);
                });

                delBtn.setOnAction(e -> {
                    Discount d = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete coupon '" + d.getCode() + "'?", ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(resp -> {
                        if (resp == ButtonType.YES) {
                            discountDAO.delete(d.getId());
                            loadDiscounts();
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

        table.getColumns().addAll(idCol, codeCol, descCol, typeCol, valueCol, minPurCol, endCol, statusCol, actionCol);
        table.setItems(discountList);

        root.getChildren().addAll(controlBar, table);
        loadDiscounts();
        return root;
    }

    private void loadDiscounts() {
        discountList.clear();
        discountList.addAll(discountDAO.findAll());
    }

    private void showDiscountDialog(Discount discountToEdit) {
        boolean isEdit = (discountToEdit != null);
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(isEdit ? "Edit Coupon" : "Create Coupon");

        VBox form = new VBox(14);
        form.setPadding(new Insets(20));
        form.getStyleClass().add("card");

        TextField codeField = new TextField(isEdit ? discountToEdit.getCode() : "");
        codeField.setPromptText("Coupon Code (e.g. SAVE10)");

        TextField descField = new TextField(isEdit ? discountToEdit.getDescription() : "");
        descField.setPromptText("Description (e.g. 10% off on all orders)");

        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("PERCENTAGE", "FLAT"));
        if (isEdit) typeCombo.getSelectionModel().select(discountToEdit.getType());
        else typeCombo.getSelectionModel().selectFirst();
        typeCombo.setMaxWidth(Double.MAX_VALUE);

        TextField valField = new TextField(isEdit ? String.valueOf(discountToEdit.getValue()) : "");
        valField.setPromptText("Discount Value (% or ₹ amount)");

        TextField minPurField = new TextField(isEdit ? String.valueOf(discountToEdit.getMinPurchase()) : "0");
        minPurField.setPromptText("Minimum Purchase Requirement (₹)");

        DatePicker startPicker = new DatePicker(isEdit ? discountToEdit.getStartDate() : LocalDate.now());
        DatePicker endPicker = new DatePicker(isEdit ? discountToEdit.getEndDate() : LocalDate.now().plusMonths(1));
        startPicker.setMaxWidth(Double.MAX_VALUE);
        endPicker.setMaxWidth(Double.MAX_VALUE);

        CheckBox activeCheckBox = new CheckBox("Coupon Active");
        activeCheckBox.setSelected(isEdit ? discountToEdit.isActive() : true);
        activeCheckBox.setStyle("-fx-text-fill: #ccccee;");

        Button saveBtn = new Button(isEdit ? "Update Coupon" : "Save Coupon");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setMaxWidth(Double.MAX_VALUE);

        saveBtn.setOnAction(e -> {
            try {
                String code = codeField.getText().trim();
                double value = Double.parseDouble(valField.getText().trim());
                double minPur = Double.parseDouble(minPurField.getText().trim());

                if (code.isEmpty()) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Coupon code is required!");
                    a.showAndWait();
                    return;
                }

                Discount d = isEdit ? discountToEdit : new Discount();
                d.setCode(code);
                d.setDescription(descField.getText().trim());
                d.setType(typeCombo.getValue());
                d.setValue(value);
                d.setMinPurchase(minPur);
                d.setStartDate(startPicker.getValue());
                d.setEndDate(endPicker.getValue());
                d.setActive(activeCheckBox.isSelected());

                if (isEdit) discountDAO.update(d);
                else discountDAO.insert(d);

                dialog.close();
                loadDiscounts();
            } catch (NumberFormatException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Please enter valid numeric values for discount and minimum order amount.");
                a.showAndWait();
            }
        });

        form.getChildren().addAll(
                new Label("Coupon Code"), codeField,
                new Label("Description"), descField,
                new HBox(10, new VBox(6, new Label("Discount Type"), typeCombo), new VBox(6, new Label("Value"), valField)),
                new Label("Minimum Order Amount (₹)"), minPurField,
                new HBox(10, new VBox(6, new Label("Start Date"), startPicker), new VBox(6, new Label("End Date"), endPicker)),
                activeCheckBox,
                saveBtn
        );

        Scene scene = new Scene(form, 450, 560);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.show();
    }
}
