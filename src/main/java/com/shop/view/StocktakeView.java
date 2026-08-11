package com.shop.view;

import com.shop.dao.ProductDAO;
import com.shop.dao.StocktakeDAO;
import com.shop.model.Product;
import com.shop.model.Stocktake;
import com.shop.model.StocktakeItem;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StocktakeView {
    private final StocktakeDAO stocktakeDAO = new StocktakeDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final ObservableList<Stocktake> stocktakeList = FXCollections.observableArrayList();
    private final TableView<Stocktake> table = new TableView<>();
    private final Label statsLabel = new Label();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label title = new Label("📋 Stocktake / Inventory Count");
        title.getStyleClass().add("section-title");
        Label sub = new Label("Create a count sheet, enter actual quantities, and apply automatic stock adjustments.");
        sub.getStyleClass().add("sub-label");
        statsLabel.getStyleClass().add("sub-label");
        headerCard.getChildren().addAll(title, sub, statsLabel);

        HBox actionBar = new HBox(12);
        actionBar.setAlignment(Pos.CENTER_LEFT);
        TextField nameField = new TextField();
        nameField.setPromptText("Stocktake name, e.g. 'Monthly count 2026'");
        HBox.setHgrow(nameField, Priority.ALWAYS);
        Button startBtn = new Button("📋  Start New Stocktake");
        startBtn.getStyleClass().add("btn-primary");
        startBtn.setOnAction(e -> startStocktake(nameField.getText().trim()));
        actionBar.getChildren().addAll(nameField, startBtn);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Stocktake, String> dateCol = new TableColumn<>("Started");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("formattedDate"));
        dateCol.setPrefWidth(110);

        TableColumn<Stocktake, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Stocktake, String> byCol = new TableColumn<>("Started By");
        byCol.setCellValueFactory(new PropertyValueFactory<>("createdByName"));

        TableColumn<Stocktake, Integer> itemsCol = new TableColumn<>("Items");
        itemsCol.setCellValueFactory(new PropertyValueFactory<>("itemCount"));

        TableColumn<Stocktake, Integer> adjCol = new TableColumn<>("Adjusted");
        adjCol.setCellValueFactory(new PropertyValueFactory<>("adjustedCount"));

        TableColumn<Stocktake, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<Stocktake, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("OPEN".equals(item) ? "badge-active" : "badge-inactive");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Stocktake, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(180);
        actionCol.setCellFactory(col -> new TableCell<Stocktake, Void>() {
            private final Button countBtn = new Button("🔢 Count");
            private final Button cancelBtn = new Button("Cancel");
            private final HBox box = new HBox(6, countBtn, cancelBtn);
            {
                countBtn.getStyleClass().addAll("btn-primary", "btn-small");
                cancelBtn.getStyleClass().addAll("btn-danger", "btn-small");
                countBtn.setOnAction(e -> countStocktake(getTableRow().getItem()));
                cancelBtn.setOnAction(e -> cancelStocktake(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    boolean open = getTableRow().getItem().isOpen();
                    countBtn.setDisable(!open);
                    cancelBtn.setDisable(!open);
                    setGraphic(box);
                }
            }
        });

        table.getColumns().addAll(dateCol, nameCol, byCol, itemsCol, adjCol, statusCol, actionCol);
        table.setItems(stocktakeList);
        table.setPlaceholder(new Label("No stocktakes yet. Click 'Start New Stocktake'."));

        root.getChildren().addAll(headerCard, actionBar, table);
        refresh();
        return root;
    }

    private void startStocktake(String name) {
        if (name.isEmpty()) {
            showAlert("Please enter a name for the stocktake.");
            return;
        }
        Stocktake st = new Stocktake();
        st.setName(name);
        for (Product p : productDAO.findAll()) {
            StocktakeItem item = new StocktakeItem();
            item.setProductId(p.getId());
            item.setProductName(p.getName());
            item.setBarcode(p.getBarcode());
            item.setExpectedQty(p.getQuantity());
            item.setCountedQty(p.getQuantity());
            st.getItems().add(item);
        }
        if (st.getItems().isEmpty()) {
            showAlert("Cannot start a stocktake with no products in inventory.");
            return;
        }
        if (stocktakeDAO.create(st)) {
            refresh();
            countStocktake(st);
        } else {
            showAlert("Failed to start stocktake.");
        }
    }

    private void countStocktake(Stocktake st) {
        if (st == null) return;
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Count — " + st.getName());

        VBox box = new VBox(12);
        box.setPadding(new Insets(15));
        box.getStyleClass().add("card");

        Label title = new Label("🔢 Enter counted quantities for '" + st.getName() + "'");
        title.getStyleClass().add("section-title");

        Map<Integer, Integer> counts = new HashMap<>();
        for (StocktakeItem item : st.getItems()) {
            counts.put(item.getProductId(), item.getCountedQty());
        }

        TableView<StocktakeItem> itemTable = new TableView<>();
        itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        itemTable.setPrefHeight(420);

        TableColumn<StocktakeItem, String> pCol = new TableColumn<>("Product");
        pCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

        TableColumn<StocktakeItem, Integer> expCol = new TableColumn<>("Expected");
        expCol.setCellValueFactory(new PropertyValueFactory<>("expectedQty"));
        expCol.setPrefWidth(80);

        TableColumn<StocktakeItem, Void> cntCol = new TableColumn<>("Counted Qty");
        cntCol.setPrefWidth(130);
        cntCol.setCellFactory(col -> new TableCell<StocktakeItem, Void>() {
            private final TextField field = new TextField();
            private final SpinnerValueFactory.IntegerSpinnerValueFactory vf =
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100000, 0);
            private final Spinner<Integer> spinner = new Spinner<>(vf);
            {
                spinner.setEditable(true);
                spinner.setPrefWidth(100);
                spinner.valueProperty().addListener((obs, o, n) -> {
                    StocktakeItem item = getTableRow() != null ? getTableRow().getItem() : null;
                    if (item != null && n != null) {
                        counts.put(item.getProductId(), n);
                        item.setCountedQty(n);
                        getTableView().refresh();
                    }
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                StocktakeItem item = getTableRow() != null ? getTableRow().getItem() : null;
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    vf.setValue(counts.getOrDefault(item.getProductId(), item.getExpectedQty()));
                    setGraphic(spinner);
                }
            }
        });

        TableColumn<StocktakeItem, String> varCol = new TableColumn<>("Variance");
        varCol.setCellValueFactory(p -> new SimpleStringProperty(String.valueOf(p.getValue().getVariance())));
        varCol.setCellFactory(col -> new TableCell<StocktakeItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                if (!empty && item != null) {
                    int v = Integer.parseInt(item);
                    getStyleClass().removeAll("cell-accent", "cell-danger");
                    if (v > 0) getStyleClass().add("cell-accent");
                    else if (v < 0) getStyleClass().add("cell-danger");
                }
            }
        });

        itemTable.getColumns().addAll(pCol, expCol, cntCol, varCol);
        itemTable.setItems(FXCollections.observableArrayList(st.getItems()));

        Button completeBtn = new Button("✅ Complete & Apply Adjustments");
        completeBtn.getStyleClass().add("btn-primary");
        completeBtn.setMaxWidth(Double.MAX_VALUE);
        completeBtn.setOnAction(e -> {
            for (StocktakeItem item : st.getItems()) {
                item.setCountedQty(counts.getOrDefault(item.getProductId(), item.getExpectedQty()));
            }
            if (stocktakeDAO.saveCounts(st.getId(), st.getItems())
                    && stocktakeDAO.complete(st)) {
                dialog.close();
                refresh();
                showAlert("✅ Stocktake completed. " + st.getAdjustedCount() + " product(s) adjusted.");
            } else {
                showAlert("❌ Failed to complete stocktake.");
            }
        });

        Button cancelBtn = new Button("Cancel Stocktake");
        cancelBtn.getStyleClass().add("btn-danger");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setOnAction(e -> {
            if (confirm("Cancel this stocktake? Counted quantities will be discarded.")) {
                stocktakeDAO.cancel(st.getId());
                dialog.close();
                refresh();
            }
        });

        box.getChildren().addAll(title, itemTable, completeBtn, cancelBtn);

        Scene scene = new Scene(box, 680, 560);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void cancelStocktake(Stocktake st) {
        if (st == null) return;
        if (confirm("Cancel stocktake '" + st.getName() + "'?")) {
            stocktakeDAO.cancel(st.getId());
            refresh();
        }
    }

    private void refresh() {
        stocktakeList.clear();
        List<Stocktake> all = stocktakeDAO.findAll();
        stocktakeList.addAll(all);
        long open = all.stream().filter(Stocktake::isOpen).count();
        statsLabel.setText("Open: " + open + "  •  Total: " + all.size());
    }

    private boolean confirm(String msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText(null);
        alert.setTitle("Confirm");
        return alert.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Stocktake");
        alert.showAndWait();
    }
}
