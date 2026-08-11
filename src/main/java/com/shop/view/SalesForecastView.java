package com.shop.view;

import com.shop.dao.SaleDAO;
import com.shop.util.SalesForecaster;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.beans.property.SimpleStringProperty;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class SalesForecastView {
    private final SaleDAO saleDAO = new SaleDAO();
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd MMM");
    private static final DateTimeFormatter FULL_LABEL = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");

    public Node getView() {
        Map<String, Double> history = saleDAO.getDailyRevenue(35);
        var forecast = SalesForecaster.forecastNextDays(history, 7);

        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label title = new Label("🔮 Sales Forecast — Next 7 Days");
        title.getStyleClass().add("section-title");
        Label sub = new Label("Predicted revenue based on your recent sales trends and weekday patterns.");
        sub.getStyleClass().add("sub-label");
        headerCard.getChildren().addAll(title, sub);

        double totalForecast = forecast.stream().mapToDouble(f -> f.value).sum();
        double avgForecast = forecast.isEmpty() ? 0 : totalForecast / forecast.size();
        var best = forecast.stream().max((a, b) -> Double.compare(a.value, b.value)).orElse(null);

        FlowPane statsGrid = new FlowPane(16, 16);
        statsGrid.getChildren().add(createStatCard("📅", String.format("₹%.2f", totalForecast), "Next 7 Days Forecast"));
        statsGrid.getChildren().add(createStatCard("📈", String.format("₹%.2f", avgForecast), "Avg Daily Forecast"));
        statsGrid.getChildren().add(createStatCard("🌟", best != null ? String.format("₹%.2f", best.value) : "—",
                best != null ? "Best Day: " + best.date.format(DAY_LABEL) : "Best Day"));

        VBox chartCard = new VBox(8);
        chartCard.getStyleClass().add("card");
        Label chartTitle = new Label("📊 Actual vs Forecast (last 14 days + next 7)");
        chartTitle.getStyleClass().add("section-title");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setPrefHeight(320);

        XYChart.Series<String, Number> actualSeries = new XYChart.Series<>();
        actualSeries.setName("Actual");
        XYChart.Series<String, Number> forecastSeries = new XYChart.Series<>();
        forecastSeries.setName("Forecast");

        LocalDate today = LocalDate.now();
        for (int i = 13; i >= 0; i--) {
            String key = today.minusDays(i).toString();
            double value = history.getOrDefault(key, 0.0);
            actualSeries.getData().add(new XYChart.Data<>(today.minusDays(i).format(DAY_LABEL), value));
        }
        for (var f : forecast) {
            forecastSeries.getData().add(new XYChart.Data<>(f.date.format(DAY_LABEL), f.value));
        }
        chart.getData().addAll(actualSeries, forecastSeries);
        chartCard.getChildren().addAll(chartTitle, chart);

        VBox tableCard = new VBox(8);
        tableCard.getStyleClass().add("card");
        Label tableTitle = new Label("🗓️ Day-by-Day Forecast");
        tableTitle.getStyleClass().add("section-title");

        TableView<SalesForecaster.ForecastDay> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<SalesForecaster.ForecastDay, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().date.format(FULL_LABEL)));

        TableColumn<SalesForecaster.ForecastDay, String> weekdayCol = new TableColumn<>("Weekday");
        weekdayCol.setCellValueFactory(p -> new SimpleStringProperty(dayName(p.getValue().date.getDayOfWeek())));

        TableColumn<SalesForecaster.ForecastDay, String> valueCol = new TableColumn<>("Predicted Revenue");
        valueCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().value)));
        valueCol.setCellFactory(col -> new TableCell<SalesForecaster.ForecastDay, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                if (!empty && item != null) getStyleClass().add("cell-accent");
            }
        });

        table.getColumns().addAll(dateCol, weekdayCol, valueCol);
        ObservableList<SalesForecaster.ForecastDay> rows = FXCollections.observableArrayList();
        rows.addAll(forecast);
        table.setItems(rows);

        tableCard.getChildren().addAll(tableTitle, table);

        root.getChildren().addAll(headerCard, statsGrid, chartCard, tableCard);
        return new ScrollPane(root) {{ setFitToWidth(true); setStyle("-fx-background-color: transparent;"); }};
    }

    private VBox createStatCard(String icon, String value, String label) {
        VBox card = new VBox(6);
        card.getStyleClass().add("stat-card");
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("stat-icon");
        Label valLabel = new Label(value);
        valLabel.getStyleClass().addAll("stat-value", "text-accent");
        Label titleLabel = new Label(label);
        titleLabel.getStyleClass().add("stat-label");
        card.getChildren().addAll(iconLabel, valLabel, titleLabel);
        card.setPrefWidth(220);
        return card;
    }

    private String dayName(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "Monday";
            case TUESDAY -> "Tuesday";
            case WEDNESDAY -> "Wednesday";
            case THURSDAY -> "Thursday";
            case FRIDAY -> "Friday";
            case SATURDAY -> "Saturday";
            default -> "Sunday";
        };
    }
}
