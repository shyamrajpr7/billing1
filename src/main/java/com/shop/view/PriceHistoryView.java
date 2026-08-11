package com.shop.view;

import com.shop.dao.PriceHistoryDAO;
import com.shop.model.PriceChange;
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

public class PriceHistoryView {
    private final PriceHistoryDAO dao = new PriceHistoryDAO();
    private final ObservableList<PriceChange> list = FXCollections.observableArrayList();
    private final TableView<PriceChange> table = new TableView<>();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label title = new Label("🏷️ Price Change Audit Trail");
        title.getStyleClass().add("section-title");
        Label sub = new Label("Every cost & selling price change is recorded automatically with who made it.");
        sub.getStyleClass().add("sub-label");
        headerCard.getChildren().addAll(title, sub);

        HBox controlBar = new HBox(12);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search by product or user...");
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        ComboBox<String> fieldFilter = new ComboBox<>();
        fieldFilter.getItems().addAll("All Fields", "Cost Price", "Selling Price");
        fieldFilter.setValue("All Fields");

        Label countLabel = new Label();
        countLabel.getStyleClass().add("sub-label");

        Runnable load = () -> {
            String q = searchField.getText().trim();
            String f = fieldFilter.getValue();
            loadData(q, f != null ? f : "All Fields");
        };
        searchField.textProperty().addListener((obs, o, n) -> load.run());
        fieldFilter.valueProperty().addListener((obs, o, n) -> load.run());

        controlBar.getChildren().addAll(searchField, fieldFilter, countLabel);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<PriceChange, String> dateCol = new TableColumn<>("Changed On");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("formattedDate"));
        dateCol.setPrefWidth(150);

        TableColumn<PriceChange, String> productCol = new TableColumn<>("Product");
        productCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

        TableColumn<PriceChange, String> fieldCol = new TableColumn<>("Field");
        fieldCol.setCellValueFactory(new PropertyValueFactory<>("field"));
        fieldCol.setCellFactory(col -> new TableCell<PriceChange, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("Selling Price".equals(item) ? "badge-warning" : "badge-info");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<PriceChange, String> oldCol = new TableColumn<>("Old Price");
        oldCol.setCellValueFactory(new PropertyValueFactory<>("formattedOld"));
        oldCol.setCellFactory(col -> new TableCell<PriceChange, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                if (!empty && item != null) getStyleClass().add("cell-danger");
            }
        });

        TableColumn<PriceChange, String> newCol = new TableColumn<>("New Price");
        newCol.setCellValueFactory(new PropertyValueFactory<>("formattedNew"));
        newCol.setCellFactory(col -> new TableCell<PriceChange, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                if (!empty && item != null) getStyleClass().add("cell-accent");
            }
        });

        TableColumn<PriceChange, String> byCol = new TableColumn<>("Changed By");
        byCol.setCellValueFactory(new PropertyValueFactory<>("userName"));

        table.getColumns().addAll(dateCol, productCol, fieldCol, oldCol, newCol, byCol);
        table.setItems(list);
        table.setPlaceholder(new Label("No price changes recorded yet."));

        root.getChildren().addAll(headerCard, controlBar, table);
        loadData("", "All Fields");
        return root;
    }

    private void loadData(String query, String field) {
        list.clear();
        List<PriceChange> rows = dao.find(query, field);
        list.addAll(rows);
    }
}
