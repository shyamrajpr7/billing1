package com.shop.view;

import com.shop.dao.SaleDAO;
import com.shop.model.BestSeller;
import com.shop.model.CategorySales;
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

public class TopSellersView {
    private final SaleDAO saleDAO = new SaleDAO();
    private final ComboBox<String> rangeCombo = new ComboBox<>();
    private final ObservableList<BestSeller> sellerList = FXCollections.observableArrayList();
    private final TableView<BestSeller> table = new TableView<>();
    private final Label statUnits = new Label();
    private final Label statTopProduct = new Label();
    private final Label statTopCategory = new Label();
    private final BarChart<String, Number> barChart = buildBarChart();
    private final PieChart pieChart = new PieChart();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label title = new Label("🏆 Top Sellers");
        title.getStyleClass().add("section-title");
        Label sub = new Label("Your best-performing products and categories.");
        sub.getStyleClass().add("sub-label");

        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER_LEFT);
        rangeCombo.getItems().addAll("Last 7 Days", "Last 30 Days", "Last 90 Days");
        rangeCombo.setValue("Last 30 Days");
        rangeCombo.setOnAction(e -> refresh());
        controls.getChildren().addAll(new Label("Range:"), rangeCombo);

        headerCard.getChildren().addAll(title, sub, controls);

        FlowPane statsGrid = new FlowPane(16, 16);
        statsGrid.getChildren().add(statCard("📦", "Total Units Sold", statUnits));
        statsGrid.getChildren().add(statCard("🥇", "Top Product", statTopProduct));
        statsGrid.getChildren().add(statCard("🗂️", "Top Category", statTopCategory));

        HBox chartsRow = new HBox(16);

        VBox barCard = new VBox(8);
        barCard.getStyleClass().add("card");
        HBox.setHgrow(barCard, Priority.ALWAYS);
        Label barTitle = new Label("💵 Top 10 Products by Revenue");
        barTitle.getStyleClass().add("section-title");

        barChart.setLegendVisible(false);
        barChart.setAnimated(false);
        barChart.setPrefHeight(300);
        barCard.getChildren().addAll(barTitle, barChart);
        VBox.setVgrow(barChart, Priority.ALWAYS);

        VBox pieCard = new VBox(8);
        pieCard.getStyleClass().add("card");
        Label pieTitle = new Label("🧩 Revenue by Category");
        pieTitle.getStyleClass().add("section-title");
        pieChart.setAnimated(false);
        pieChart.setLegendVisible(true);
        pieChart.setLabelsVisible(true);
        pieChart.setPrefHeight(300);
        pieCard.getChildren().addAll(pieTitle, pieChart);

        chartsRow.getChildren().addAll(barCard, pieCard);
        HBox.setHgrow(pieCard, Priority.ALWAYS);

        VBox tableCard = new VBox(8);
        tableCard.getStyleClass().add("card");
        Label tableTitle = new Label("📈 Detailed Ranking");
        tableTitle.getStyleClass().add("section-title");

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<BestSeller, String> rankCol = new TableColumn<>("#");
        rankCol.setCellValueFactory(p -> new SimpleStringProperty(String.valueOf(table.getItems().indexOf(p.getValue()) + 1)));
        rankCol.setPrefWidth(50);

        TableColumn<BestSeller, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<BestSeller, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<BestSeller, Integer> unitsCol = new TableColumn<>("Units Sold");
        unitsCol.setCellValueFactory(new PropertyValueFactory<>("units"));

        TableColumn<BestSeller, String> revCol = new TableColumn<>("Revenue");
        revCol.setCellValueFactory(new PropertyValueFactory<>("formattedRevenue"));
        revCol.setCellFactory(col -> new TableCell<BestSeller, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                if (!empty && item != null) getStyleClass().add("cell-accent");
            }
        });

        table.getColumns().addAll(rankCol, nameCol, catCol, unitsCol, revCol);
        table.setItems(sellerList);
        table.setPlaceholder(new Label("No sales recorded in this period yet."));

        tableCard.getChildren().addAll(tableTitle, table);

        root.getChildren().addAll(headerCard, statsGrid, chartsRow, tableCard);
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
        card.setPrefWidth(260);
        return card;
    }

    private BarChart<String, Number> buildBarChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        return new BarChart<>(xAxis, yAxis);
    }

    private void refresh() {
        int days = switch (rangeCombo.getValue()) {
            case "Last 7 Days" -> 7;
            case "Last 90 Days" -> 90;
            default -> 30;
        };
        List<BestSeller> sellers = saleDAO.getTopSellers(days, 10);
        sellerList.clear();
        sellerList.addAll(sellers);

        int totalUnits = sellers.stream().mapToInt(BestSeller::getUnits).sum();
        statUnits.setText(String.valueOf(totalUnits));
        statTopProduct.setText(!sellers.isEmpty() ? sellers.get(0).getName() : "—");

        List<CategorySales> cats = saleDAO.getTopCategories(days);
        statTopCategory.setText(!cats.isEmpty() ? cats.get(0).getCategory() : "—");

        barChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (BestSeller bs : sellers) {
            series.getData().add(new XYChart.Data<>(bs.getName(), bs.getRevenue()));
        }
        barChart.getData().add(series);

        pieChart.getData().clear();
        for (CategorySales cs : cats) {
            pieChart.getData().add(new PieChart.Data(cs.getCategory() + " ₹" + String.format("%.0f", cs.getRevenue()),
                    cs.getRevenue()));
        }
    }
}
