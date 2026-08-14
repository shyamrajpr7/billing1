package com.shop.view;

import com.shop.dao.*;
import com.shop.model.*;
import com.shop.util.SessionManager;
import com.shop.util.WhatsAppSender;
import javafx.application.Platform;
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

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class POSView {
    private final ProductDAO productDAO = new ProductDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final DiscountDAO discountDAO = new DiscountDAO();
    private final SaleDAO saleDAO = new SaleDAO();
    private final User currentUser = SessionManager.getInstance().getCurrentUser();

    private final ObservableList<SaleItem> cartItems = FXCollections.observableArrayList();
    private final ObservableList<Product> availableProducts = FXCollections.observableArrayList();
    private final FlowPane quickPad = new FlowPane(8, 8);

    private final Label subtotalLabel = new Label("₹0.00");
    private final Label discountLabel = new Label("-₹0.00");
    private final Label loyaltyDiscountLabel = new Label("-₹0.00");
    private final Label taxLabel = new Label("₹0.00");
    private final Label grandTotalLabel = new Label("₹0.00");
    private final Label giftCardLabel = new Label("-₹0.00");

    private Discount appliedDiscount = null;
    private Customer selectedCustomer = null;
    private int pointsRedeemed = 0;
    private final ComboBox<Customer> customerCombo = new ComboBox<>();
    private final ComboBox<String> paymentMethodCombo = new ComboBox<>();
    private final Label loyaltyBalanceLabel = new Label("🅿️  Loyalty Points: 0");
    private final TextField pointsField = new TextField();

    private final TextField phoneField = new TextField();

    private GiftCard appliedGiftCard = null;
    private final TextField giftCardField = new TextField();
    private final Label giftCardStatusLabel = new Label("Enter a gift card number to apply.");

    private final Label scanStatus = new Label("Ready to scan. Use a USB scanner or type a barcode and press Enter.");
    private Timer scanDebounce = new Timer(true);

    private static final double TAX_RATE = 0.05; // 5% GST/Tax

    public Node getView() {
        HBox mainLayout = new HBox(20);
        mainLayout.setPadding(new Insets(5));

        // Left Panel: Product Catalog & Barcode Scanner (60% width)
        VBox catalogPanel = buildCatalogPanel();
        HBox.setHgrow(catalogPanel, Priority.ALWAYS);

        // Right Panel: Shopping Cart & Checkout (40% width)
        VBox cartPanel = buildCartPanel();
        cartPanel.setPrefWidth(480);
        cartPanel.setMinWidth(420);

        mainLayout.getChildren().addAll(catalogPanel, cartPanel);
        loadProducts("");
        loadCustomers();
        refreshQuickPad();

        return mainLayout;
    }

    private VBox buildCatalogPanel() {
        VBox panel = new VBox(16);

        // Top Search & Barcode Bar
        HBox searchBar = new HBox(12);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search product by name...");
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            loadProducts(newVal.trim());
            String code = newVal.trim();
            if (!code.isEmpty()) {
                scanDebounce.cancel();
                scanDebounce = new Timer(true);
                scanDebounce.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> {
                            Product foundByBarcode = productDAO.findByBarcode(code);
                            if (foundByBarcode != null && foundByBarcode.getQuantity() > 0) {
                                addToCart(foundByBarcode);
                                searchField.clear();
                                setScanStatus("✓ " + foundByBarcode.getName() + " added to cart.");
                            }
                        });
                    }
                }, 300);
            }
        });

        // Dedicated barcode scan row (USB scanner = keyboard wedge, or manual entry)
        Label scanLabel = new Label("📷 Scan Barcode:");
        scanLabel.getStyleClass().add("form-label");

        TextField scanField = new TextField();
        scanField.setPromptText("Type/scan barcode, press Enter (or barcode*qty)");
        HBox.setHgrow(scanField, Priority.ALWAYS);

        Button scanBtn = new Button("Scan");
        scanBtn.getStyleClass().add("btn-primary");
        scanBtn.setOnAction(e -> processScan(scanField.getText()));

        scanField.setOnAction(e -> processScan(scanField.getText()));
        scanField.requestFocus();

        scanStatus.getStyleClass().add("sub-label");

        VBox scanBox = new VBox(4);
        HBox scanRow = new HBox(8);
        scanRow.setAlignment(Pos.CENTER_LEFT);
        scanRow.getChildren().addAll(scanLabel, scanField, scanBtn);
        scanBox.getChildren().addAll(scanRow, scanStatus);

        searchBar.getChildren().add(searchField);

        Button sellGiftCardBtn = new Button("🎁  Sell Gift Card");
        sellGiftCardBtn.getStyleClass().add("btn-secondary");
        sellGiftCardBtn.setOnAction(e -> sellGiftCardDialog());
        searchBar.getChildren().add(sellGiftCardBtn);

        // Product Catalog Table
        TableView<Product> productTable = new TableView<>();
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(productTable, Priority.ALWAYS);

        TableColumn<Product, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Product, String> barcodeCol = new TableColumn<>("Barcode");
        barcodeCol.setCellValueFactory(new PropertyValueFactory<>("barcode"));

        TableColumn<Product, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<Product, String> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().getSellPrice())));

        TableColumn<Product, Integer> qtyCol = new TableColumn<>("Stock");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Product, Void> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(col -> new TableCell<Product, Void>() {
            private final Button addBtn = new Button("➕ Add");
            {
                addBtn.getStyleClass().addAll("btn-primary", "btn-small");
                addBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    if (p.getQuantity() > 0) {
                        addToCart(p);
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Out of Stock", "This product is currently out of stock!");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Product p = getTableView().getItems().get(getIndex());
                    addBtn.setDisable(p.getQuantity() <= 0);
                    setGraphic(addBtn);
                }
            }
        });

        productTable.getColumns().addAll(nameCol, barcodeCol, categoryCol, priceCol, qtyCol, actionCol);
        productTable.setItems(availableProducts);
        productTable.setPlaceholder(new Label("No products found matching your search."));

        VBox quickPadCard = new VBox(6);
        quickPadCard.getStyleClass().add("card");
        quickPadCard.setPadding(new Insets(10));
        Label quickTitle = new Label("⚡ Frequently Bought");
        quickTitle.getStyleClass().add("form-label");
        quickPad.setVgap(8);
        quickPad.setHgap(8);
        quickPadCard.getChildren().addAll(quickTitle, quickPad);

        panel.getChildren().addAll(scanBox, searchBar, quickPadCard, productTable);
        return panel;
    }

    private void refreshQuickPad() {
        quickPad.getChildren().clear();
        List<BestSeller> top = saleDAO.getTopSellers(30, 8);
        for (BestSeller bs : top) {
            Product p = productDAO.findById(bs.getProductId());
            if (p == null) continue;
            Button btn = new Button(p.getName() + "\n₹" + String.format("%.0f", p.getSellPrice()));
            btn.setPrefWidth(118);
            btn.setPrefHeight(52);
            btn.setWrapText(true);
            btn.getStyleClass().add(p.getQuantity() > 0 ? "btn-primary" : "btn-secondary");
            btn.setDisable(p.getQuantity() <= 0);
            btn.setTooltip(new Tooltip(p.getName() + " — " + (p.getQuantity() > 0 ? "In stock: " + p.getQuantity() : "Out of stock")));
            btn.setOnAction(e -> addToCart(p));
            quickPad.getChildren().add(btn);
        }
    }

    private VBox buildCartPanel() {
        VBox panel = new VBox();
        panel.getStyleClass().add("card");

        VBox content = new VBox(14);

        // Cart Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("🛒 Current Cart");
        title.getStyleClass().add("section-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button clearBtn = new Button("Clear");
        clearBtn.getStyleClass().addAll("btn-secondary", "btn-small");
        clearBtn.setOnAction(e -> {
            if (cartItems.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Cart Empty", "Your cart is already empty.");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Are you sure you want to clear the cart and remove all " + cartItems.size() + " item(s)?",
                    ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Clear Cart");
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(r -> {
                if (r == ButtonType.YES) clearCart();
            });
        });
        header.getChildren().addAll(title, spacer, clearBtn);

        // Customer Selection
        VBox customerBox = new VBox(6);
        Label custLabel = new Label("Customer");
        custLabel.getStyleClass().add("form-label");

        customerCombo.setMaxWidth(Double.MAX_VALUE);
        customerCombo.setOnAction(e -> {
            selectedCustomer = customerCombo.getValue();
            pointsRedeemed = 0;
            pointsField.clear();
            phoneField.setText(selectedCustomer != null && selectedCustomer.getPhone() != null ? selectedCustomer.getPhone() : "");
            updateLoyaltyBalance();
            recalculateTotals();
        });

        customerBox.getChildren().addAll(custLabel, customerCombo);

        // WhatsApp e-Bill Phone
        VBox phoneBox = new VBox(6);
        Label phoneLabel = new Label("📲 Phone for WhatsApp e-Bill");
        phoneLabel.getStyleClass().add("form-label");
        phoneField.setPromptText("e.g. 9876543210");
        phoneField.setMaxWidth(Double.MAX_VALUE);
        phoneBox.getChildren().addAll(phoneLabel, phoneField);

        // Loyalty points redemption
        VBox loyaltyBox = new VBox(6);
        loyaltyBox.getStyleClass().add("card");
        loyaltyBox.setPadding(new Insets(10));

        loyaltyBalanceLabel.getStyleClass().add("sub-label");
        loyaltyBalanceLabel.setWrapText(true);

        pointsField.setPromptText("Points to redeem (1 pt = ₹1)");
        pointsField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(pointsField, Priority.ALWAYS);

        Button maxBtn = new Button("Max");
        maxBtn.getStyleClass().addAll("btn-secondary", "btn-small");
        maxBtn.setTooltip(new Tooltip("Redeem as many points as possible for this bill"));

        Button applyBtn = new Button("Apply");
        applyBtn.getStyleClass().addAll("btn-primary", "btn-small");

        maxBtn.setOnAction(e -> applyPoints(true));
        applyBtn.setOnAction(e -> applyPoints(false));

        HBox pointsRow = new HBox(8, pointsField, maxBtn, applyBtn);
        pointsRow.setAlignment(Pos.CENTER_LEFT);

        loyaltyBox.getChildren().addAll(loyaltyBalanceLabel, pointsRow);

        // Cart Table
        TableView<SaleItem> cartTable = new TableView<>(cartItems);
        cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        cartTable.setPrefHeight(220);

        TableColumn<SaleItem, String> itemCol = new TableColumn<>("Item");
        itemCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

        TableColumn<SaleItem, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<SaleItem, String> priceCol = new TableColumn<>("Total");
        priceCol.setCellValueFactory(i -> new SimpleStringProperty(i.getValue().getFormattedTotal()));

        TableColumn<SaleItem, Void> actionCol = new TableColumn<>("");
        actionCol.setPrefWidth(80);
        actionCol.setCellFactory(col -> new TableCell<SaleItem, Void>() {
            private final Button minusBtn = new Button("-");
            private final Button plusBtn = new Button("+");
            private final HBox btnBox = new HBox(4, minusBtn, plusBtn);
            {
                minusBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                plusBtn.getStyleClass().addAll("btn-primary", "btn-small");

                minusBtn.setOnAction(e -> {
                    SaleItem item = getTableView().getItems().get(getIndex());
                    if (item.getQuantity() > 1) {
                        item.setQuantity(item.getQuantity() - 1);
                        cartTable.refresh();
                        recalculateTotals();
                    } else {
                        cartItems.remove(item);
                        recalculateTotals();
                    }
                });

                plusBtn.setOnAction(e -> {
                    SaleItem item = getTableView().getItems().get(getIndex());
                    Product p = productDAO.findById(item.getProductId());
                    if (p != null && item.getQuantity() < p.getQuantity()) {
                        item.setQuantity(item.getQuantity() + 1);
                        cartTable.refresh();
                        recalculateTotals();
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Stock Limit", "Cannot add more. Reached available stock!");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(btnBox);
            }
        });

        cartTable.getColumns().addAll(itemCol, qtyCol, priceCol, actionCol);
        cartTable.setPlaceholder(new Label("Cart is empty. Click ➕ Add on products to start."));

        // Coupon & Discount Bar
        HBox couponBox = new HBox(8);
        TextField couponField = new TextField();
        couponField.setPromptText("Coupon code (e.g. SAVE10)");
        HBox.setHgrow(couponField, Priority.ALWAYS);
        Button applyCouponBtn = new Button("Apply");
        applyCouponBtn.getStyleClass().add("btn-secondary");

        applyCouponBtn.setOnAction(e -> {
            String code = couponField.getText().trim();
            if (code.isEmpty()) return;

            Discount d = discountDAO.findByCode(code);
            double currentSubtotal = calculateSubtotal();

            if (d == null) {
                showAlert(Alert.AlertType.ERROR, "Invalid Coupon", "Discount coupon code not found or inactive.");
            } else if (!d.isValid()) {
                showAlert(Alert.AlertType.ERROR, "Expired Coupon", "This coupon code has expired or is inactive.");
            } else if (currentSubtotal < d.getMinPurchase()) {
                showAlert(Alert.AlertType.WARNING, "Min Purchase Required",
                        String.format("This coupon requires a minimum purchase of ₹%.2f", d.getMinPurchase()));
            } else {
                appliedDiscount = d;
                recalculateTotals();
                showAlert(Alert.AlertType.INFORMATION, "Discount Applied!",
                        "Applied coupon: " + d.getCode() + " (" + d.getValueDisplay() + " off)");
            }
        });
        couponBox.getChildren().addAll(couponField, applyCouponBtn);

        // Gift Card Payment
        VBox giftCardBox = new VBox(6);
        giftCardBox.getStyleClass().add("card");
        giftCardBox.setPadding(new Insets(10));
        Label gcTitle = new Label("🎁 Gift Card Payment");
        gcTitle.getStyleClass().add("sub-label");

        giftCardField.setPromptText("Gift card number (e.g. GC-12345678)");
        HBox.setHgrow(giftCardField, Priority.ALWAYS);
        Button applyGcBtn = new Button("Apply");
        applyGcBtn.getStyleClass().addAll("btn-primary", "btn-small");
        Button clearGcBtn = new Button("Remove");
        clearGcBtn.getStyleClass().addAll("btn-secondary", "btn-small");

        applyGcBtn.setOnAction(e -> applyGiftCard(giftCardField.getText().trim()));
        clearGcBtn.setOnAction(e -> clearGiftCard());
        giftCardField.setOnAction(e -> applyGiftCard(giftCardField.getText().trim()));

        giftCardStatusLabel.getStyleClass().add("sub-label");
        giftCardStatusLabel.setWrapText(true);

        HBox gcRow = new HBox(8, giftCardField, applyGcBtn, clearGcBtn);
        gcRow.setAlignment(Pos.CENTER_LEFT);
        giftCardBox.getChildren().addAll(gcTitle, gcRow, giftCardStatusLabel);

        // Payment Method Combo
        paymentMethodCombo.setItems(FXCollections.observableArrayList("Cash", "Card", "UPI", "Net Banking", "Credit"));
        paymentMethodCombo.getSelectionModel().selectFirst();
        paymentMethodCombo.setMaxWidth(Double.MAX_VALUE);
        paymentMethodCombo.valueProperty().addListener((obs, o, n) -> {
            boolean credit = "Credit".equals(n);
            if (credit && (selectedCustomer == null || selectedCustomer.getId() <= 0)) {
                showAlert(Alert.AlertType.WARNING, "Credit Sale",
                        "Credit sales require a registered customer. Please select a customer first.");
                paymentMethodCombo.getSelectionModel().select(o);
            }
        });

        // Bill Summary Grid
        GridPane summaryGrid = new GridPane();
        summaryGrid.setHgap(10);
        summaryGrid.setVgap(8);
        summaryGrid.setPadding(new Insets(10, 0, 10, 0));

        summaryGrid.add(new Label("Subtotal:"), 0, 0);
        summaryGrid.add(subtotalLabel, 1, 0);
        summaryGrid.add(new Label("Discount:"), 0, 1);
        summaryGrid.add(discountLabel, 1, 1);
        summaryGrid.add(new Label("Loyalty:"), 0, 2);
        summaryGrid.add(loyaltyDiscountLabel, 1, 2);
        summaryGrid.add(new Label("Gift Card:"), 0, 3);
        summaryGrid.add(giftCardLabel, 1, 3);
        summaryGrid.add(new Label("Tax (5%):"), 0, 4);
        summaryGrid.add(taxLabel, 1, 4);

        Label totalText = new Label("Grand Total:");
        totalText.getStyleClass().add("section-title");
        grandTotalLabel.getStyleClass().add("cart-total");

        summaryGrid.add(totalText, 0, 5);
        summaryGrid.add(grandTotalLabel, 1, 5);

        // Checkout Button
        Button checkoutBtn = new Button("💳 COMPLETE CHECKOUT");
        checkoutBtn.getStyleClass().add("btn-primary");
        checkoutBtn.setMaxWidth(Double.MAX_VALUE);
        checkoutBtn.setPrefHeight(48);
        checkoutBtn.setOnAction(e -> processCheckout());

        content.getChildren().addAll(
                header, customerBox, phoneBox, loyaltyBox, cartTable, couponBox, giftCardBox,
                new Label("Payment Method:"), paymentMethodCombo,
                new Separator(), summaryGrid, checkoutBtn
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        panel.getChildren().add(scroll);

        return panel;
    }

    private void loadProducts(String query) {
        availableProducts.clear();
        if (query.isEmpty()) {
            availableProducts.addAll(productDAO.findAll());
        } else {
            availableProducts.addAll(productDAO.search(query));
        }
    }

    private void loadCustomers() {
        customerCombo.getItems().clear();
        Customer walkIn = new Customer("Walk-in Customer", "", "", "");
        walkIn.setId(0);
        customerCombo.getItems().add(walkIn);
        customerCombo.getItems().addAll(customerDAO.findAll());
        customerCombo.getSelectionModel().selectFirst();
        selectedCustomer = walkIn;
        updateLoyaltyBalance();
    }

    private void processScan(String input) {
        String text = input == null ? "" : input.trim();
        if (text.isEmpty()) return;

        int qty = 1;
        String code = text;
        Matcher qtyMatcher = Pattern.compile("^(.*?)[*xX](\\d+)$").matcher(text);
        if (qtyMatcher.matches()) {
            code = qtyMatcher.group(1).trim();
            qty = Integer.parseInt(qtyMatcher.group(2));
            if (qty < 1) qty = 1;
        }

        if (code.isEmpty()) return;

        Product product = productDAO.findByBarcode(code);
        if (product == null) {
            beep();
            setScanStatus("✗ Barcode not found: " + code);
            return;
        }
        if (product.getQuantity() <= 0) {
            beep();
            setScanStatus("✗ " + product.getName() + " is out of stock.");
            return;
        }
        if (qty > product.getQuantity()) {
            beep();
            setScanStatus("✗ Only " + product.getQuantity() + " of " + product.getName() + " in stock.");
            return;
        }

        for (int i = 0; i < qty; i++) {
            addToCart(product);
        }
        beep();
        setScanStatus("✓ " + product.getName() + " added" + (qty > 1 ? " x" + qty : "") + " to cart.");
    }

    private void setScanStatus(String text) {
        scanStatus.setText(text);
    }

    private void beep() {
        try {
            java.awt.Toolkit.getDefaultToolkit().beep();
        } catch (Exception ignored) {
        }
    }

    private void addToCart(Product product) {
        for (SaleItem item : cartItems) {
            if (item.getProductId() == product.getId()) {
                if (item.getQuantity() < product.getQuantity()) {
                    item.setQuantity(item.getQuantity() + 1);
                    recalculateTotals();
                } else {
                    showAlert(Alert.AlertType.WARNING, "Stock Limit", "Cannot add more. Available stock: " + product.getQuantity());
                }
                return;
            }
        }
        // New item in cart
        SaleItem newItem = new SaleItem(product.getId(), product.getName(), 1, product.getSellPrice());
        cartItems.add(newItem);
        recalculateTotals();
    }

    private double calculateSubtotal() {
        return cartItems.stream().mapToDouble(SaleItem::getTotal).sum();
    }

    private void recalculateTotals() {
        double subtotal = calculateSubtotal();
        double discountAmt = 0;
        if (appliedDiscount != null) {
            discountAmt = appliedDiscount.apply(subtotal);
        }

        int maxRedeemable = getMaxRedeemablePoints();
        if (pointsRedeemed > maxRedeemable) {
            pointsRedeemed = maxRedeemable;
            pointsField.setText(pointsRedeemed > 0 ? String.valueOf(pointsRedeemed) : "");
        }
        double loyaltyAmt = pointsRedeemed;

        double taxableAmount = Math.max(0, subtotal - discountAmt - loyaltyAmt);
        double tax = taxableAmount * TAX_RATE;
        double grandTotal = taxableAmount + tax;

        double gcAmount = appliedGiftCard != null ? Math.min(appliedGiftCard.getBalance(), grandTotal) : 0;

        subtotalLabel.setText(String.format("₹%.2f", subtotal));
        discountLabel.setText(String.format("-₹%.2f", discountAmt));
        loyaltyDiscountLabel.setText(String.format("-₹%.2f", loyaltyAmt));
        giftCardLabel.setText(String.format("-₹%.2f", gcAmount));
        taxLabel.setText(String.format("₹%.2f", tax));
        grandTotalLabel.setText(String.format("₹%.2f", grandTotal));
    }

    private int getMaxRedeemablePoints() {
        if (selectedCustomer == null || selectedCustomer.getId() <= 0) return 0;
        double subtotal = calculateSubtotal();
        double discountAmt = appliedDiscount != null ? appliedDiscount.apply(subtotal) : 0;
        double payable = Math.max(0, subtotal - discountAmt);
        return (int) Math.min(selectedCustomer.getLoyaltyPoints(), Math.floor(payable));
    }

    private void updateLoyaltyBalance() {
        if (selectedCustomer == null || selectedCustomer.getId() <= 0) {
            loyaltyBalanceLabel.setText("🅿️  Loyalty Points: 0 — select a customer to redeem.");
            pointsField.setDisable(true);
            return;
        }
        pointsField.setDisable(false);
        loyaltyBalanceLabel.setText("🅿️  " + selectedCustomer.getName()
                + " has " + selectedCustomer.getLoyaltyPoints() + " points (worth ₹"
                + selectedCustomer.getLoyaltyPoints() + " off). 1 point = ₹1.");
    }

    private void applyPoints(boolean useMax) {
        if (selectedCustomer == null || selectedCustomer.getId() <= 0) {
            showAlert(Alert.AlertType.WARNING, "Select Customer", "Select a customer first to redeem loyalty points.");
            return;
        }
        int max = getMaxRedeemablePoints();
        if (max <= 0) {
            showAlert(Alert.AlertType.INFORMATION, "Nothing to Redeem",
                    selectedCustomer.getLoyaltyPoints() <= 0
                            ? "This customer has no loyalty points yet."
                            : "This bill can't be reduced further with points (₹0 payable).");
            return;
        }
        if (useMax) {
            pointsField.setText(String.valueOf(max));
        }
        try {
            int pts = Integer.parseInt(pointsField.getText().trim());
            if (pts < 0) pts = 0;
            if (pts > max) {
                showAlert(Alert.AlertType.WARNING, "Too Many Points",
                        "You can redeem at most " + max + " points for this bill (worth ₹" + max + ").");
                pts = max;
            }
            pointsRedeemed = pts;
            pointsField.setText(pts > 0 ? String.valueOf(pts) : "");
        } catch (NumberFormatException ex) {
            pointsRedeemed = 0;
            pointsField.clear();
        }
        recalculateTotals();
    }

    private void clearCart() {
        cartItems.clear();
        appliedDiscount = null;
        pointsRedeemed = 0;
        pointsField.clear();
        clearGiftCard();
        recalculateTotals();
    }

    private void applyGiftCard(String number) {
        if (number.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Gift Card", "Please enter a gift card number.");
            return;
        }
        GiftCard gc = new com.shop.dao.GiftCardDAO().findByNumber(number);
        if (gc == null) {
            showAlert(Alert.AlertType.ERROR, "Gift Card", "No gift card found with number " + number + ".");
            return;
        }
        if (!gc.isActive()) {
            showAlert(Alert.AlertType.ERROR, "Gift Card", "This gift card is inactive.");
            return;
        }
        if (gc.getBalance() <= 0) {
            showAlert(Alert.AlertType.WARNING, "Gift Card", "This gift card has no remaining balance.");
            return;
        }
        appliedGiftCard = gc;
        giftCardField.setText(gc.getCardNumber());
        giftCardStatusLabel.setText("✅ " + gc.getCardNumber() + " applied. Balance: " + gc.getFormattedBalance());
        recalculateTotals();
    }

    private void clearGiftCard() {
        appliedGiftCard = null;
        giftCardField.clear();
        giftCardStatusLabel.setText("Enter a gift card number to apply.");
        recalculateTotals();
    }

    private void processCheckout() {
        if (cartItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty Cart", "Please add items to cart before checkout!");
            return;
        }

        double subtotal = calculateSubtotal();
        double discountAmt = appliedDiscount != null ? appliedDiscount.apply(subtotal) : 0;
        int maxRedeemable = getMaxRedeemablePoints();
        if (pointsRedeemed > maxRedeemable) pointsRedeemed = maxRedeemable;
        double loyaltyAmt = pointsRedeemed;
        double taxableAmount = Math.max(0, subtotal - discountAmt - loyaltyAmt);
        double tax = taxableAmount * TAX_RATE;
        double grandTotal = taxableAmount + tax;

        double gcAmount = appliedGiftCard != null ? Math.min(appliedGiftCard.getBalance(), grandTotal) : 0;
        if (appliedGiftCard != null && gcAmount > 0) {
            GiftCard fresh = new com.shop.dao.GiftCardDAO().findByNumber(appliedGiftCard.getCardNumber());
            if (fresh == null || !fresh.isActive() || fresh.getBalance() <= 0) {
                showAlert(Alert.AlertType.WARNING, "Gift Card", "The gift card is no longer valid. Please re-apply or remove it.");
                appliedGiftCard = null;
                recalculateTotals();
                return;
            }
            gcAmount = Math.min(fresh.getBalance(), grandTotal);
        }

        Sale sale = new Sale();
        sale.setInvoiceNumber(saleDAO.generateNextInvoiceNumber());
        sale.setCustomerId(selectedCustomer != null ? selectedCustomer.getId() : 0);
        sale.setUserId(currentUser != null ? currentUser.getId() : 1);
        sale.setSubtotal(subtotal);
        sale.setDiscountAmount(discountAmt);
        sale.setPointsRedeemed(pointsRedeemed);
        sale.setTax(tax);
        sale.setTotal(grandTotal);
        sale.setGiftCardAmount(gcAmount);
        if (appliedGiftCard != null && gcAmount > 0) {
            sale.setGiftCardNumber(appliedGiftCard.getCardNumber());
        }
        if (gcAmount >= grandTotal) {
            sale.setPaymentMethod(appliedGiftCard != null ? "Gift Card" : paymentMethodCombo.getValue());
        } else if (gcAmount > 0) {
            sale.setPaymentMethod(paymentMethodCombo.getValue() + " / Gift Card");
        } else {
            sale.setPaymentMethod(paymentMethodCombo.getValue());
        }

        boolean effectiveCredit = "Credit".equals(paymentMethodCombo.getValue()) && gcAmount < grandTotal;
        sale.setCreditSale(effectiveCredit);
        sale.setAmountPaid(effectiveCredit ? 0 : grandTotal);

        for (SaleItem item : cartItems) {
            sale.addItem(item);
        }

        boolean success = saleDAO.createSale(sale);
        if (success) {
            String customerName = selectedCustomer != null ? selectedCustomer.getName() : "Walk-in";
            String phone = phoneField.getText() == null ? "" : phoneField.getText().trim();
            showReceiptWindow(sale, customerName, phone);
            sendEBill(sale, customerName, phone, true);
            clearCart();
            loadProducts(""); // Refresh inventory stock levels
            loadCustomers(); // Refresh loyalty point balances
        } else {
            showAlert(Alert.AlertType.ERROR, "Checkout Failed", "Could not complete transaction. Please try again.");
        }
    }

    private void sendEBill(Sale sale, String customerName, String phone, boolean silent) {
        String p = phone == null ? "" : phone.trim();
        if (p.isEmpty()) {
            if (!silent) {
                showAlert(Alert.AlertType.WARNING, "WhatsApp e-Bill", "Enter a phone number first to send the e-Bill on WhatsApp.");
            }
            return;
        }
        if (WhatsAppSender.normalizePhone(p) == null) {
            if (!silent) {
                showAlert(Alert.AlertType.WARNING, "WhatsApp e-Bill", "Please enter a valid phone number.");
            }
            return;
        }
        if (WhatsAppSender.sendEBill(p, sale, customerName)) {
            if (!silent) {
                showAlert(Alert.AlertType.INFORMATION, "WhatsApp e-Bill",
                        "WhatsApp is opening with the e-Bill for " + p + ".\nJust press Send to deliver it to the customer.");
            }
        } else if (!silent) {
            showAlert(Alert.AlertType.ERROR, "WhatsApp e-Bill",
                    "Could not open WhatsApp. Make sure a default browser is configured.");
        }
    }

    private void showReceiptWindow(Sale sale, String customerName, String phone) {
        Stage receiptStage = new Stage();
        receiptStage.initModality(Modality.APPLICATION_MODAL);
        receiptStage.setTitle("Invoice Receipt - " + sale.getInvoiceNumber());

        VBox root = new VBox(15);
        root.setPadding(new Insets(25));
        root.getStyleClass().add("card");
        root.setAlignment(Pos.TOP_CENTER);

        Label shopTitle = new Label("🛍️ SUPER STORE RETAIL");
        shopTitle.getStyleClass().add("section-title");
        Label headerSub = new Label("123 Main Street, Commerce City\nPhone: +91 98765 43210");
        headerSub.getStyleClass().add("sub-label");
        headerSub.setStyle("-fx-text-alignment: center;");

        VBox invoiceMeta = new VBox(4);
        invoiceMeta.getChildren().addAll(
                new Label("Invoice No: " + sale.getInvoiceNumber()),
                new Label("Date: " + sale.getFormattedDate()),
                new Label("Customer: " + (customerName == null ? "Walk-in" : customerName)),
                new Label("Payment: " + sale.getPaymentMethod())
        );

        Separator sep1 = new Separator();

        VBox itemsBox = new VBox(6);
        for (SaleItem item : sale.getItems()) {
            HBox itemRow = new HBox();
            Label nameL = new Label(item.getProductName() + " x" + item.getQuantity());
            Region s = new Region();
            HBox.setHgrow(s, Priority.ALWAYS);
            Label priceL = new Label(item.getFormattedTotal());
            itemRow.getChildren().addAll(nameL, s, priceL);
            itemsBox.getChildren().add(itemRow);
        }

        Separator sep2 = new Separator();

        VBox totalsBox = new VBox(4);
        VBox totalLines = new VBox(4);
        totalLines.getChildren().addAll(
                new Label(String.format("Subtotal: ₹%.2f", sale.getSubtotal())),
                new Label(String.format("Discount: -₹%.2f", sale.getDiscountAmount())));
        if (sale.getPointsRedeemed() > 0) {
            totalLines.getChildren().add(new Label(String.format("Loyalty Points: -₹%.2f (%d pts)", sale.getLoyaltyDiscount(), sale.getPointsRedeemed())));
        }
        if (sale.getGiftCardAmount() > 0) {
            totalLines.getChildren().add(new Label(String.format("Gift Card (%s): -₹%.2f", sale.getGiftCardNumber(), sale.getGiftCardAmount())));
        }
        totalLines.getChildren().addAll(
                new Label(String.format("Tax (5%%): ₹%.2f", sale.getTax())),
                new Label(String.format("TOTAL: ₹%.2f", sale.getTotal())) {{ setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #16c79a;"); }});
        totalsBox.getChildren().addAll(totalLines);
        if (sale.getCustomerId() > 0) {
            int earned = (int) (sale.getTotal() / 100);
            Label pointsLine = new Label(earned > 0
                    ? "🅿️ Earned " + earned + " loyalty point(s) on this purchase!"
                    : "🅿️ Loyalty points redeemed. Keep shopping to earn more!");
            pointsLine.getStyleClass().add("sub-label");
            totalsBox.getChildren().add(pointsLine);
        }

        Label thankYou = new Label("Thank you for shopping with us!");
        thankYou.getStyleClass().add("sub-label");

        Button printBtn = new Button("🖨️ Print");
        printBtn.getStyleClass().add("btn-primary");
        printBtn.setOnAction(e -> {
            boolean printed = com.shop.util.ReceiptPrinter.printNode(receiptStage, "Receipt " + sale.getInvoiceNumber(), root);
            if (!printed) {
                showAlert(Alert.AlertType.WARNING, "Print", "Print was cancelled or no printer is available.");
            }
        });

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-secondary");
        closeBtn.setOnAction(e -> receiptStage.close());

        Button waBtn = new Button("📲 WhatsApp e-Bill");
        waBtn.getStyleClass().addAll("btn-primary");
        waBtn.setTooltip(new Tooltip("Send this e-Bill to " + (phone == null || phone.isEmpty() ? "a phone number" : phone) + " on WhatsApp"));
        waBtn.setOnAction(e -> sendEBill(sale, customerName, phone, false));

        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(Pos.CENTER);
        buttonRow.getChildren().addAll(waBtn, printBtn, closeBtn);

        root.getChildren().addAll(shopTitle, headerSub, invoiceMeta, sep1, itemsBox, sep2, totalsBox, thankYou, buttonRow);

        Scene scene = new Scene(root, 400, 580);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        receiptStage.setScene(scene);
        receiptStage.show();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void sellGiftCardDialog() {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Sell Gift Card");

        VBox box = new VBox(12);
        box.setPadding(new Insets(20));
        box.getStyleClass().add("card");

        Label title = new Label("🎁 Sell a Gift Card");
        title.getStyleClass().add("section-title");

        TextField amountField = new TextField();
        amountField.setPromptText("Amount (₹)");

        ComboBox<Customer> custCombo = new ComboBox<>();
        custCombo.setPromptText("Assign to customer (optional)");
        custCombo.setMaxWidth(Double.MAX_VALUE);
        custCombo.getItems().addAll(customerDAO.findAll());

        Label status = new Label();
        status.getStyleClass().add("sub-label");
        status.setWrapText(true);

        Button sellBtn = new Button("💵  Sell Gift Card");
        sellBtn.getStyleClass().add("btn-primary");
        sellBtn.setMaxWidth(Double.MAX_VALUE);
        sellBtn.setOnAction(e -> {
            double amount;
            try {
                amount = Double.parseDouble(amountField.getText().trim());
            } catch (NumberFormatException ex) {
                status.setText("⚠️ Please enter a valid amount.");
                return;
            }
            if (amount <= 0) {
                status.setText("⚠️ Amount must be greater than zero.");
                return;
            }
            GiftCard gc = new GiftCard();
            gc.setInitialAmount(Math.round(amount * 100.0) / 100.0);
            gc.setBalance(gc.getInitialAmount());
            Customer c = custCombo.getValue();
            if (c != null) {
                gc.setCustomerId(c.getId());
                gc.setCustomerName(c.getName());
            }
            if (new com.shop.dao.GiftCardDAO().create(gc)) {
                status.setText("✅ Gift card " + gc.getCardNumber() + " issued for ₹"
                        + String.format("%.2f", gc.getInitialAmount()) + ".\nGive this number to the customer.");
                amountField.clear();
                custCombo.setValue(null);
            } else {
                status.setText("❌ Failed to issue gift card. Please try again.");
            }
        });

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-secondary");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> dialogStage.close());

        box.getChildren().addAll(title, amountField, custCombo, sellBtn, status, closeBtn);

        Scene scene = new Scene(box, 360, 330);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }
}
