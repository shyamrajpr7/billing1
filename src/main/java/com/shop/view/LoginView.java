package com.shop.view;

import com.shop.dao.UserDAO;
import com.shop.model.User;
import com.shop.util.PasswordUtil;
import com.shop.util.SessionManager;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class LoginView {
    private final Stage stage;
    private final UserDAO userDAO = new UserDAO();

    public LoginView(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        VBox root = new VBox();
        root.getStyleClass().add("login-pane");
        root.setAlignment(Pos.CENTER);

        VBox card = new VBox(20);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(420);
        card.setAlignment(Pos.CENTER);

        // Header
        Label iconLabel = new Label("🛍️");
        iconLabel.getStyleClass().add("login-icon");

        Label titleLabel = new Label("Shop Management");
        titleLabel.getStyleClass().add("login-title");

        Label subtitleLabel = new Label("Sign in to your shop account");
        subtitleLabel.getStyleClass().add("login-subtitle");

        VBox headerBox = new VBox(5, iconLabel, titleLabel, subtitleLabel);
        headerBox.setAlignment(Pos.CENTER);

        // Form fields
        Label userLabel = new Label("Username");
        userLabel.getStyleClass().add("form-label");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter username");
        usernameField.setText("admin");

        VBox userBox = new VBox(6, userLabel, usernameField);

        Label passLabel = new Label("Password");
        passLabel.getStyleClass().add("form-label");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");
        passwordField.setText("admin123");

        VBox passBox = new VBox(6, passLabel, passwordField);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);

        Button loginBtn = new Button("SIGN IN");
        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setPrefHeight(45);

        Runnable handleLogin = () -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Please enter both username and password");
                errorLabel.setVisible(true);
                return;
            }

            User user = userDAO.authenticate(username, PasswordUtil.hash(password));
            if (user != null) {
                SessionManager.getInstance().setCurrentUser(user);
                DashboardView dashboardView = new DashboardView(stage);
                dashboardView.show();
            } else {
                errorLabel.setText("Invalid username or password");
                errorLabel.setVisible(true);
            }
        };

        loginBtn.setOnAction(e -> handleLogin.run());
        passwordField.setOnAction(e -> handleLogin.run());
        usernameField.setOnAction(e -> handleLogin.run());

        Label hintLabel = new Label("Default Admin: admin / admin123");
        hintLabel.getStyleClass().add("sub-label");

        card.getChildren().addAll(headerBox, userBox, passBox, errorLabel, loginBtn, hintLabel);
        root.getChildren().add(card);

        Scene scene = new Scene(root, 1024, 700);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Shop Management System - Login");
        stage.show();
    }
}
