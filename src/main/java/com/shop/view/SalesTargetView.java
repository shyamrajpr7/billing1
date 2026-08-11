package com.shop.view;

import com.shop.dao.SaleDAO;
import com.shop.dao.SalesTargetDAO;
import com.shop.model.SalesTarget;
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

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class SalesTargetView {
    private final SalesTargetDAO targetDAO = new SalesTargetDAO();
    private final SaleDAO saleDAO = new SaleDAO();

    private final Label statTarget = new Label();
    private final Label statActual = new Label();
    private final Label statProjected = new Label();
    private final Label statStatus = new Label();
    private final ProgressIndicator gauge = new ProgressIndicator(0);
    private final Label gaugePct = new Label();
    private final Label gaugeText = new Label();
    private final ObservableList<SalesTarget> targetList = FXCollections.observableArrayList();
    private final TableView<SalesTarget> table = new TableView<>();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label title = new Label("🎯 Sales Targets");
        title.getStyleClass().add("section-title");
        Label sub = new Label("Set monthly revenue goals and track progress in real time.");
        sub.getStyleClass().add("sub-label");

        Button setBtn = new Button("⚙️  Set / Edit Target");
        setBtn.getStyleClass().add("btn-primary");
        setBtn.setOnAction(e -> showTargetDialog());

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(sub, Priority.ALWAYS);
        headerRow.getChildren().addAll(sub, setBtn);
        headerCard.getChildren().addAll(title, headerRow);

        HBox gaugeRow = new HBox(20);

        VBox gaugeCard = new VBox(8);
        gaugeCard.getStyleClass().add("card");
        gaugeCard.setPadding(new Insets(20));
        Label gaugeTitle = new Label("📈 This Month Progress");
        gaugeTitle.getStyleClass().add("section-title");

        gauge.setPrefSize(230, 230);
        gauge.getStyleClass().add("target-gauge");
        gaugePct.getStyleClass().addAll("stat-value", "text-accent");
        gaugePct.setStyle("-fx-font-size: 42px; -fx-font-weight: bold;");
        gaugeText.getStyleClass().add("sub-label");
        gaugeText.setWrapText(true);

        StackPane gaugeStack = new StackPane(gauge, new VBox(4, gaugePct, gaugeText));
        gaugeStack.setAlignment(Pos.CENTER);

        gaugeCard.getChildren().addAll(gaugeTitle, gaugeStack);

        VBox statsCard = new VBox(10);
        statsCard.getStyleClass().add("card");
        statsCard.setPadding(new Insets(20));
        Label statsTitle = new Label("🏁 Monthly Snapshot");
        statsTitle.getStyleClass().add("section-title");
        VBox.setVgrow(statsCard, Priority.ALWAYS);
        statsCard.getChildren().addAll(statsTitle,
                statRow("🎯 Monthly Target", statTarget),
                statRow("💰 Revenue (Month to Date)", statActual),
                statRow("🔮 Projected by Month End", statProjected),
                statRow("📍 Status", statStatus));

        gaugeRow.getChildren().addAll(gaugeCard, statsCard);
        HBox.setHgrow(gaugeCard, Priority.ALWAYS);
        HBox.setHgrow(statsCard, Priority.ALWAYS);

        VBox tableCard = new VBox(8);
        tableCard.getStyleClass().add("card");
        Label tableTitle = new Label("🗓️ Target History");
        tableTitle.getStyleClass().add("section-title");

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<SalesTarget, String> monthCol = new TableColumn<>("Month");
        monthCol.setCellValueFactory(new PropertyValueFactory<>("monthLabel"));

        TableColumn<SalesTarget, String> targetCol = new TableColumn<>("Target");
        targetCol.setCellValueFactory(new PropertyValueFactory<>("formattedTarget"));

        TableColumn<SalesTarget, String> actualCol = new TableColumn<>("Actual");
        actualCol.setCellValueFactory(p -> new SimpleStringProperty(
                String.format("₹%,.0f", saleDAO.getTotalRevenueForMonth(p.getValue().getMonth()))));

        TableColumn<SalesTarget, String> pctCol = new TableColumn<>("Achieved");
        pctCol.setCellValueFactory(p -> {
            double target = p.getValue().getTarget();
            double actual = saleDAO.getTotalRevenueForMonth(p.getValue().getMonth());
            double pct = target > 0 ? actual / target * 100 : 0;
            return new SimpleStringProperty(String.format("%.0f%%", pct));
        });

        TableColumn<SalesTarget, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(p -> {
            double target = p.getValue().getTarget();
            double actual = saleDAO.getTotalRevenueForMonth(p.getValue().getMonth());
            return new SimpleStringProperty(target > 0 && actual >= target ? "✓ Achieved" : "In Progress");
        });
        statusCol.setCellFactory(col -> new TableCell<SalesTarget, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("✓ Achieved".equals(item) ? "badge-active" : "badge-warning");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        table.getColumns().addAll(monthCol, targetCol, actualCol, pctCol, statusCol);
        table.setItems(targetList);
        table.setPlaceholder(new Label("No targets set yet. Click 'Set / Edit Target' to begin."));
        tableCard.getChildren().addAll(tableTitle, table);

        root.getChildren().addAll(headerCard, gaugeRow, tableCard);
        refresh();
        return new ScrollPane(root) {{ setFitToWidth(true); setStyle("-fx-background-color: transparent;"); }};
    }

    private HBox statRow(String label, Label valueLabel) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label);
        l.getStyleClass().add("sub-label");
        HBox.setHgrow(l, Priority.ALWAYS);
        valueLabel.getStyleClass().addAll("stat-value", "text-accent");
        valueLabel.setStyle("-fx-font-size: 18px;");
        row.getChildren().addAll(l, valueLabel);
        return row;
    }

    private void refresh() {
        String month = LocalDate.now().toString().substring(0, 7);
        double target = targetDAO.getTargetForMonth(month);
        double actual = saleDAO.getTotalRevenueThisMonth();
        YearMonth ym = YearMonth.parse(month);
        int dayOfMonth = LocalDate.now().getDayOfMonth();
        int daysInMonth = ym.lengthOfMonth();
        double projected = dayOfMonth > 0 ? actual / dayOfMonth * daysInMonth : 0;

        statTarget.setText(target > 0 ? String.format("₹%,.0f", target) : "Not set");
        statActual.setText(String.format("₹%,.0f", actual));
        statProjected.setText(String.format("₹%,.0f", projected));

        double pct = target > 0 ? actual / target * 100 : 0;
        gauge.setProgress(target > 0 ? Math.min(pct / 100, 1.0) : 0);
        gaugePct.setText(target > 0 ? String.format("%.0f%%", Math.min(pct, 999)) : "—");
        if (target > 0) {
            gauge.setStyle("-fx-progress-color: " + (pct >= 100 ? "#22c55e" : pct >= 60 ? "#16c79a" : "#f59e0b") + ";");
            statStatus.setText(pct >= 100 ? "✓ Target achieved!" : "On track to " + (projected >= target ? "hit" : "miss") + " target");
            gaugeText.setText(String.format("₹%,.0f earned of ₹%,.0f target%n%s day%s of %s",
                    actual, target, dayOfMonth, dayOfMonth == 1 ? "" : "s", daysInMonth));
        } else {
            gauge.setStyle("-fx-progress-color: #64748b;");
            statStatus.setText("No target for this month");
            gaugeText.setText("Set a monthly target to see progress.");
        }

        targetList.clear();
        targetList.addAll(targetDAO.findAll());
    }

    private void showTargetDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Set Monthly Sales Target");

        VBox form = new VBox(14);
        form.setPadding(new Insets(20));
        form.getStyleClass().add("card");

        String currentMonth = LocalDate.now().toString().substring(0, 7);
        TextField monthField = new TextField(currentMonth);
        monthField.setPromptText("Month (yyyy-MM)");

        TextField targetField = new TextField();
        targetField.setPromptText("Target revenue (₹)");

        Button saveBtn = new Button("Save Target");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setMaxWidth(Double.MAX_VALUE);

        saveBtn.setOnAction(e -> {
            try {
                String month = monthField.getText().trim();
                double target = Double.parseDouble(targetField.getText().trim().replace(",", ""));
                if (!month.matches("\\d{4}-\\d{2}")) {
                    new Alert(Alert.AlertType.ERROR, "Invalid month format. Use yyyy-MM.").showAndWait();
                    return;
                }
                if (target <= 0) {
                    new Alert(Alert.AlertType.ERROR, "Target must be greater than zero.").showAndWait();
                    return;
                }
                targetDAO.upsert(month, target);
                dialog.close();
                refresh();
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.ERROR, "Target must be a valid number.").showAndWait();
            }
        });

        form.getChildren().addAll(
                new Label("Month"), monthField,
                new Label("Target Amount (₹)"), targetField,
                saveBtn);

        Scene scene = new Scene(form, 380, 240);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.show();
    }
}
