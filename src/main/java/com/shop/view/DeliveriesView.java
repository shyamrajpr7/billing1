package com.shop.view;

import com.shop.dao.CustomerDAO;
import com.shop.dao.DeliveryDAO;
import com.shop.model.Customer;
import com.shop.model.Delivery;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.time.LocalDate;

public class DeliveriesView {
    private final DeliveryDAO deliveryDAO = new DeliveryDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final ObservableList<Delivery> deliveryList = FXCollections.observableArrayList();
    private final TableView<Delivery> table = new TableView<>();
    private final Label statsLabel = new Label();

    private final ComboBox<Customer> customerCombo = new ComboBox<>();
    private final TextField addressField = new TextField();
    private final TextField invoiceField = new TextField();
    private final TextField itemsField = new TextField();
    private final DatePicker schedulePicker = new DatePicker(LocalDate.now());
    private final TextField courierField = new TextField();
    private final TextField trackingField = new TextField();

    public Node getView() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label title = new Label("🚚 Delivery Management");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Plan and track customer deliveries and couriers.");
        subtitle.getStyleClass().add("sub-label");
        statsLabel.getStyleClass().add("sub-label");
        headerCard.getChildren().addAll(title, subtitle, statsLabel);

        VBox newCard = new VBox(12);
        newCard.getStyleClass().add("card");
        Label newTitle = new Label("➕ Schedule a Delivery");
        newTitle.getStyleClass().add("section-title");

        customerCombo.setPromptText("Customer");
        customerCombo.setPrefWidth(240);
        customerCombo.getItems().addAll(customerDAO.findAll());

        addressField.setPromptText("Delivery address");
        addressField.setPrefWidth(240);

        invoiceField.setPromptText("Invoice no. (optional)");
        invoiceField.setPrefWidth(130);

        itemsField.setPromptText("Items (e.g. 2x Phone)");
        itemsField.setPrefWidth(160);

        schedulePicker.setPrefWidth(140);

        HBox row1 = new HBox(10, customerCombo, addressField, invoiceField);
        row1.setAlignment(Pos.CENTER_LEFT);

        courierField.setPromptText("Courier");
        courierField.setPrefWidth(140);

        trackingField.setPromptText("Tracking no.");
        trackingField.setPrefWidth(140);

        Button scheduleBtn = new Button("🚚  Schedule Delivery");
        scheduleBtn.getStyleClass().add("btn-primary");
        scheduleBtn.setOnAction(e -> createDelivery());

        HBox row2 = new HBox(10, schedulePicker, courierField, trackingField, scheduleBtn);
        row2.setAlignment(Pos.CENTER_LEFT);

        itemsField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(itemsField, Priority.ALWAYS);

        newCard.getChildren().addAll(newTitle, row1, itemsField, row2);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Delivery, String> numCol = new TableColumn<>("Delivery No.");
        numCol.setCellValueFactory(new PropertyValueFactory<>("deliveryNumber"));

        TableColumn<Delivery, String> custCol = new TableColumn<>("Customer");
        custCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getCustomerName() != null
                ? p.getValue().getCustomerName() : "—"));

        TableColumn<Delivery, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getCustomerPhone() != null
                ? p.getValue().getCustomerPhone() : "—"));

        TableColumn<Delivery, String> itemsCol = new TableColumn<>("Items");
        itemsCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getItemsDescription() != null
                ? p.getValue().getItemsDescription() : "—"));

        TableColumn<Delivery, String> scheduledCol = new TableColumn<>("Scheduled");
        scheduledCol.setCellValueFactory(new PropertyValueFactory<>("scheduledDateLabel"));

        TableColumn<Delivery, String> courierCol = new TableColumn<>("Courier");
        courierCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getCourier() != null
                && !p.getValue().getCourier().isEmpty() ? p.getValue().getCourier() : "—"));

        TableColumn<Delivery, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<Delivery, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add(Delivery.STATUS_DELIVERED.equals(item)
                            ? "badge-active" : Delivery.STATUS_CANCELLED.equals(item)
                            ? "badge-inactive" : Delivery.STATUS_OUT_FOR_DELIVERY.equals(item)
                            ? "badge-warning" : "badge-warning");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Delivery, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(240);
        actionCol.setCellFactory(col -> new TableCell<Delivery, Void>() {
            private final Button outBtn = new Button("🛵 Out for Delivery");
            private final Button deliveredBtn = new Button("✅ Delivered");
            private final Button cancelBtn = new Button("🚫");
            private final HBox box = new HBox(6, outBtn, deliveredBtn, cancelBtn);
            {
                outBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                deliveredBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                cancelBtn.getStyleClass().addAll("btn-danger", "btn-small");
                outBtn.setOnAction(e -> setStatus(getTableRow().getItem(), Delivery.STATUS_OUT_FOR_DELIVERY));
                deliveredBtn.setOnAction(e -> setStatus(getTableRow().getItem(), Delivery.STATUS_DELIVERED));
                cancelBtn.setOnAction(e -> setStatus(getTableRow().getItem(), Delivery.STATUS_CANCELLED));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    boolean active = getTableRow().getItem().isActive();
                    outBtn.setDisable(!active || Delivery.STATUS_OUT_FOR_DELIVERY.equals(getTableRow().getItem().getStatus()));
                    deliveredBtn.setDisable(!active);
                    cancelBtn.setDisable(!active);
                    setGraphic(box);
                }
            }
        });

        table.getColumns().addAll(numCol, custCol, phoneCol, itemsCol, scheduledCol, courierCol, statusCol, actionCol);
        table.setItems(deliveryList);
        table.setPlaceholder(new Label("No deliveries scheduled yet."));

        root.getChildren().addAll(headerCard, newCard, table);
        scrollPane.setContent(root);
        refresh();
        return scrollPane;
    }

    private void createDelivery() {
        Customer c = customerCombo.getValue();
        if (c == null) {
            showAlert("Please select a customer.");
            return;
        }
        Delivery d = new Delivery();
        d.setCustomerId(c.getId());
        d.setCustomerName(c.getName());
        d.setCustomerPhone(c.getPhone());
        d.setAddress(addressField.getText().trim());
        d.setSaleInvoiceNumber(invoiceField.getText().trim());
        d.setItemsDescription(itemsField.getText().trim());
        d.setScheduledDate(schedulePicker.getValue());
        d.setCourier(courierField.getText().trim());
        d.setTrackingNumber(trackingField.getText().trim());
        if (deliveryDAO.create(d)) {
            showAlert("Delivery " + d.getDeliveryNumber() + " scheduled for " + d.getScheduledDateLabel()
                    + ".\n\nCustomer: " + d.getCustomerName()
                    + "\nAddress: " + (d.getAddress() != null && !d.getAddress().isEmpty() ? d.getAddress() : "—"));
            customerCombo.setValue(null);
            addressField.clear();
            invoiceField.clear();
            itemsField.clear();
            courierField.clear();
            trackingField.clear();
            refresh();
        } else {
            showAlert("Failed to schedule delivery.");
        }
    }

    private void setStatus(Delivery d, String status) {
        if (d == null) return;
        String msg = switch (status) {
            case Delivery.STATUS_OUT_FOR_DELIVERY -> "Mark delivery " + d.getDeliveryNumber() + " as Out for Delivery?";
            case Delivery.STATUS_DELIVERED -> "Mark delivery " + d.getDeliveryNumber() + " as Delivered?";
            default -> "Cancel delivery " + d.getDeliveryNumber() + "?";
        };
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES && deliveryDAO.updateStatus(d.getId(), status)) {
                refresh();
            }
        });
    }

    private void refresh() {
        deliveryList.clear();
        deliveryList.addAll(deliveryDAO.findAll());
        statsLabel.setText("Active: " + deliveryDAO.countActive()
                + "  •  Due Today: " + deliveryDAO.countDueToday()
                + "  •  Delivered: " + deliveryDAO.countDelivered()
                + "  •  Total: " + deliveryDAO.count());
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Deliveries");
        alert.showAndWait();
    }
}
