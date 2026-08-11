package com.shop.view;

import com.shop.dao.SupplierPerformanceDAO;
import com.shop.model.SupplierPerformance;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.List;

public class SupplierPerformanceView {
    private final SupplierPerformanceDAO dao = new SupplierPerformanceDAO();
    private final ObservableList<SupplierPerformance> list = FXCollections.observableArrayList();
    private final TableView<SupplierPerformance> table = new TableView<>();
    private final Label statSuppliers = new Label();
    private final Label statSpend = new Label();
    private final Label statPending = new Label();
    private final Label statAvgDays = new Label();
    private final BarChart<String, Number> barChart = buildBarChart();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label title = new Label("🤝 Supplier Performance");
        title.getStyleClass().add("section-title");
        Label sub = new Label("Spend, delivery speed and reliability per supplier.");
        sub.getStyleClass().add("sub-label");

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> refresh());
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(sub, Priority.ALWAYS);
        headerRow.getChildren().addAll(sub, refreshBtn);

        headerCard.getChildren().addAll(title, headerRow);

        FlowPane statsGrid = new FlowPane(16, 16);
        statsGrid.getChildren().add(statCard("🏢", "Total Suppliers", statSuppliers));
        statsGrid.getChildren().add(statCard("💸", "Total Spend", statSpend));
        statsGrid.getChildren().add(statCard("⏳", "Pending POs", statPending));
        statsGrid.getChildren().add(statCard("🚚", "Avg Delivery", statAvgDays));

        VBox barCard = new VBox(8);
        barCard.getStyleClass().add("card");
        Label barTitle = new Label("💰 Top Suppliers by Spend");
        barTitle.getStyleClass().add("section-title");
        barChart.setLegendVisible(false);
        barChart.setAnimated(false);
        barChart.setPrefHeight(260);
        barCard.getChildren().addAll(barTitle, barChart);

        VBox tableCard = new VBox(8);
        tableCard.getStyleClass().add("card");
        Label tableTitle = new Label("📋 Supplier Scorecard");
        tableTitle.getStyleClass().add("section-title");

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<SupplierPerformance, String> nameCol = new TableColumn<>("Supplier");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
        nameCol.setPrefWidth(180);

        TableColumn<SupplierPerformance, String> contactCol = new TableColumn<>("Contact");
        contactCol.setCellValueFactory(p -> new SimpleStringProperty(
                (p.getValue().getContactPerson() == null || p.getValue().getContactPerson().isEmpty())
                        ? (p.getValue().getPhone() != null ? p.getValue().getPhone() : "—")
                        : p.getValue().getContactPerson()));

        TableColumn<SupplierPerformance, Integer> ordersCol = new TableColumn<>("POs");
        ordersCol.setCellValueFactory(new PropertyValueFactory<>("totalOrders"));

        TableColumn<SupplierPerformance, Integer> pendingCol = new TableColumn<>("Pending");
        pendingCol.setCellValueFactory(new PropertyValueFactory<>("pendingOrders"));
        pendingCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.valueOf(item));
                getStyleClass().removeAll("badge-warning");
                if (!empty && item != null && item > 0) {
                    getStyleClass().add("badge-warning");
                    setText(item + " ⏳");
                }
            }
        });

        TableColumn<SupplierPerformance, String> spendCol = new TableColumn<>("Total Spend");
        spendCol.setCellValueFactory(new PropertyValueFactory<>("formattedSpend"));
        spendCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                if (!empty && item != null) getStyleClass().add("cell-accent");
            }
        });

        TableColumn<SupplierPerformance, Integer> itemsCol = new TableColumn<>("Units Received");
        itemsCol.setCellValueFactory(new PropertyValueFactory<>("itemsReceived"));

        TableColumn<SupplierPerformance, String> daysCol = new TableColumn<>("Avg Delivery");
        daysCol.setCellValueFactory(new PropertyValueFactory<>("formattedAvgDays"));

        TableColumn<SupplierPerformance, String> lastCol = new TableColumn<>("Last Order");
        lastCol.setCellValueFactory(p -> new SimpleStringProperty(
                p.getValue().getLastOrderDate() != null ? p.getValue().getLastOrderDate() : "—"));

        table.getColumns().addAll(nameCol, contactCol, ordersCol, pendingCol, spendCol, itemsCol, daysCol, lastCol);
        table.setItems(list);
        table.setPlaceholder(new Label("No suppliers yet. Add suppliers to see performance."));

        tableCard.getChildren().addAll(tableTitle, table);

        root.getChildren().addAll(headerCard, statsGrid, barCard, tableCard);
        refresh();
        return new ScrollPane(root) {{ setFitToWidth(true); setStyle("-fx-background-color: transparent;"); }};
    }

    private VBox statCard(String icon, String label, Label valueLabel) {
        VBox card = new VBox(6);
        card.getStyleClass().add("stat-card");
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("stat-icon");
        valueLabel.getStyleClass().addAll("stat-value", "text-accent");
        valueLabel.setWrapText(true);
        Label titleLabel = new Label(label);
        titleLabel.getStyleClass().add("stat-label");
        card.getChildren().addAll(iconLabel, valueLabel, titleLabel);
        card.setPrefWidth(220);
        return card;
    }

    private BarChart<String, Number> buildBarChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        return new BarChart<>(xAxis, yAxis);
    }

    private void refresh() {
        List<SupplierPerformance> stats = dao.getStats();
        list.clear();
        list.addAll(stats);

        statSuppliers.setText(String.valueOf(stats.size()));
        double totalSpend = stats.stream().mapToDouble(SupplierPerformance::getTotalSpend).sum();
        statSpend.setText(String.format("₹%,.0f", totalSpend));
        int pending = stats.stream().mapToInt(SupplierPerformance::getPendingOrders).sum();
        statPending.setText(String.valueOf(pending));
        long received = stats.stream().filter(s -> s.getReceivedOrders() > 0).count();
        double avgDays = received > 0
                ? stats.stream().filter(s -> s.getReceivedOrders() > 0)
                        .mapToDouble(s -> s.getAvgDaysToReceive() * s.getReceivedOrders()).sum() / received
                : 0;
        statAvgDays.setText(avgDays > 0 ? String.format("%.1f days", avgDays) : "—");

        barChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (SupplierPerformance sp : stats.stream().limit(8).toList()) {
            series.getData().add(new XYChart.Data<>(sp.getSupplierName(), sp.getTotalSpend()));
        }
        barChart.getData().add(series);
    }
}
