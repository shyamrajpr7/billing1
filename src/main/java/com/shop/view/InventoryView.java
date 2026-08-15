package com.shop.view;

import com.shop.dao.ProductDAO;
import com.shop.dao.SupplierDAO;
import com.shop.model.Product;
import com.shop.model.Supplier;
import com.shop.util.BarcodeLabelUtil;
import com.shop.util.CsvImportUtil;
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
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.UUID;

public class InventoryView {
    private final ProductDAO productDAO = new ProductDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final ObservableList<Product> productList = FXCollections.observableArrayList();
    private final TableView<Product> table = new TableView<>();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        // Control Bar (Search + Add Product Button)
        HBox controlBar = new HBox(12);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search products by name, category, or barcode...");
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadProducts(newVal.trim()));

        CheckBox lowStockCheck = new CheckBox("Low stock only");
        lowStockCheck.setStyle("-fx-font-size: 12px;");
        lowStockCheck.setOnAction(e -> loadProducts(searchField.getText().trim()));

        Button addProductBtn = new Button("➕  Add New Product");
        addProductBtn.getStyleClass().add("btn-primary");
        addProductBtn.setOnAction(e -> showProductDialog(null));

        Button importBtn = new Button("📥  Import CSV");
        importBtn.getStyleClass().add("btn-secondary");
        importBtn.setOnAction(e -> importCsv());

        controlBar.getChildren().addAll(searchField, lowStockCheck, importBtn, addProductBtn);

        // Product Table
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Product, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setMaxWidth(60);

        TableColumn<Product, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Product, String> barcodeCol = new TableColumn<>("Barcode");
        barcodeCol.setCellValueFactory(new PropertyValueFactory<>("barcode"));

        TableColumn<Product, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<Product, String> buyPriceCol = new TableColumn<>("Buy Price");
        buyPriceCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().getBuyPrice())));

        TableColumn<Product, String> sellPriceCol = new TableColumn<>("Sell Price");
        sellPriceCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().getSellPrice())));

        TableColumn<Product, String> stockValueCol = new TableColumn<>("Stock Value");
        stockValueCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f",
                p.getValue().getBuyPrice() * p.getValue().getQuantity())));

        TableColumn<Product, Integer> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Product, String> statusCol = new TableColumn<>("Stock Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("stockStatus"));
        statusCol.setCellFactory(col -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    if ("In Stock".equals(item)) badge.getStyleClass().add("badge-active");
                    else if ("Low Stock".equals(item)) badge.getStyleClass().add("badge-warning");
                    else badge.getStyleClass().add("badge-inactive");
                    setGraphic(badge);
                }
            }
        });

        TableColumn<Product, String> supplierCol = new TableColumn<>("Supplier");
        supplierCol.setCellValueFactory(new PropertyValueFactory<>("supplierName"));

        TableColumn<Product, String> expiryCol = new TableColumn<>("Expiry Date");
        expiryCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getExpiryDateLabel()));
        expiryCol.setCellFactory(col -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || "—".equals(item)) {
                    setText(item == null ? null : item);
                    setGraphic(null);
                } else {
                    Product p = getTableView().getItems().get(getIndex());
                    Label badge = new Label(item);
                    if (p.hasExpired()) badge.getStyleClass().add("badge-inactive");
                    else if (p.isExpiringSoon(30)) badge.getStyleClass().add("badge-warning");
                    else badge.getStyleClass().add("badge-active");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Product, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<Product, Void>() {
            private final Button editBtn = new Button("✏️");
            private final Button labelBtn = new Button("🏷️");
            private final Button delBtn = new Button("🗑️");
            private final HBox btnBox = new HBox(6, editBtn, labelBtn, delBtn);
            {
                editBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                labelBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                delBtn.getStyleClass().addAll("btn-danger", "btn-small");
                labelBtn.setTooltip(new Tooltip("Print barcode label"));

                editBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    showProductDialog(p);
                });

                labelBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    printBarcodeLabel(p);
                });

                delBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete '" + p.getName() + "'?", ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.YES) {
                            productDAO.delete(p.getId());
                            loadProducts("");
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(btnBox);
            }
        });

        table.getColumns().addAll(idCol, nameCol, barcodeCol, categoryCol, buyPriceCol, sellPriceCol, stockValueCol, qtyCol, statusCol, supplierCol, expiryCol, actionCol);
        table.setItems(productList);
        table.setPlaceholder(new Label("No inventory records found. Click 'Add New Product' to get started."));

        root.getChildren().addAll(controlBar, table);
        loadProducts("");
        return root;
    }

    private void importCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Products from CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv", "*.txt"));
        java.io.File file = chooser.showOpenDialog(table.getScene().getWindow());
        if (file == null) return;

        CsvImportUtil.ImportResult result = CsvImportUtil.parse(file.toPath());
        if (result.errors.isEmpty() && result.products.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Import", "No valid product rows found in the file.");
            return;
        }

        int imported = 0;
        java.util.List<String> insertErrors = new java.util.ArrayList<>();
        for (Product p : result.products) {
            if (productDAO.insert(p)) {
                imported++;
            } else {
                insertErrors.add(p.getName());
            }
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Imported ").append(imported).append(" of ").append(result.products.size()).append(" rows.\n\n");
        summary.append("Columns expected:\nName, Category, Barcode, Quantity, Cost Price, Sell Price, Min Stock, Supplier, Expiry (yyyy-MM-dd)\n\n");
        if (!insertErrors.isEmpty()) {
            summary.append("Failed to insert: ").append(String.join(", ", insertErrors)).append("\n\n");
        }
        if (!result.errors.isEmpty()) {
            summary.append("Skipped rows (").append(result.skipped).append("):\n");
            for (String err : result.errors) summary.append("  • ").append(err).append("\n");
        }

        showAlert(imported > 0 ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                "CSV Import Complete", summary.toString());
        loadProducts("");
    }

    private void loadProducts(String query) {
        productList.clear();
        List<Product> source;
        if (query.isEmpty()) {
            source = productDAO.findAll();
        } else {
            source = productDAO.search(query);
        }
        for (Product p : source) {
            if (lowStockCheck.isSelected() && p.getQuantity() > p.getMinStockLevel()) continue;
            productList.add(p);
        }
    }

    private void showProductDialog(Product productToEdit) {
        boolean isEdit = (productToEdit != null);
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(isEdit ? "Edit Product" : "Add New Product");

        VBox form = new VBox(14);
        form.setPadding(new Insets(20));
        form.getStyleClass().add("card");

        TextField nameField = new TextField(isEdit ? productToEdit.getName() : "");
        nameField.setPromptText("Product Name");

        HBox barcodeBox = new HBox(8);
        TextField barcodeField = new TextField(isEdit ? productToEdit.getBarcode() : "");
        barcodeField.setPromptText("Barcode");
        HBox.setHgrow(barcodeField, Priority.ALWAYS);
        Button genBarcodeBtn = new Button("⚡ Auto");
        genBarcodeBtn.getStyleClass().add("btn-secondary");
        genBarcodeBtn.setOnAction(e -> barcodeField.setText("P" + UUID.randomUUID().toString().substring(0, 10).toUpperCase().replace("-", "")));
        barcodeBox.getChildren().addAll(barcodeField, genBarcodeBtn);

        TextField categoryField = new TextField(isEdit ? productToEdit.getCategory() : "");
        categoryField.setPromptText("Category (e.g. Electronics, Grocery)");

        TextField buyPriceField = new TextField(isEdit ? String.valueOf(productToEdit.getBuyPrice()) : "");
        buyPriceField.setPromptText("Cost / Buy Price (₹)");

        TextField sellPriceField = new TextField(isEdit ? String.valueOf(productToEdit.getSellPrice()) : "");
        sellPriceField.setPromptText("Selling Price (₹)");

        TextField qtyField = new TextField(isEdit ? String.valueOf(productToEdit.getQuantity()) : "10");
        qtyField.setPromptText("Current Quantity");

        TextField minQtyField = new TextField(isEdit ? String.valueOf(productToEdit.getMinStockLevel()) : "5");
        minQtyField.setPromptText("Min Alert Threshold");

        DatePicker expiryPicker = new DatePicker(isEdit ? productToEdit.getExpiryDate() : null);
        expiryPicker.setPromptText("Select expiry date (optional)");

        ComboBox<Supplier> supplierCombo = new ComboBox<>();
        supplierCombo.getItems().clear();
        Supplier noSupplier = new Supplier("None", "", "", "", "");
        noSupplier.setId(0);
        supplierCombo.getItems().add(noSupplier);
        supplierCombo.getItems().addAll(supplierDAO.findAll());
        supplierCombo.setMaxWidth(Double.MAX_VALUE);

        if (isEdit && productToEdit.getSupplierId() > 0) {
            for (Supplier s : supplierCombo.getItems()) {
                if (s.getId() == productToEdit.getSupplierId()) {
                    supplierCombo.getSelectionModel().select(s);
                    break;
                }
            }
        } else {
            supplierCombo.getSelectionModel().selectFirst();
        }

        Button saveBtn = new Button(isEdit ? "Update Product" : "Save Product");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setMaxWidth(Double.MAX_VALUE);

        saveBtn.setOnAction(e -> {
            try {
                String name = nameField.getText().trim();
                String barcode = barcodeField.getText().trim();
                String category = categoryField.getText().trim();
                double buyPrice = Double.parseDouble(buyPriceField.getText().trim());
                double sellPrice = Double.parseDouble(sellPriceField.getText().trim());
                int qty = Integer.parseInt(qtyField.getText().trim());
                int minQty = Integer.parseInt(minQtyField.getText().trim());
                Supplier selectedSup = supplierCombo.getValue();
                int supplierId = selectedSup != null ? selectedSup.getId() : 0;

                if (name.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Validation Error", "Product name cannot be empty.");
                    return;
                }

                Product p = isEdit ? productToEdit : new Product();
                p.setName(name);
                p.setBarcode(barcode);
                p.setCategory(category);
                p.setBuyPrice(buyPrice);
                p.setSellPrice(sellPrice);
                p.setQuantity(qty);
                p.setMinStockLevel(minQty);
                p.setSupplierId(supplierId);
                p.setExpiryDate(expiryPicker.getValue());

                if (isEdit) {
                    productDAO.update(p);
                } else {
                    productDAO.insert(p);
                }

                dialog.close();
                loadProducts("");
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.ERROR, "Format Error", "Prices and quantities must be valid numbers.");
            }
        });

        form.getChildren().addAll(
                new Label("Product Name"), nameField,
                new Label("Barcode"), barcodeBox,
                new Label("Category"), categoryField,
                new HBox(10, new VBox(6, new Label("Cost Price (₹)"), buyPriceField), new VBox(6, new Label("Selling Price (₹)"), sellPriceField)),
                new HBox(10, new VBox(6, new Label("Quantity"), qtyField), new VBox(6, new Label("Min Stock Level"), minQtyField)),
                new Label("Expiry Date (optional)"), expiryPicker,
                new Label("Supplier"), supplierCombo,
                saveBtn
        );

        Scene scene = new Scene(form, 450, 600);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.show();
    }

    private void printBarcodeLabel(Product p) {
        BarcodeLabelUtil.printLabel(table.getScene().getWindow(), p, 1);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
