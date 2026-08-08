package com.shop.view;

import com.shop.dao.*;
import com.shop.model.Product;
import com.shop.model.Role;
import com.shop.model.User;
import com.shop.util.SessionManager;
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

        // Stat Cards Grid
        HBox statsGrid = new HBox(16);

        VBox revenueCard = createStatCard("💵", String.format("₹%.2f", saleDAO.getTotalRevenueToday()), "Today's Revenue", "text-accent");
        VBox salesCard = createStatCard("🛒", String.valueOf(saleDAO.getSalesCountToday()), "Transactions Today", "text-white");
        VBox productsCard = createStatCard("📦", String.valueOf(productDAO.count()), "Total Products", "text-white");
        VBox lowStockCard = createStatCard("⚠️", String.valueOf(productDAO.countLowStock()), "Low Stock Alerts", "text-warning");

        statsGrid.getChildren().addAll(revenueCard, salesCard, productsCard, lowStockCard);
        HBox.setHgrow(revenueCard, Priority.ALWAYS);
        HBox.setHgrow(salesCard, Priority.ALWAYS);
        HBox.setHgrow(productsCard, Priority.ALWAYS);
        HBox.setHgrow(lowStockCard, Priority.ALWAYS);

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

        qaButtons.getChildren().addAll(posBtn, addProductBtn, addCustomerBtn);
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

        alertsCard.getChildren().addAll(alertsTitle, alertTable);

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
