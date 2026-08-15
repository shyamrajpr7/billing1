package com.shop.view;

import com.shop.dao.ReminderDAO;
import com.shop.model.Reminder;
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

public class RemindersView {
    private final ReminderDAO reminderDAO = new ReminderDAO();
    private final ObservableList<Reminder> reminderList = FXCollections.observableArrayList();
    private final TableView<Reminder> table = new TableView<>();
    private final Label statsLabel = new Label();

    private final TextField titleField = new TextField();
    private final TextField detailsField = new TextField();
    private final DatePicker duePicker = new DatePicker(LocalDate.now());
    private final TextField timeField = new TextField("09:00");
    private final ComboBox<String> priorityCombo = new ComboBox<>();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label title = new Label("⏰ Reminders & To-Dos");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Keep track of important shop tasks and deadlines.");
        subtitle.getStyleClass().add("sub-label");
        statsLabel.getStyleClass().add("sub-label");
        headerCard.getChildren().addAll(title, subtitle, statsLabel);

        VBox newCard = new VBox(12);
        newCard.getStyleClass().add("card");
        Label newTitle = new Label("➕ Add a Reminder");
        newTitle.getStyleClass().add("section-title");

        titleField.setPromptText("Task / title");
        titleField.setPrefWidth(220);

        detailsField.setPromptText("Details (optional)");
        detailsField.setPrefWidth(220);

        duePicker.setPrefWidth(140);

        timeField.setPromptText("Time (HH:mm)");
        timeField.setPrefWidth(90);

        priorityCombo.getItems().addAll(Reminder.PRIORITY_LOW, Reminder.PRIORITY_NORMAL, Reminder.PRIORITY_HIGH);
        priorityCombo.setValue(Reminder.PRIORITY_NORMAL);
        priorityCombo.setPrefWidth(110);

        Button addBtn = new Button("➕  Add Reminder");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> addReminder());
        titleField.setOnAction(e -> addBtn.fire());

        HBox formRow = new HBox(10, titleField, detailsField, duePicker, timeField, priorityCombo, addBtn);
        formRow.setAlignment(Pos.CENTER_LEFT);

        newCard.getChildren().addAll(newTitle, formRow);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Reminder, String> doneCol = new TableColumn<>("Done");
        doneCol.setPrefWidth(60);
        doneCol.setCellFactory(col -> new TableCell<Reminder, String>() {
            private final CheckBox cb = new CheckBox();
            {
                cb.setOnAction(e -> {
                    Reminder r = getTableRow().getItem();
                    if (r != null) {
                        reminderDAO.toggleCompleted(r.getId());
                        refresh();
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    cb.setSelected(getTableRow().getItem().isCompleted());
                    setGraphic(cb);
                }
            }
        });

        TableColumn<Reminder, String> titleCol = new TableColumn<>("Task");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<Reminder, String> detailsCol = new TableColumn<>("Details");
        detailsCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getDetails() != null
                ? p.getValue().getDetails() : ""));

        TableColumn<Reminder, String> dueCol = new TableColumn<>("Due Date");
        dueCol.setCellValueFactory(new PropertyValueFactory<>("dueDateLabel"));

        TableColumn<Reminder, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("dueTimeLabel"));

        TableColumn<Reminder, String> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(new PropertyValueFactory<>("priority"));
        priorityCol.setCellFactory(col -> new TableCell<Reminder, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add(Reminder.PRIORITY_HIGH.equals(item)
                            ? "badge-inactive" : Reminder.PRIORITY_NORMAL.equals(item)
                            ? "badge-warning" : "badge-active");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Reminder, String> stateCol = new TableColumn<>("State");
        stateCol.setCellValueFactory(p -> new SimpleStringProperty(
                p.getValue().isCompleted() ? "Completed"
                        : p.getValue().isOverdue() ? "Overdue" : "Pending"));
        stateCol.setCellFactory(col -> new TableCell<Reminder, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("Completed".equals(item)
                            ? "badge-active" : "Overdue".equals(item)
                            ? "badge-inactive" : "badge-warning");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Reminder, Void> actionCol = new TableColumn<>("");
        actionCol.setPrefWidth(60);
        actionCol.setCellFactory(col -> new TableCell<Reminder, Void>() {
            private final Button deleteBtn = new Button("🗑️");
            {
                deleteBtn.getStyleClass().addAll("btn-danger", "btn-small");
                deleteBtn.setOnAction(e -> {
                    Reminder r = getTableRow().getItem();
                    if (r != null) {
                        reminderDAO.delete(r.getId());
                        refresh();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || getTableRow() == null || getTableRow().getItem() == null ? null : deleteBtn);
            }
        });

        table.getColumns().addAll(doneCol, titleCol, detailsCol, dueCol, timeCol, priorityCol, stateCol, actionCol);
        table.setItems(reminderList);
        table.setPlaceholder(new Label("No reminders yet."));

        root.getChildren().addAll(headerCard, newCard, table);
        refresh();
        return root;
    }

    private void addReminder() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            showAlert("Please enter a task title.");
            return;
        }
        Reminder r = new Reminder();
        r.setTitle(title);
        r.setDetails(detailsField.getText().trim());
        r.setDueDate(duePicker.getValue());
        r.setPriority(priorityCombo.getValue());
        try {
            r.setDueTime(java.time.LocalTime.parse(timeField.getText().trim()));
        } catch (Exception ex) {
            r.setDueTime(null);
        }
        if (reminderDAO.create(r)) {
            titleField.clear();
            detailsField.clear();
            timeField.setText("09:00");
            duePicker.setValue(LocalDate.now());
            priorityCombo.setValue(Reminder.PRIORITY_NORMAL);
            refresh();
        } else {
            showAlert("Failed to add reminder.");
        }
    }

    private void refresh() {
        reminderList.clear();
        reminderList.addAll(reminderDAO.findAll());
        statsLabel.setText("Pending: " + reminderDAO.countPending()
                + "  •  Overdue: " + reminderDAO.countOverdue()
                + "  •  Due in 7 days: " + reminderDAO.countDueSoon(7));
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Reminders");
        alert.showAndWait();
    }
}
