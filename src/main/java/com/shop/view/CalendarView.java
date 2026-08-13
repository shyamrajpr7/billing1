package com.shop.view;

import com.shop.dao.CalendarEventDAO;
import com.shop.model.CalendarEvent;
import com.shop.model.User;
import com.shop.util.SessionManager;
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

public class CalendarView {
    private final CalendarEventDAO eventDAO = new CalendarEventDAO();
    private final ObservableList<CalendarEvent> eventList = FXCollections.observableArrayList();
    private final TableView<CalendarEvent> table = new TableView<>();
    private final Label statsLabel = new Label();

    private final TextField titleField = new TextField();
    private final TextField descriptionField = new TextField();
    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final TextField timeField = new TextField("09:00");
    private final ComboBox<String> typeCombo = new ComboBox<>();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label title = new Label("📅 Store Calendar & Events");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Plan holidays, sales, meetings and important dates.");
        subtitle.getStyleClass().add("sub-label");
        statsLabel.getStyleClass().add("sub-label");
        headerCard.getChildren().addAll(title, subtitle, statsLabel);

        VBox addCard = new VBox(12);
        addCard.getStyleClass().add("card");
        Label addTitle = new Label("➕ Add an Event");
        addTitle.getStyleClass().add("section-title");

        titleField.setPromptText("Event title *");
        titleField.setPrefWidth(220);

        descriptionField.setPromptText("Details (optional)");
        descriptionField.setPrefWidth(200);

        datePicker.setPrefWidth(140);

        timeField.setPromptText("Time (HH:mm)");
        timeField.setPrefWidth(90);

        typeCombo.getItems().addAll(
                CalendarEvent.TYPE_SALE, CalendarEvent.TYPE_HOLIDAY,
                CalendarEvent.TYPE_MEETING, CalendarEvent.TYPE_OTHER);
        typeCombo.setValue(CalendarEvent.TYPE_SALE);
        typeCombo.setPrefWidth(110);

        Button addBtn = new Button("➕  Add Event");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> addEvent());

        HBox formRow = new HBox(10, titleField, descriptionField, datePicker, timeField, typeCombo, addBtn);
        formRow.setAlignment(Pos.CENTER_LEFT);

        addCard.getChildren().addAll(addTitle, formRow);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<CalendarEvent, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("dateLabel"));

        TableColumn<CalendarEvent, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timeLabel"));

        TableColumn<CalendarEvent, String> titleCol = new TableColumn<>("Event");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setCellFactory(col -> new TableCell<CalendarEvent, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                if (!empty && item != null) getStyleClass().add("cell-accent");
            }
        });

        TableColumn<CalendarEvent, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("eventType"));
        typeCol.setCellFactory(col -> new TableCell<CalendarEvent, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add(CalendarEvent.TYPE_SALE.equals(item)
                            ? "badge-active" : CalendarEvent.TYPE_HOLIDAY.equals(item)
                            ? "badge-warning" : "badge-inactive");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<CalendarEvent, String> descCol = new TableColumn<>("Details");
        descCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getDescription() != null
                ? p.getValue().getDescription() : ""));

        TableColumn<CalendarEvent, Void> actionCol = new TableColumn<>("");
        actionCol.setPrefWidth(60);
        actionCol.setCellFactory(col -> new TableCell<CalendarEvent, Void>() {
            private final Button deleteBtn = new Button("🗑️");
            {
                deleteBtn.getStyleClass().addAll("btn-danger", "btn-small");
                deleteBtn.setOnAction(e -> {
                    CalendarEvent ev = getTableRow().getItem();
                    if (ev != null) {
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                                "Delete event " + ev.getTitle() + "?", ButtonType.YES, ButtonType.NO);
                        confirm.showAndWait().ifPresent(resp -> {
                            if (resp == ButtonType.YES) {
                                eventDAO.delete(ev.getId());
                                refresh();
                            }
                        });
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || getTableRow() == null || getTableRow().getItem() == null ? null : deleteBtn);
            }
        });

        table.getColumns().addAll(dateCol, timeCol, titleCol, typeCol, descCol, actionCol);
        table.setItems(eventList);
        table.setPlaceholder(new Label("No events planned yet."));

        root.getChildren().addAll(headerCard, addCard, table);
        refresh();
        return root;
    }

    private void addEvent() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            showAlert("Please enter an event title.");
            return;
        }
        CalendarEvent e = new CalendarEvent();
        e.setTitle(title);
        e.setDescription(descriptionField.getText().trim());
        e.setDate(datePicker.getValue());
        e.setEventType(typeCombo.getValue());
        try {
            e.setTime(java.time.LocalTime.parse(timeField.getText().trim()));
        } catch (Exception ex) {
            e.setTime(null);
        }
        User u = SessionManager.getInstance().getCurrentUser();
        if (u != null) e.setCreatedBy(u.getFullName());
        if (eventDAO.create(e)) {
            showAlert("Event added!\n\n" + e.getTitle() + "\n" + e.getDateLabel()
                    + (e.getTimeLabel() != null ? " at " + e.getTimeLabel() : ""));
            titleField.clear();
            descriptionField.clear();
            timeField.setText("09:00");
            datePicker.setValue(LocalDate.now());
            typeCombo.setValue(CalendarEvent.TYPE_SALE);
            refresh();
        } else {
            showAlert("Failed to add event.");
        }
    }

    private void refresh() {
        eventList.clear();
        eventList.addAll(eventDAO.findAll());
        statsLabel.setText("Upcoming Events: " + eventDAO.countUpcoming()
                + "  •  Total Events: " + eventDAO.count());
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Calendar");
        alert.showAndWait();
    }
}
