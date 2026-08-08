package com.shop.view;

import com.shop.dao.ExpenseDAO;
import com.shop.dao.SaleDAO;
import com.shop.model.Expense;
import com.shop.util.ReportExporter;
import com.shop.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExpensesView {
    private final ExpenseDAO expenseDAO = new ExpenseDAO();
    private final SaleDAO saleDAO = new SaleDAO();
    private final ObservableList<Expense> expenseList = FXCollections.observableArrayList();
    private final TableView<Expense> table = new TableView<>();
    private final Label todayExpenseLabel = new Label();
    private final Label todayProfitLabel = new Label();
    private final Label monthExpenseLabel = new Label();
    private final Label monthProfitLabel = new Label();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        Label title = new Label("💰 Expenses & Profit");
        title.getStyleClass().add("section-title");

        HBox summaryBar = new HBox(14);
        VBox todayExpenseCard = createSummaryCard("💸", todayExpenseLabel, "Today's Expenses");
        VBox todayProfitCard = createSummaryCard("📈", todayProfitLabel, "Today's Profit");
        VBox monthExpenseCard = createSummaryCard("🗓️", monthExpenseLabel, "This Month's Expenses");
        VBox monthProfitCard = createSummaryCard("🏆", monthProfitLabel, "This Month's Profit");
        for (VBox card : List.of(todayExpenseCard, todayProfitCard, monthExpenseCard, monthProfitCard)) {
            HBox.setHgrow(card, Priority.ALWAYS);
        }
        summaryBar.getChildren().addAll(todayExpenseCard, todayProfitCard, monthExpenseCard, monthProfitCard);

        HBox addRow = new HBox(12);
        addRow.setAlignment(Pos.CENTER_LEFT);

        TextField descField = new TextField();
        descField.setPromptText("Description (e.g. Electricity bill)");
        descField.setPrefWidth(240);

        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.setEditable(true);
        categoryCombo.setPromptText("Category");
        categoryCombo.getItems().addAll("Rent", "Electricity", "Water", "Staff", "Restock", "Transport",
                "Supplies", "Maintenance", "Marketing", "Tax", "Other");
        categoryCombo.setPrefWidth(150);

        TextField amountField = new TextField();
        amountField.setPromptText("Amount (₹)");
        amountField.setPrefWidth(120);

        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setPrefWidth(140);

        Button addBtn = new Button("➕  Add Expense");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> addExpense(descField, categoryCombo, amountField, datePicker));

        addRow.getChildren().addAll(descField, categoryCombo, amountField, datePicker, addBtn);

        HBox exportRow = new HBox(10);
        exportRow.setAlignment(Pos.CENTER_LEFT);
        Button csvBtn = new Button("⬇️ Export CSV");
        csvBtn.getStyleClass().add("btn-secondary");
        csvBtn.setOnAction(e -> exportCsv());
        Button excelBtn = new Button("⬇️ Export Excel");
        excelBtn.getStyleClass().add("btn-primary");
        excelBtn.setOnAction(e -> exportExcel());
        Region exportSpacer = new Region();
        HBox.setHgrow(exportSpacer, Priority.ALWAYS);
        exportRow.getChildren().addAll(csvBtn, excelBtn, exportSpacer);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Expense, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(p -> new SimpleStringProperty(
                p.getValue().getExpenseDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))));
        dateCol.setMaxWidth(110);

        TableColumn<Expense, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<Expense, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        catCol.setMaxWidth(130);

        TableColumn<Expense, String> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().getAmount())));

        TableColumn<Expense, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setMaxWidth(70);
        actionCol.setCellFactory(col -> new TableCell<Expense, Void>() {
            private final Button delBtn = new Button("🗑️");
            {
                delBtn.getStyleClass().addAll("btn-danger", "btn-small");
                delBtn.setOnAction(e -> {
                    Expense ex = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "Delete expense '" + ex.getDescription() + "' (₹" + String.format("%.2f", ex.getAmount()) + ")?",
                            ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.YES) {
                            expenseDAO.delete(ex.getId());
                            loadExpenses();
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : delBtn);
            }
        });

        table.getColumns().addAll(dateCol, descCol, catCol, amountCol, actionCol);
        table.setItems(expenseList);
        table.setPlaceholder(new Label("No expenses recorded yet. Add one above."));

        root.getChildren().addAll(title, summaryBar, addRow, exportRow, table);
        loadExpenses();
        return root;
    }

    private VBox createSummaryCard(String icon, Label valueLabel, String label) {
        VBox card = new VBox(6);
        card.getStyleClass().add("stat-card");
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("stat-icon");
        valueLabel.getStyleClass().addAll("stat-value", "text-white");
        Label titleLabel = new Label(label);
        titleLabel.getStyleClass().add("stat-label");
        card.getChildren().addAll(iconLabel, valueLabel, titleLabel);
        return card;
    }

    private void addExpense(TextField desc, ComboBox<String> category, TextField amount, DatePicker date) {
        try {
            String description = desc.getText().trim();
            if (description.isEmpty()) {
                showAlert("Description cannot be empty.");
                return;
            }
            double amt = Double.parseDouble(amount.getText().trim());
            if (amt <= 0) {
                showAlert("Amount must be greater than zero.");
                return;
            }

            Expense e = new Expense();
            e.setDescription(description);
            String cat = category.getValue();
            e.setCategory(cat == null || cat.isBlank() ? "General" : cat.trim());
            e.setAmount(amt);
            e.setExpenseDate(date.getValue() != null ? date.getValue() : LocalDate.now());
            e.setUserId(SessionManager.getInstance().getCurrentUser() != null
                    ? SessionManager.getInstance().getCurrentUser().getId() : 0);

            if (expenseDAO.insert(e)) {
                desc.clear();
                amount.clear();
                loadExpenses();
            } else {
                showAlert("Could not save the expense. Please try again.");
            }
        } catch (NumberFormatException ex) {
            showAlert("Amount must be a valid number.");
        }
    }

    private void loadExpenses() {
        expenseList.clear();
        expenseList.addAll(expenseDAO.findAll());

        double todayExpense = expenseDAO.getTotalToday();
        double todayRevenue = saleDAO.getTotalRevenueToday();
        double monthExpense = expenseDAO.getTotalThisMonth();
        double monthRevenue = saleDAO.getTotalRevenueThisMonth();

        todayExpenseLabel.setText(String.format("₹%.2f", todayExpense));
        todayProfitLabel.setText(String.format("₹%.2f", todayRevenue - todayExpense));
        todayProfitLabel.getStyleClass().remove("text-danger");
        todayProfitLabel.getStyleClass().remove("text-accent");
        todayProfitLabel.getStyleClass().add(todayRevenue - todayExpense >= 0 ? "text-accent" : "text-danger");

        monthExpenseLabel.setText(String.format("₹%.2f", monthExpense));
        monthProfitLabel.setText(String.format("₹%.2f", monthRevenue - monthExpense));
        monthProfitLabel.getStyleClass().remove("text-danger");
        monthProfitLabel.getStyleClass().remove("text-accent");
        monthProfitLabel.getStyleClass().add(monthRevenue - monthExpense >= 0 ? "text-accent" : "text-danger");
    }

    private void exportCsv() {
        File file = chooseFile("Export Expenses CSV", "expense-report", ".csv");
        if (file == null) return;
        try {
            ReportExporter.exportExpensesCsv(expenseList, file);
            showAlert("Expenses report saved to:\n" + file.getAbsolutePath());
        } catch (IOException ex) {
            showAlert("Could not write CSV file:\n" + ex.getMessage());
        }
    }

    private void exportExcel() {
        File file = chooseFile("Export Expenses Excel", "expense-report", ".xlsx");
        if (file == null) return;
        try {
            ReportExporter.exportExpensesExcel(expenseList, file);
            showAlert("Expenses report saved to:\n" + file.getAbsolutePath());
        } catch (IOException ex) {
            showAlert("Could not write Excel file:\n" + ex.getMessage());
        }
    }

    private File chooseFile(String title, String baseName, String ext) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        chooser.setInitialFileName(baseName + "-" + stamp + ext);
        FileChooser.ExtensionFilter filter = ext.equals(".csv")
                ? new FileChooser.ExtensionFilter("CSV File (*.csv)", "*.csv")
                : new FileChooser.ExtensionFilter("Excel File (*.xlsx)", "*.xlsx");
        chooser.getExtensionFilters().add(filter);
        return chooser.showSaveDialog(null);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
