package com.shop.view;

import com.shop.dao.ExpenseDAO;
import com.shop.dao.SaleDAO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.Map;

public class AnalyticsView {
    private final SaleDAO saleDAO = new SaleDAO();
    private final ExpenseDAO expenseDAO = new ExpenseDAO();
    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("dd MMM");

    private final ComboBox<String> rangeCombo = new ComboBox<>();
    private final LineChart<String, Number> trendChart = buildTrendChart();
    private final BarChart<String, Number> salesChart = buildSalesChart();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        Label title = new Label("📊  Sales Analytics");
        title.getStyleClass().add("section-title");

        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);
        rangeCombo.getItems().addAll("Last 7 Days", "Last 14 Days", "Last 30 Days");
        rangeCombo.setValue("Last 14 Days");
        rangeCombo.setOnAction(e -> refresh());

        Label rangeLabel = new Label("Range:");
        rangeLabel.getStyleClass().add("form-label");
        controls.getChildren().addAll(rangeLabel, rangeCombo);

        VBox trendCard = new VBox(8);
        trendCard.getStyleClass().add("card");
        Label trendTitle = new Label("💵 Revenue vs Expenses");
        trendTitle.getStyleClass().add("section-title");
        trendChart.setPrefHeight(320);
        trendCard.getChildren().addAll(trendTitle, trendChart);

        VBox salesCard = new VBox(8);
        salesCard.getStyleClass().add("card");
        Label salesTitle = new Label("🛒 Daily Sales Trend");
        salesTitle.getStyleClass().add("section-title");
        salesChart.setPrefHeight(280);
        salesCard.getChildren().addAll(salesTitle, salesChart);

        root.getChildren().addAll(title, controls, trendCard, salesCard);
        refresh();
        return root;
    }

    private LineChart<String, Number> buildTrendChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setLegendVisible(true);
        chart.setAnimated(false);
        return chart;
    }

    private BarChart<String, Number> buildSalesChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        return chart;
    }

    private void refresh() {
        int days = switch (rangeCombo.getValue()) {
            case "Last 7 Days" -> 7;
            case "Last 30 Days" -> 30;
            default -> 14;
        };

        Map<String, Double> revenue = saleDAO.getDailyRevenue(days);
        Map<String, Double> expenses = expenseDAO.getDailyTotals(days);

        trendChart.getData().clear();
        XYChart.Series<String, Number> revSeries = new XYChart.Series<>();
        revSeries.setName("Revenue");
        XYChart.Series<String, Number> expSeries = new XYChart.Series<>();
        expSeries.setName("Expenses");

        salesChart.getData().clear();
        XYChart.Series<String, Number> salesSeries = new XYChart.Series<>();
        salesSeries.setName("Revenue");

        for (Map.Entry<String, Double> entry : revenue.entrySet()) {
            String label = formatDay(entry.getKey());
            revSeries.getData().add(new XYChart.Data<>(label, entry.getValue()));
            salesSeries.getData().add(new XYChart.Data<>(label, entry.getValue()));
        }
        for (Map.Entry<String, Double> entry : expenses.entrySet()) {
            expSeries.getData().add(new XYChart.Data<>(formatDay(entry.getKey()), entry.getValue()));
        }

        trendChart.getData().addAll(revSeries, expSeries);
        salesChart.getData().add(salesSeries);
    }

    private String formatDay(String iso) {
        try {
            return java.time.LocalDate.parse(iso).format(LABEL);
        } catch (Exception e) {
            return iso;
        }
    }
}
