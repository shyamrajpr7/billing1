package com.shop.view;

import com.shop.dao.*;
import com.shop.model.*;
import com.shop.util.SessionManager;
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

public class POSView {
    private final ProductDAO productDAO = new ProductDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final DiscountDAO discountDAO = new DiscountDAO();
    private final SaleDAO saleDAO = new SaleDAO();
    private final User currentUser = SessionManager.getInstance().getCurrentUser();

    private final ObservableList<SaleItem> cartItems = FXCollections.observableArrayList();
    private final ObservableList<Product> availableProducts = FXCollections.observableArrayList();

    private final Label subtotalLabel = new Label("₹0.00");
    private final Label discountLabel = new Label("-₹0.00");
    private final Label taxLabel = new Label("₹0.00");
    private final Label grandTotalLabel = new Label("₹0.00");

    private Discount appliedDiscount = null;
    private Customer selectedCustomer = null;
    private final ComboBox<Customer> customerCombo = new ComboBox<>();
    private final ComboBox<String> paymentMethodCombo = new ComboBox<>();

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

        return mainLayout;
    }

    private VBox buildCatalogPanel() {
        VBox panel = new VBox(16);

        // Top Search & Barcode Bar
        HBox searchBar = new HBox(12);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search product by name or scan barcode...");
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            loadProducts(newVal.trim());
            // Fast barcode check
            if (!newVal.trim().isEmpty()) {
                Product foundByBarcode = productDAO.findByBarcode(newVal.trim());
                if (foundByBarcode != null && foundByBarcode.getQuantity() > 0) {
                    addToCart(foundByBarcode);
                    searchField.clear();
                }
            }
        });

        searchBar.getChildren().add(searchField);

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

        panel.getChildren().addAll(searchBar, productTable);
        return panel;
    }

    private VBox buildCartPanel() {
        VBox panel = new VBox(14);
        panel.getStyleClass().add("card");

        // Cart Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("🛒 Current Cart");
        title.getStyleClass().add("section-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button clearBtn = new Button("Clear");
        clearBtn.getStyleClass().addAll("btn-secondary", "btn-small");
        clearBtn.setOnAction(e -> clearCart());
        header.getChildren().addAll(title, spacer, clearBtn);

        // Customer Selection
        VBox customerBox = new VBox(6);
        Label custLabel = new Label("Customer");
        custLabel.getStyleClass().add("form-label");

        customerCombo.setMaxWidth(Double.MAX_VALUE);
        customerCombo.setOnAction(e -> selectedCustomer = customerCombo.getValue());

        customerBox.getChildren().addAll(custLabel, customerCombo);

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

        // Payment Method Combo
        paymentMethodCombo.setItems(FXCollections.observableArrayList("Cash", "Card", "UPI", "Net Banking"));
        paymentMethodCombo.getSelectionModel().selectFirst();
        paymentMethodCombo.setMaxWidth(Double.MAX_VALUE);

        // Bill Summary Grid
        GridPane summaryGrid = new GridPane();
        summaryGrid.setHgap(10);
        summaryGrid.setVgap(8);
        summaryGrid.setPadding(new Insets(10, 0, 10, 0));

        summaryGrid.add(new Label("Subtotal:"), 0, 0);
        summaryGrid.add(subtotalLabel, 1, 0);
        summaryGrid.add(new Label("Discount:"), 0, 1);
        summaryGrid.add(discountLabel, 1, 1);
        summaryGrid.add(new Label("Tax (5%):"), 0, 2);
        summaryGrid.add(taxLabel, 1, 2);

        Label totalText = new Label("Grand Total:");
        totalText.getStyleClass().add("section-title");
        grandTotalLabel.getStyleClass().add("cart-total");

        summaryGrid.add(totalText, 0, 3);
        summaryGrid.add(grandTotalLabel, 1, 3);

        // Checkout Button
        Button checkoutBtn = new Button("💳 COMPLETE CHECKOUT");
        checkoutBtn.getStyleClass().add("btn-primary");
        checkoutBtn.setMaxWidth(Double.MAX_VALUE);
        checkoutBtn.setPrefHeight(48);
        checkoutBtn.setOnAction(e -> processCheckout());

        panel.getChildren().addAll(
                header, customerBox, cartTable, couponBox,
                new Label("Payment Method:"), paymentMethodCombo,
                new Separator(), summaryGrid, checkoutBtn
        );

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

        double taxableAmount = Math.max(0, subtotal - discountAmt);
        double tax = taxableAmount * TAX_RATE;
        double grandTotal = taxableAmount + tax;

        subtotalLabel.setText(String.format("₹%.2f", subtotal));
        discountLabel.setText(String.format("-₹%.2f", discountAmt));
        taxLabel.setText(String.format("₹%.2f", tax));
        grandTotalLabel.setText(String.format("₹%.2f", grandTotal));
    }

    private void clearCart() {
        cartItems.clear();
        appliedDiscount = null;
        recalculateTotals();
    }

    private void processCheckout() {
        if (cartItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty Cart", "Please add items to cart before checkout!");
            return;
        }

        double subtotal = calculateSubtotal();
        double discountAmt = appliedDiscount != null ? appliedDiscount.apply(subtotal) : 0;
        double taxableAmount = Math.max(0, subtotal - discountAmt);
        double tax = taxableAmount * TAX_RATE;
        double grandTotal = taxableAmount + tax;

        Sale sale = new Sale();
        sale.setInvoiceNumber(saleDAO.generateNextInvoiceNumber());
        sale.setCustomerId(selectedCustomer != null ? selectedCustomer.getId() : 0);
        sale.setUserId(currentUser != null ? currentUser.getId() : 1);
        sale.setSubtotal(subtotal);
        sale.setDiscountAmount(discountAmt);
        sale.setTax(tax);
        sale.setTotal(grandTotal);
        sale.setPaymentMethod(paymentMethodCombo.getValue());

        for (SaleItem item : cartItems) {
            sale.addItem(item);
        }

        boolean success = saleDAO.createSale(sale);
        if (success) {
            showReceiptWindow(sale);
            clearCart();
            loadProducts(""); // Refresh inventory stock levels
        } else {
            showAlert(Alert.AlertType.ERROR, "Checkout Failed", "Could not complete transaction. Please try again.");
        }
    }

    private void showReceiptWindow(Sale sale) {
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
                new Label("Customer: " + (selectedCustomer != null ? selectedCustomer.getName() : "Walk-in")),
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
        totalsBox.getChildren().addAll(
                new Label(String.format("Subtotal: ₹%.2f", sale.getSubtotal())),
                new Label(String.format("Discount: -₹%.2f", sale.getDiscountAmount())),
                new Label(String.format("Tax (5%%): ₹%.2f", sale.getTax())),
                new Label(String.format("TOTAL: ₹%.2f", sale.getTotal())) {{ setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #16c79a;"); }}
        );

        Label thankYou = new Label("Thank you for shopping with us!");
        thankYou.getStyleClass().add("sub-label");

        Button closeBtn = new Button("Print / Close");
        closeBtn.getStyleClass().add("btn-primary");
        closeBtn.setOnAction(e -> receiptStage.close());

        root.getChildren().addAll(shopTitle, headerSub, invoiceMeta, sep1, itemsBox, sep2, totalsBox, thankYou, closeBtn);

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
}
