package com.shop.view;

import com.shop.dao.ActivityLogDAO;
import com.shop.model.ActivityLog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.Arrays;
import java.util.List;

public class ActivityLogView {
    private final ActivityLogDAO logDAO = new ActivityLogDAO();
    private final ObservableList<ActivityLog> logList = FXCollections.observableArrayList();
    private final TableView<ActivityLog> table = new TableView<>();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        HBox controlBar = new HBox(12);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search by action, user, or details...");
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        ComboBox<String> actionFilter = new ComboBox<>();
        actionFilter.getItems().add("All Actions");
        actionFilter.getItems().addAll(getKnownActions());
        actionFilter.setValue("All Actions");

        Runnable load = () -> {
            String q = searchField.getText().trim();
            if (!"All Actions".equals(actionFilter.getValue()) && actionFilter.getValue() != null) {
                q = (q.isEmpty() ? "" : q + " ") + actionFilter.getValue();
            }
            loadLogs(q);
        };

        searchField.textProperty().addListener((obs, o, n) -> load.run());
        actionFilter.valueProperty().addListener((obs, o, n) -> load.run());

        Label countLabel = new Label();
        countLabel.getStyleClass().add("sub-label");

        controlBar.getChildren().addAll(searchField, actionFilter, countLabel);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<ActivityLog, String> timeCol = new TableColumn<>("Timestamp");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("createdAtLabel"));
        timeCol.setPrefWidth(160);

        TableColumn<ActivityLog, String> userCol = new TableColumn<>("User");
        userCol.setCellValueFactory(new PropertyValueFactory<>("userName"));

        TableColumn<ActivityLog, String> actionCol = new TableColumn<>("Action");
        actionCol.setCellValueFactory(new PropertyValueFactory<>("action"));
        actionCol.setCellFactory(col -> new TableCell<ActivityLog, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    String lower = item.toLowerCase();
                    if (lower.contains("sale") || lower.contains("payment")) {
                        badge.getStyleClass().add("badge-active");
                    } else if (lower.contains("delete") || lower.contains("logout")) {
                        badge.getStyleClass().add("badge-inactive");
                    } else if (lower.contains("edit") || lower.contains("update")) {
                        badge.getStyleClass().add("badge-warning");
                    } else {
                        badge.getStyleClass().add("badge-info");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<ActivityLog, String> detailsCol = new TableColumn<>("Details");
        detailsCol.setCellValueFactory(new PropertyValueFactory<>("details"));

        table.getColumns().addAll(timeCol, userCol, actionCol, detailsCol);
        table.setItems(logList);
        table.setPlaceholder(new Label("No activity recorded yet."));

        root.getChildren().addAll(controlBar, table);
        loadLogs("");
        return root;
    }

    private List<String> getKnownActions() {
        return Arrays.asList("LOGIN", "LOGOUT", "SALE", "PRODUCT_ADD", "PRODUCT_UPDATE",
                "PRODUCT_DELETE", "CUSTOMER_ADD", "EXPENSE_ADD", "REFUND", "ORDER",
                "BACKUP", "STOCK_ADJUST");
    }

    private void loadLogs(String query) {
        logList.clear();
        logList.addAll(logDAO.find(query));
    }
}
