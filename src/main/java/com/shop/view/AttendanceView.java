package com.shop.view;

import com.shop.dao.AttendanceDAO;
import com.shop.dao.UserDAO;
import com.shop.model.Attendance;
import com.shop.model.User;
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
import java.time.YearMonth;
import java.util.List;

public class AttendanceView {
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final UserDAO userDAO = new UserDAO();
    private final ObservableList<Attendance> attendanceList = FXCollections.observableArrayList();
    private final TableView<Attendance> table = new TableView<>();
    private final Label statsLabel = new Label();

    private final ComboBox<User> employeeCombo = new ComboBox<>();
    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final ComboBox<String> statusCombo = new ComboBox<>();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label title = new Label("👔 Staff Attendance & Timesheet");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Track employee check-in/check-out and leaves.");
        subtitle.getStyleClass().add("sub-label");
        statsLabel.getStyleClass().add("sub-label");
        headerCard.getChildren().addAll(title, subtitle, statsLabel);

        VBox recordCard = new VBox(12);
        recordCard.getStyleClass().add("card");
        Label recordTitle = new Label("➕ Record Attendance");
        recordTitle.getStyleClass().add("section-title");

        employeeCombo.setPromptText("Employee");
        employeeCombo.setPrefWidth(260);
        employeeCombo.getItems().addAll(userDAO.findAll());

        statusCombo.getItems().addAll(
                Attendance.STATUS_PRESENT, Attendance.STATUS_HALF_DAY,
                Attendance.STATUS_ABSENT, Attendance.STATUS_LEAVE);
        statusCombo.setValue(Attendance.STATUS_PRESENT);
        statusCombo.setPrefWidth(120);

        Button checkInBtn = new Button("⏱️  Check-In (now)");
        checkInBtn.getStyleClass().add("btn-primary");
        checkInBtn.setOnAction(e -> record(true));

        Button markBtn = new Button("💾  Mark / Save");
        markBtn.getStyleClass().add("btn-secondary");
        markBtn.setOnAction(e -> record(false));

        Button checkoutBtn = new Button("🚪  Check-Out");
        checkoutBtn.getStyleClass().add("btn-secondary");
        checkoutBtn.setOnAction(e -> checkOutSelected());

        HBox formRow = new HBox(10);
        formRow.setAlignment(Pos.CENTER_LEFT);
        formRow.getChildren().addAll(employeeCombo, datePicker, statusCombo, checkInBtn, markBtn, checkoutBtn);

        recordCard.getChildren().addAll(recordTitle, formRow);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Attendance, String> empCol = new TableColumn<>("Employee");
        empCol.setCellValueFactory(new PropertyValueFactory<>("employeeName"));

        TableColumn<Attendance, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("dateLabel"));

        TableColumn<Attendance, String> inCol = new TableColumn<>("Check-In");
        inCol.setCellValueFactory(new PropertyValueFactory<>("checkInLabel"));

        TableColumn<Attendance, String> outCol = new TableColumn<>("Check-Out");
        outCol.setCellValueFactory(new PropertyValueFactory<>("checkOutLabel"));

        TableColumn<Attendance, String> hoursCol = new TableColumn<>("Hours");
        hoursCol.setCellValueFactory(p -> new SimpleStringProperty(
                p.getValue().getHoursWorked() > 0 ? String.valueOf(p.getValue().getHoursWorked()) : "—"));

        TableColumn<Attendance, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<Attendance, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add(Attendance.STATUS_PRESENT.equals(item)
                            ? "badge-active" : Attendance.STATUS_ABSENT.equals(item)
                            ? "badge-inactive" : "badge-warning");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Attendance, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(120);
        actionCol.setCellFactory(col -> new TableCell<Attendance, Void>() {
            private final Button checkoutBtn = new Button("🚪 Check-Out");
            {
                checkoutBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                checkoutBtn.setOnAction(e -> checkOut(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    checkoutBtn.setDisable(!getTableRow().getItem().isCheckedIn());
                    setGraphic(checkoutBtn);
                }
            }
        });

        table.getColumns().addAll(empCol, dateCol, inCol, outCol, hoursCol, statusCol, actionCol);
        table.setItems(attendanceList);
        table.setPlaceholder(new Label("No attendance records yet."));
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        root.getChildren().addAll(headerCard, recordCard, table);
        refresh();
        return root;
    }

    private void record(boolean checkInNow) {
        User u = employeeCombo.getValue();
        if (u == null) {
            showAlert("Please select an employee.");
            return;
        }
        LocalDate date = datePicker.getValue();
        if (date == null) {
            showAlert("Please select a date.");
            return;
        }
        if (attendanceDAO.findByUserAndDate(u.getId(), date) != null) {
            showAlert(u.getFullName() + " already has an attendance record for " + date + ".");
            return;
        }
        Attendance a = new Attendance();
        a.setUserId(u.getId());
        a.setEmployeeName(u.getFullName());
        a.setDate(date);
        a.setStatus(statusCombo.getValue());
        if (checkInNow && !Attendance.STATUS_ABSENT.equals(a.getStatus())
                && !Attendance.STATUS_LEAVE.equals(a.getStatus())) {
            a.setCheckInTime(java.time.LocalTime.now());
        }
        if (attendanceDAO.record(a)) {
            refresh();
        } else {
            showAlert("Failed to record attendance.");
        }
    }

    private void checkOutSelected() {
        Attendance selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Select an attendance row to check out.");
            return;
        }
        checkOut(selected);
    }

    private void checkOut(Attendance a) {
        if (a == null || !a.isCheckedIn()) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Check out " + a.getEmployeeName() + " now?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES && attendanceDAO.checkOut(a.getId())) {
                refresh();
            }
        });
    }

    private void refresh() {
        attendanceList.clear();
        attendanceList.addAll(attendanceDAO.findAll());
        YearMonth ym = YearMonth.now();
        statsLabel.setText("Present Today: " + attendanceDAO.countPresentOn(LocalDate.now())
                + "  •  Records This Month: " + attendanceDAO.countForMonth(ym.toString()));
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Attendance");
        alert.showAndWait();
    }
}
