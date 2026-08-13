package com.shop.view;

import com.shop.dao.CustomerDAO;
import com.shop.dao.ServiceTicketDAO;
import com.shop.model.Customer;
import com.shop.model.ServiceTicket;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

public class ServiceTicketsView {
    private final ServiceTicketDAO ticketDAO = new ServiceTicketDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final ObservableList<ServiceTicket> ticketList = FXCollections.observableArrayList();
    private final TableView<ServiceTicket> table = new TableView<>();
    private final Label statsLabel = new Label();

    private final ComboBox<Customer> customerCombo = new ComboBox<>();
    private final TextField subjectField = new TextField();
    private final TextArea descriptionArea = new TextArea();
    private final ComboBox<String> priorityCombo = new ComboBox<>();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label title = new Label("🎫 Customer Service Tickets");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Log and resolve customer complaints and service requests.");
        subtitle.getStyleClass().add("sub-label");
        statsLabel.getStyleClass().add("sub-label");
        headerCard.getChildren().addAll(title, subtitle, statsLabel);

        VBox newCard = new VBox(12);
        newCard.getStyleClass().add("card");
        Label newTitle = new Label("➕ Open a New Ticket");
        newTitle.getStyleClass().add("section-title");

        customerCombo.setPromptText("Customer");
        customerCombo.setPrefWidth(240);
        customerCombo.getItems().addAll(customerDAO.findAll());

        subjectField.setPromptText("Subject *");
        subjectField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(subjectField, Priority.ALWAYS);

        priorityCombo.getItems().addAll(ServiceTicket.PRIORITY_LOW, ServiceTicket.PRIORITY_NORMAL, ServiceTicket.PRIORITY_HIGH);
        priorityCombo.setValue(ServiceTicket.PRIORITY_NORMAL);
        priorityCombo.setPrefWidth(110);

        Button openBtn = new Button("🎫  Open Ticket");
        openBtn.getStyleClass().add("btn-primary");
        openBtn.setOnAction(e -> openTicket());

        HBox row1 = new HBox(10, customerCombo, subjectField, priorityCombo, openBtn);
        row1.setAlignment(Pos.CENTER_LEFT);

        descriptionArea.setPromptText("Describe the issue...");
        descriptionArea.setPrefRowCount(2);
        descriptionArea.setPrefHeight(55);

        newCard.getChildren().addAll(newTitle, row1, descriptionArea);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<ServiceTicket, String> numCol = new TableColumn<>("Ticket");
        numCol.setCellValueFactory(new PropertyValueFactory<>("ticketNumber"));

        TableColumn<ServiceTicket, String> custCol = new TableColumn<>("Customer");
        custCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getCustomerName() != null
                ? p.getValue().getCustomerName() : "—"));

        TableColumn<ServiceTicket, String> subjectCol = new TableColumn<>("Subject");
        subjectCol.setCellValueFactory(new PropertyValueFactory<>("subject"));

        TableColumn<ServiceTicket, String> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(new PropertyValueFactory<>("priority"));
        priorityCol.setCellFactory(col -> new TableCell<ServiceTicket, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add(ServiceTicket.PRIORITY_HIGH.equals(item)
                            ? "badge-inactive" : ServiceTicket.PRIORITY_NORMAL.equals(item)
                            ? "badge-warning" : "badge-active");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<ServiceTicket, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<ServiceTicket, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add(ServiceTicket.STATUS_RESOLVED.equals(item)
                            || ServiceTicket.STATUS_CLOSED.equals(item)
                            ? "badge-active" : ServiceTicket.STATUS_IN_PROGRESS.equals(item)
                            ? "badge-warning" : "badge-inactive");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<ServiceTicket, String> createdCol = new TableColumn<>("Opened");
        createdCol.setCellValueFactory(new PropertyValueFactory<>("createdAtLabel"));

        TableColumn<ServiceTicket, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(240);
        actionCol.setCellFactory(col -> new TableCell<ServiceTicket, Void>() {
            private final Button startBtn = new Button("▶ Start");
            private final Button resolveBtn = new Button("✅ Resolve");
            private final Button closeBtn = new Button("🔒 Close");
            private final HBox box = new HBox(6, startBtn, resolveBtn, closeBtn);
            {
                startBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                resolveBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                closeBtn.getStyleClass().addAll("btn-danger", "btn-small");
                startBtn.setOnAction(e -> setStatus(getTableRow().getItem(), ServiceTicket.STATUS_IN_PROGRESS));
                resolveBtn.setOnAction(e -> setStatus(getTableRow().getItem(), ServiceTicket.STATUS_RESOLVED));
                closeBtn.setOnAction(e -> setStatus(getTableRow().getItem(), ServiceTicket.STATUS_CLOSED));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    boolean open = getTableRow().getItem().isOpen();
                    startBtn.setDisable(!open || ServiceTicket.STATUS_IN_PROGRESS.equals(getTableRow().getItem().getStatus()));
                    resolveBtn.setDisable(!open);
                    closeBtn.setDisable(!open);
                    setGraphic(box);
                }
            }
        });

        table.getColumns().addAll(numCol, custCol, subjectCol, priorityCol, statusCol, createdCol, actionCol);
        table.setItems(ticketList);
        table.setPlaceholder(new Label("No service tickets yet."));

        root.getChildren().addAll(headerCard, newCard, table);
        refresh();
        return root;
    }

    private void openTicket() {
        String subject = subjectField.getText().trim();
        if (subject.isEmpty()) {
            showAlert("Please enter a subject.");
            return;
        }
        ServiceTicket t = new ServiceTicket();
        Customer c = customerCombo.getValue();
        if (c != null) {
            t.setCustomerId(c.getId());
            t.setCustomerName(c.getName());
            t.setCustomerPhone(c.getPhone());
        } else {
            t.setCustomerName("Walk-in");
        }
        t.setSubject(subject);
        t.setDescription(descriptionArea.getText().trim());
        t.setPriority(priorityCombo.getValue());
        if (ticketDAO.create(t)) {
            showAlert("Ticket " + t.getTicketNumber() + " opened successfully!");
            subjectField.clear();
            descriptionArea.clear();
            customerCombo.setValue(null);
            priorityCombo.setValue(ServiceTicket.PRIORITY_NORMAL);
            refresh();
        } else {
            showAlert("Failed to open ticket.");
        }
    }

    private void setStatus(ServiceTicket t, String status) {
        if (t == null) return;
        ticketDAO.updateStatus(t.getId(), status);
        refresh();
    }

    private void refresh() {
        ticketList.clear();
        ticketList.addAll(ticketDAO.findAll());
        statsLabel.setText("Open: " + ticketDAO.countOpen()
                + "  •  High Priority: " + ticketDAO.countHighPriority()
                + "  •  Resolved: " + ticketDAO.countResolved()
                + "  •  Total: " + ticketDAO.count());
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Service Tickets");
        alert.showAndWait();
    }
}
