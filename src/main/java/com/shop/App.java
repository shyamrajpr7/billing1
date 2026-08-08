package com.shop;

import com.shop.dao.DatabaseManager;
import com.shop.view.LoginView;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Initialize SQLite database and tables
        DatabaseManager.getInstance();

        // Launch Login View
        LoginView loginView = new LoginView(primaryStage);
        loginView.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
