package com.shop.view;

import com.shop.dao.UserDAO;
import com.shop.model.Role;
import com.shop.model.User;
import com.shop.util.PasswordUtil;
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

public class EmployeeView {
    private final UserDAO userDAO = new UserDAO();
    private final ObservableList<User> userList = FXCollections.observableArrayList();
    private final TableView<User> table = new TableView<>();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        HBox controlBar = new HBox(12);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        Label headerTitle = new Label("Employee Accounts");
        headerTitle.getStyleClass().add("section-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addUserBtn = new Button("➕  Create Employee Account");
        addUserBtn.getStyleClass().add("btn-primary");
        addUserBtn.setOnAction(e -> showUserDialog(null));

        controlBar.getChildren().addAll(headerTitle, spacer, addUserBtn);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<User, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setMaxWidth(60);

        TableColumn<User, String> nameCol = new TableColumn<>("Full Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));

        TableColumn<User, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getRoleDisplay()));

        TableColumn<User, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setGraphic(null);
                else {
                    Label badge = new Label(item);
                    if ("Active".equals(item)) badge.getStyleClass().add("badge-active");
                    else badge.getStyleClass().add("badge-inactive");
                    setGraphic(badge);
                }
            }
        });

        TableColumn<User, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<User, Void>() {
            private final Button editBtn = new Button("✏️ Edit");
            private final Button passBtn = new Button("🔑 Reset Pass");
            private final HBox btnBox = new HBox(6, editBtn, passBtn);
            {
                editBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                passBtn.getStyleClass().addAll("btn-warning", "btn-small");

                editBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    showUserDialog(u);
                });

                passBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    showPasswordResetDialog(u);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(btnBox);
            }
        });

        table.getColumns().addAll(idCol, nameCol, usernameCol, roleCol, statusCol, actionCol);
        table.setItems(userList);

        root.getChildren().addAll(controlBar, table);
        loadUsers();
        return root;
    }

    private void loadUsers() {
        userList.clear();
        userList.addAll(userDAO.findAll());
    }

    private void showUserDialog(User userToEdit) {
        boolean isEdit = (userToEdit != null);
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(isEdit ? "Edit Employee Account" : "Create Employee Account");

        VBox form = new VBox(14);
        form.setPadding(new Insets(20));
        form.getStyleClass().add("card");

        TextField nameField = new TextField(isEdit ? userToEdit.getFullName() : "");
        nameField.setPromptText("Full Name");

        TextField usernameField = new TextField(isEdit ? userToEdit.getUsername() : "");
        usernameField.setPromptText("Username");

        PasswordField passField = new PasswordField();
        passField.setPromptText("Password");

        ComboBox<Role> roleCombo = new ComboBox<>(FXCollections.observableArrayList(Role.values()));
        if (isEdit) roleCombo.getSelectionModel().select(userToEdit.getRole());
        else roleCombo.getSelectionModel().select(Role.CASHIER);
        roleCombo.setMaxWidth(Double.MAX_VALUE);

        CheckBox activeCheckBox = new CheckBox("Account Active");
        activeCheckBox.setSelected(isEdit ? userToEdit.isActive() : true);
        activeCheckBox.setStyle("-fx-text-fill: #ccccee;");

        Button saveBtn = new Button(isEdit ? "Update Employee" : "Create Account");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setMaxWidth(Double.MAX_VALUE);

        saveBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String username = usernameField.getText().trim();
            String password = passField.getText();

            if (name.isEmpty() || username.isEmpty() || (!isEdit && password.isEmpty())) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Please fill in all required fields.");
                a.showAndWait();
                return;
            }

            User u = isEdit ? userToEdit : new User();
            u.setFullName(name);
            u.setUsername(username);
            if (!password.isEmpty()) u.setPasswordHash(PasswordUtil.hash(password));
            u.setRole(roleCombo.getValue());
            u.setActive(activeCheckBox.isSelected());

            if (isEdit) userDAO.update(u);
            else userDAO.insert(u);

            dialog.close();
            loadUsers();
        });

        form.getChildren().addAll(
                new Label("Full Name"), nameField,
                new Label("Username"), usernameField
        );

        if (!isEdit) {
            form.getChildren().addAll(new Label("Password"), passField);
        }

        form.getChildren().addAll(
                new Label("Role"), roleCombo,
                activeCheckBox,
                saveBtn
        );

        Scene scene = new Scene(form, 400, isEdit ? 420 : 480);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.show();
    }

    private void showPasswordResetDialog(User user) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Reset Password - " + user.getUsername());

        VBox form = new VBox(14);
        form.setPadding(new Insets(20));
        form.getStyleClass().add("card");

        Label label = new Label("Enter new password for " + user.getFullName() + ":");
        label.setStyle("-fx-text-fill: #ffffff;");

        PasswordField newPassField = new PasswordField();
        newPassField.setPromptText("New Password");

        Button updateBtn = new Button("Update Password");
        updateBtn.getStyleClass().add("btn-warning");
        updateBtn.setMaxWidth(Double.MAX_VALUE);

        updateBtn.setOnAction(e -> {
            String newPass = newPassField.getText();
            if (newPass.length() < 4) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Password must be at least 4 characters.");
                a.showAndWait();
                return;
            }

            userDAO.updatePassword(user.getId(), PasswordUtil.hash(newPass));
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Password updated successfully!");
            a.showAndWait();
            dialog.close();
        });

        form.getChildren().addAll(label, newPassField, updateBtn);

        Scene scene = new Scene(form, 350, 220);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.show();
    }
}
