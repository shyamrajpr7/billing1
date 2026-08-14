package com.shop.view;

import com.shop.ai.AssistantPanel;
import com.shop.ai.CommandAssistant;
import com.shop.model.Role;
import com.shop.model.User;
import com.shop.util.SessionManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DashboardView {
    private final Stage stage;
    private final BorderPane rootLayout = new BorderPane();
    private final StackPane contentArea = new StackPane();
    private final Label headerTitle = new Label("Dashboard");
    private final List<Button> navButtons = new ArrayList<>();
    private final User currentUser = SessionManager.getInstance().getCurrentUser();
    private Supplier<Node> currentViewSupplier;
    private String currentViewTitle = "Dashboard";

    public DashboardView(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        VBox sidebar = buildSidebar();
        HBox header = buildHeader();

        contentArea.getStyleClass().add("content-area");
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        VBox centerLayout = new VBox(header, contentArea);
        HBox.setHgrow(centerLayout, Priority.ALWAYS);

        rootLayout.setLeft(sidebar);
        rootLayout.setCenter(centerLayout);

        // AI Assistant floating action button + chat panel overlay
        AssistantPanel assistantPanel = new AssistantPanel();
        CommandAssistant.getInstance().setDashboard(this);
        StackPane.setAlignment(assistantPanel.getView(), Pos.BOTTOM_RIGHT);
        StackPane root = new StackPane(rootLayout, assistantPanel.getView());
        root.setPickOnBounds(false);

        // Load default view
        loadView("Dashboard", () -> new HomeView(this).getView());

        // Background desktop notifications for low stock & expiry
        com.shop.util.StockNotifier.start();

        Scene scene = new Scene(root, 1280, 800);
        applyTheme(scene, currentUser != null && currentUser.isDarkTheme());
        GlobalSearch.install(scene, this);
        stage.setScene(scene);
        stage.setTitle("Shop Management System");
        stage.show();
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");

        // Header
        Label logoLabel = new Label("🛍️ SHOP OS");
        logoLabel.getStyleClass().add("sidebar-title");
        Label versionLabel = new Label("v1.0 • Professional Retail");
        versionLabel.getStyleClass().add("sidebar-version");
        VBox headerBox = new VBox(2, logoLabel, versionLabel);
        headerBox.getStyleClass().add("sidebar-header");

        VBox navBox = new VBox(4);
        VBox.setVgrow(navBox, Priority.ALWAYS);

        // Menu items based on role
        addNavButton(navBox, "📊  Dashboard", () -> loadView("Dashboard", () -> new HomeView(this).getView()), true);
            addNavButton(navBox, "💳  Point of Sale (POS)", () -> loadView("Point of Sale", () -> new POSView().getView()), false);

        if (currentUser != null && (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.MANAGER)) {
            Label mgmtLabel = new Label("MANAGEMENT");
            mgmtLabel.getStyleClass().add("sidebar-section");
            navBox.getChildren().add(mgmtLabel);

            addNavButton(navBox, "📦  Inventory / Stock", () -> loadView("Inventory Management", () -> new InventoryView().getView()), false);
            addNavButton(navBox, "🏷️  Brands", () -> loadView("Brand Management", () -> new BrandsView().getView()), false);
            addNavButton(navBox, "↩️  Returns & Refunds", () -> loadView("Returns & Refunds", () -> new ReturnsView().getView()), false);
            addNavButton(navBox, "🎁  Gift Cards", () -> loadView("Gift Cards", () -> new GiftCardsView().getView()), false);
            addNavButton(navBox, "🔁  Reorder Suggestions", () -> loadView("Reorder Suggestions", () -> new ReorderSuggestionsView().getView()), false);
            addNavButton(navBox, "📋  Stocktake", () -> loadView("Stocktake", () -> new StocktakeView().getView()), false);
            addNavButton(navBox, "💰  Expenses & Profit", () -> loadView("Expense Management", () -> new ExpensesView().getView()), false);
            addNavButton(navBox, "👥  Customers", () -> loadView("Customer Management", () -> new CustomerView().getView()), false);
            addNavButton(navBox, "🎫  Service Tickets", () -> loadView("Service Tickets", () -> new ServiceTicketsView().getView()), false);
            addNavButton(navBox, "💬  Customer Feedback", () -> loadView("Customer Feedback", () -> new FeedbackView().getView()), false);
            addNavButton(navBox, "💳  Credit & Dues", () -> loadView("Credit & Dues", () -> new CreditView().getView()), false);
            addNavButton(navBox, "⭐  Memberships & Loyalty", () -> loadView("Membership & Loyalty", () -> new MembershipsView().getView()), false);
            addNavButton(navBox, "🏷️  Discounts & Coupons", () -> loadView("Discount Management", () -> new DiscountView().getView()), false);
            addNavButton(navBox, "🎯  Promotions", () -> loadView("Promotions", () -> new PromotionsView().getView()), false);
            addNavButton(navBox, "🚚  Suppliers", () -> loadView("Supplier Management", () -> new SupplierView().getView()), false);
            addNavButton(navBox, "🤝  Supplier Performance", () -> loadView("Supplier Performance", () -> new SupplierPerformanceView().getView()), false);
            addNavButton(navBox, "💸  Supplier Payments", () -> loadView("Supplier Payments", () -> new SupplierPaymentsView().getView()), false);
            addNavButton(navBox, "📋  Purchase Orders", () -> loadView("Purchase Orders", () -> new PurchaseOrdersView().getView()), false);
            addNavButton(navBox, "📝  Pre-Orders", () -> loadView("Pre-Orders", () -> new PreOrdersView().getView()), false);
            addNavButton(navBox, "📄  Quotations", () -> loadView("Quotations", () -> new QuotationsView().getView()), false);
            addNavButton(navBox, "🛡️  Warranties", () -> loadView("Warranty Management", () -> new WarrantiesView().getView()), false);
            addNavButton(navBox, "🛒  Layaway Plans", () -> loadView("Layaway Plans", () -> new LayawayView().getView()), false);
            addNavButton(navBox, "🚚  Deliveries", () -> loadView("Delivery Management", () -> new DeliveriesView().getView()), false);
        }

        if (currentUser != null && currentUser.getRole() == Role.ADMIN) {
            Label adminLabel = new Label("ADMINISTRATION");
            adminLabel.getStyleClass().add("sidebar-section");
            navBox.getChildren().add(adminLabel);

            addNavButton(navBox, "📈  Sales & Analytics", () -> loadView("Sales & Reports", () -> new ReportsView().getView()), false);
            addNavButton(navBox, "🏦  Shift & Cash Drawer", () -> loadView("Shift & Cash Drawer", () -> new ShiftView().getView()), false);
            addNavButton(navBox, "🏆  Top Sellers", () -> loadView("Top Sellers", () -> new TopSellersView().getView()), false);
            addNavButton(navBox, "📊  Charts & Trends", () -> loadView("Sales Analytics", () -> new AnalyticsView().getView()), false);
            addNavButton(navBox, "🔮  Sales Forecast", () -> loadView("Sales Forecast", () -> new SalesForecastView().getView()), false);
            addNavButton(navBox, "🎯  Sales Targets", () -> loadView("Sales Targets", () -> new SalesTargetView().getView()), false);
            addNavButton(navBox, "👔  Employee Directory", () -> loadView("Employee Management", () -> new EmployeeView().getView()), false);
            addNavButton(navBox, "⏱️  Staff Attendance", () -> loadView("Staff Attendance", () -> new AttendanceView().getView()), false);
            addNavButton(navBox, "📜  Activity Log", () -> loadView("Activity Log", () -> new ActivityLogView().getView()), false);
            addNavButton(navBox, "📢  Announcements", () -> loadView("Announcements", () -> new AnnouncementsView().getView()), false);
            addNavButton(navBox, "🏷️  Price Change Audit", () -> loadView("Price Change Audit", () -> new PriceHistoryView().getView()), false);
            addNavButton(navBox, "🛡️  Backup & Restore", () -> loadView("Backup & Restore", () -> new BackupView().getView()), false);
            addNavButton(navBox, "⚙️  Store Settings", () -> loadView("Store Settings", () -> new SettingsView().getView()), false);
            addNavButton(navBox, "⏰  Reminders & To-Dos", () -> loadView("Reminders & To-Dos", () -> new RemindersView().getView()), false);
            addNavButton(navBox, "📅  Store Calendar", () -> loadView("Store Calendar", () -> new CalendarView().getView()), false);
        }

        // User profile panel at bottom
        VBox userPanel = buildUserPanel();

        ScrollPane navScroll = new ScrollPane(navBox);
        navScroll.setFitToWidth(true);
        navScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        navScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(navScroll, Priority.ALWAYS);

        sidebar.getChildren().addAll(headerBox, navScroll, userPanel);
        return sidebar;
    }

    private void addNavButton(VBox container, String text, Runnable action, boolean isActive) {
        Button btn = new Button(text);
        btn.getStyleClass().add("sidebar-btn");
        if (isActive) btn.getStyleClass().add("sidebar-btn-active");

        btn.setOnAction(e -> {
            navButtons.forEach(b -> b.getStyleClass().remove("sidebar-btn-active"));
            btn.getStyleClass().add("sidebar-btn-active");
            action.run();
        });

        navButtons.add(btn);
        container.getChildren().add(btn);
    }

    private VBox buildUserPanel() {
        VBox userPanel = new VBox(4);
        userPanel.getStyleClass().add("sidebar-user-panel");

        String name = currentUser != null ? currentUser.getFullName() : "Guest";
        String role = currentUser != null ? currentUser.getRoleDisplay() : "";

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("sidebar-user-name");

        Label roleLabel = new Label("Role: " + role);
        roleLabel.getStyleClass().add("sidebar-user-role");

        Button logoutBtn = new Button("🔒  Sign Out");
        logoutBtn.getStyleClass().addAll("btn-secondary", "btn-small");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> {
            com.shop.util.StockNotifier.stop();
            SessionManager.getInstance().logout();
            new LoginView(stage).show();
        });

        userPanel.getChildren().addAll(nameLabel, roleLabel, logoutBtn);
        return userPanel;
    }

    private HBox buildHeader() {
        HBox header = new HBox();
        header.getStyleClass().add("header-bar");
        header.setAlignment(Pos.CENTER_LEFT);

        headerTitle.getStyleClass().add("header-title");
        header.getChildren().add(headerTitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().add(spacer);

        DateTimeFormatter clockFormat = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  •  hh:mm a");
        Label clockLabel = new Label(LocalDateTime.now().format(clockFormat));
        clockLabel.getStyleClass().add("sub-label");
        HBox.setMargin(clockLabel, new javafx.geometry.Insets(0, 14, 0, 0));
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e ->
                clockLabel.setText(LocalDateTime.now().format(clockFormat))));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
        header.getChildren().add(clockLabel);

        Button searchBtn = new Button("🔍  Search  (⌘K)");
        searchBtn.getStyleClass().add("btn-secondary");
        searchBtn.setOnAction(e -> GlobalSearch.open(this));
        header.getChildren().add(searchBtn);

        boolean dark = currentUser != null && currentUser.isDarkTheme();
        Button themeBtn = new Button(dark ? "🌙  Dark" : "☀️  Light");
        themeBtn.getStyleClass().add("btn-secondary");
        themeBtn.setOnAction(e -> {
            boolean newDark = !(currentUser != null && currentUser.isDarkTheme());
            if (currentUser != null) {
                currentUser.setDarkTheme(newDark);
                new com.shop.dao.UserDAO().updateTheme(currentUser.getId(), newDark);
            }
            themeBtn.setText(newDark ? "🌙  Dark" : "☀️  Light");
            applyTheme(stage.getScene(), newDark);
        });

        header.getChildren().add(themeBtn);
        return header;
    }

    private void applyTheme(Scene scene, boolean dark) {
        scene.getStylesheets().remove(getClass().getResource("/css/dark.css").toExternalForm());
        scene.getStylesheets().remove(getClass().getResource("/css/style.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource(dark ? "/css/dark.css" : "/css/style.css").toExternalForm());
    }

    public void loadView(String title, Supplier<Node> viewSupplier) {
        this.currentViewTitle = title;
        this.currentViewSupplier = viewSupplier;
        headerTitle.setText(title);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(viewSupplier.get());
    }

    public void loadView(String title, Node view) {
        loadView(title, () -> view);
    }

    public void reloadCurrentView() {
        if (currentViewSupplier != null) {
            loadView(currentViewTitle, currentViewSupplier);
        }
    }
}
