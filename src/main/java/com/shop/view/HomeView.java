package com.shop.view;

import com.shop.dao.*;
import com.shop.model.Product;
import com.shop.model.Role;
import com.shop.model.User;
import com.shop.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.List;

public class HomeView {
    private final DashboardView parent;
    private final ProductDAO productDAO = new ProductDAO();
    private final SaleDAO saleDAO = new SaleDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final UserDAO userDAO = new UserDAO();
    private final ExpenseDAO expenseDAO = new ExpenseDAO();
    private final User currentUser = SessionManager.getInstance().getCurrentUser();

    public HomeView(DashboardView parent) {
        this.parent = parent;
    }

    public Node getView() {
        VBox root = new VBox(24);
        root.setPadding(new Insets(10));

        // Welcome Header
        String userName = currentUser != null ? currentUser.getFullName() : "User";
        Label welcomeLabel = new Label("Welcome back, " + userName + " 👋");
        welcomeLabel.getStyleClass().add("section-title");
        Label subtitleLabel = new Label("Here is what's happening with your shop today.");
        subtitleLabel.getStyleClass().add("sub-label");
        VBox topBox = new VBox(4, welcomeLabel, subtitleLabel);

        // Stat Cards Grid (wraps as needed)
        FlowPane statsGrid = new FlowPane(16, 16);

        double todayRevenue = saleDAO.getTotalRevenueToday();
        double todayExpense = expenseDAO.getTotalToday();
        double todayProfit = todayRevenue - todayExpense;

        VBox revenueCard = createStatCard("💵", String.format("₹%.2f", todayRevenue), "Today's Revenue", "text-accent");
        VBox profitCard = createStatCard("📈", String.format("₹%.2f", todayProfit), "Today's Profit", todayProfit >= 0 ? "text-accent" : "text-danger");
        VBox salesCard = createStatCard("🛒", String.valueOf(saleDAO.getSalesCountToday()), "Transactions Today", "text-white");
        VBox productsCard = createStatCard("📦", String.valueOf(productDAO.count()), "Total Products", "text-white");
        VBox lowStockCard = createStatCard("⚠️", String.valueOf(productDAO.countLowStock()), "Low Stock Alerts", "text-warning");
        int expiringCount = productDAO.findExpiring(30).size() + productDAO.findExpired().size();
        VBox expiryCard = createStatCard("⏳", String.valueOf(expiringCount), "Expiring / Expired", "text-danger");

        for (VBox card : List.of(revenueCard, profitCard, salesCard, productsCard, lowStockCard, expiryCard)) {
            card.setPrefWidth(175);
            statsGrid.getChildren().add(card);
        }

        // Quick Actions Bar
        VBox quickActionsCard = new VBox(14);
        quickActionsCard.getStyleClass().add("card");

        Label qaTitle = new Label("⚡ Quick Actions");
        qaTitle.getStyleClass().add("section-title");

        HBox qaButtons = new HBox(12);
        Button posBtn = new Button("💳  Open POS / New Sale");
        posBtn.getStyleClass().add("btn-primary");
        posBtn.setOnAction(e -> parent.loadView("Point of Sale", new POSView().getView()));

        Button addProductBtn = new Button("➕  Add Product");
        addProductBtn.getStyleClass().add("btn-secondary");
        addProductBtn.setOnAction(e -> parent.loadView("Inventory Management", new InventoryView().getView()));

        Button addCustomerBtn = new Button("👤  Add Customer");
        addCustomerBtn.getStyleClass().add("btn-secondary");
        addCustomerBtn.setOnAction(e -> parent.loadView("Customer Management", new CustomerView().getView()));

        Button addExpenseBtn = new Button("💰  Add Expense");
        addExpenseBtn.getStyleClass().add("btn-secondary");
        addExpenseBtn.setOnAction(e -> parent.loadView("Expense Management", new ExpensesView().getView()));

        qaButtons.getChildren().addAll(posBtn, addProductBtn, addCustomerBtn, addExpenseBtn);
        quickActionsCard.getChildren().addAll(qaTitle, qaButtons);

        // Low Stock Table Section
        VBox alertsCard = new VBox(14);
        alertsCard.getStyleClass().add("card");
        VBox.setVgrow(alertsCard, Priority.ALWAYS);

        Label alertsTitle = new Label("⚠️ Stock Attention Required");
        alertsTitle.getStyleClass().add("section-title");

        TableView<Product> alertTable = new TableView<>();
        alertTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        alertTable.setPrefHeight(220);

        TableColumn<Product, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Product, String> barcodeCol = new TableColumn<>("Barcode");
        barcodeCol.setCellValueFactory(new PropertyValueFactory<>("barcode"));

        TableColumn<Product, Integer> qtyCol = new TableColumn<>("In Stock");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Product, Integer> minQtyCol = new TableColumn<>("Min Level");
        minQtyCol.setCellValueFactory(new PropertyValueFactory<>("minStockLevel"));

        TableColumn<Product, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("stockStatus"));
        statusCol.setCellFactory(col -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    if ("Out of Stock".equals(item)) {
                        badge.getStyleClass().add("badge-inactive");
                    } else {
                        badge.getStyleClass().add("badge-warning");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        alertTable.getColumns().addAll(nameCol, barcodeCol, qtyCol, minQtyCol, statusCol);

        List<Product> lowStockProducts = productDAO.findLowStock();
        alertTable.getItems().addAll(lowStockProducts);
        alertTable.setPlaceholder(new Label("No low stock items. All inventory levels are healthy! 👍"));

        // Expiring / Expired Table
        Label expiryTitle = new Label("⏳ Expiring Soon / Expired");
        expiryTitle.getStyleClass().add("section-title");

        TableView<Product> expiryTable = new TableView<>();
        expiryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        expiryTable.setPrefHeight(220);

        TableColumn<Product, String> expNameCol = new TableColumn<>("Product Name");
        expNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Product, String> expQtyCol = new TableColumn<>("In Stock");
        expQtyCol.setCellValueFactory(p -> new SimpleStringProperty(String.valueOf(p.getValue().getQuantity())));

        TableColumn<Product, String> expDateCol = new TableColumn<>("Expiry Date");
        expDateCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getExpiryDateLabel()));

        TableColumn<Product, String> expStatusCol = new TableColumn<>("Status");
        expStatusCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().hasExpired() ? "Expired" : "Expiring Soon"));
        expStatusCol.setCellFactory(col -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("Expired".equals(item) ? "badge-inactive" : "badge-warning");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        expiryTable.getColumns().addAll(expNameCol, expQtyCol, expDateCol, expStatusCol);

        List<Product> expiringProducts = new java.util.ArrayList<>(productDAO.findExpired());
        expiringProducts.addAll(productDAO.findExpiring(30));
        expiryTable.getItems().addAll(expiringProducts);
        expiryTable.setPlaceholder(new Label("No products expiring. Nothing to worry about! 🎉"));

        HBox alertsTables = new HBox(14);
        HBox.setHgrow(alertTable, Priority.ALWAYS);
        HBox.setHgrow(expiryTable, Priority.ALWAYS);
        VBox lowStockPane = new VBox(8, new Label("📦 Low Stock"), alertTable);
        VBox expiryPane = new VBox(8, new Label("📆 Expiry"), expiryTable);
        alertsTables.getChildren().addAll(lowStockPane, expiryPane);

        alertsCard.getChildren().addAll(alertsTitle, alertsTables);

        root.getChildren().addAll(topBox, statsGrid, quickActionsCard, alertsCard);
        return new ScrollPane(root) {{ setFitToWidth(true); setStyle("-fx-background-color: transparent;"); }};
    }

    private VBox createStatCard(String icon, String value, String label, String valueStyleClass) {
        VBox card = new VBox(6);
        card.getStyleClass().add("stat-card");

        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("stat-icon");

        Label valLabel = new Label(value);
        valLabel.getStyleClass().addAll("stat-value", valueStyleClass);

        Label titleLabel = new Label(label);
        titleLabel.getStyleClass().add("stat-label");

        card.getChildren().addAll(iconLabel, valLabel, titleLabel);
        return card;
    }
}
