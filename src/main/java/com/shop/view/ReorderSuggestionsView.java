package com.shop.view;

import com.shop.dao.ProductDAO;
import com.shop.dao.PurchaseOrderDAO;
import com.shop.dao.SaleDAO;
import com.shop.dao.SupplierDAO;
import com.shop.model.Product;
import com.shop.model.PurchaseOrder;
import com.shop.model.PurchaseOrderItem;
import com.shop.model.ReorderSuggestion;
import com.shop.model.Supplier;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Smart reorder suggestions. Estimates daily demand for every product from the
 * last 30 days of sales, flags anything at risk of running out, proposes a
 * restock quantity per product, groups them by supplier, and creates all the
 * purchase orders in one click.
 */
public class ReorderSuggestionsView {

    private static final int SALES_WINDOW_DAYS = 30;
    private static final int COVER_DAYS = 14;
    private static final int REORDER_THRESHOLD_DAYS = 7;

    private final ProductDAO productDAO = new ProductDAO();
    private final SaleDAO saleDAO = new SaleDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final PurchaseOrderDAO purchaseOrderDAO = new PurchaseOrderDAO();

    private final ObservableList<ReorderSuggestion> suggestions = FXCollections.observableArrayList();
    private final Label summaryLabel = new Label();

    private final ChangeListener<Object> summaryListener = (obs, o, n) -> updateSummary();

    public Node getView() {
        suggestions.addListener((ListChangeListener<ReorderSuggestion>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (ReorderSuggestion s : change.getAddedSubList()) {
                        s.selectedProperty().addListener(summaryListener);
                        s.suggestedQtyProperty().addListener(summaryListener);
                    }
                }
                if (change.wasRemoved()) {
                    for (ReorderSuggestion s : change.getRemoved()) {
                        s.selectedProperty().removeListener(summaryListener);
                        s.suggestedQtyProperty().removeListener(summaryListener);
                    }
                }
            }
            updateSummary();
        });

        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        Label title = new Label("🔁 Smart Reorder Suggestions");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Based on your last " + SALES_WINDOW_DAYS + " days of sales, these products are at risk of running out. "
                + "Quantities are tuned to cover ~" + COVER_DAYS + " days of demand.");
        subtitle.getStyleClass().add("sub-label");
        subtitle.setWrapText(true);

        HBox controlBar = new HBox(12);
        controlBar.setAlignment(Pos.CENTER_LEFT);
        summaryLabel.getStyleClass().add("sub-label");
        HBox.setHgrow(summaryLabel, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");

        Button generateBtn = new Button("📋 Generate Purchase Orders");
        generateBtn.getStyleClass().add("btn-primary");

        controlBar.getChildren().addAll(summaryLabel, refreshBtn, generateBtn);

        TableView<ReorderSuggestion> table = new TableView<>();
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<ReorderSuggestion, Boolean> selCol = new TableColumn<>("");
        selCol.setMinWidth(36);
        selCol.setMaxWidth(36);
        selCol.setCellValueFactory(p -> p.getValue().selectedProperty());
        selCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox cb = new CheckBox();
            {
                cb.setOnAction(e -> {
                    ReorderSuggestion s = getTableView().getItems().get(getIndex());
                    s.setSelected(cb.isSelected());
                    updateSummary();
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0) {
                    setGraphic(null);
                } else {
                    cb.setSelected(getTableView().getItems().get(getIndex()).isSelected());
                    setGraphic(cb);
                }
            }
        });

        TableColumn<ReorderSuggestion, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(p.getValue().getProduct().getName()));

        TableColumn<ReorderSuggestion, String> supplierCol = new TableColumn<>("Supplier");
        supplierCol.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(p.getValue().getProduct().getSupplierName()));

        TableColumn<ReorderSuggestion, Integer> stockCol = new TableColumn<>("In Stock");
        stockCol.setCellValueFactory(p -> new javafx.beans.property.ReadOnlyObjectWrapper<>(p.getValue().getProduct().getQuantity()));

        TableColumn<ReorderSuggestion, Integer> minCol = new TableColumn<>("Min Level");
        minCol.setCellValueFactory(p -> new javafx.beans.property.ReadOnlyObjectWrapper<>(p.getValue().getProduct().getMinStockLevel()));

        TableColumn<ReorderSuggestion, String> avgCol = new TableColumn<>("Avg Sales/Day");
        avgCol.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(String.format("%.1f", p.getValue().getAvgDailySales())));

        TableColumn<ReorderSuggestion, String> daysCol = new TableColumn<>("Stock Left");
        daysCol.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(p.getValue().getDaysOfStockLabel()));

        TableColumn<ReorderSuggestion, Integer> qtyCol = new TableColumn<>("Order Qty");
        qtyCol.setCellValueFactory(p -> new javafx.beans.property.ReadOnlyObjectWrapper<>(p.getValue().getSuggestedQty()));        qtyCol.setCellFactory(col -> new TableCell<>() {
            private final TextField field = new TextField();
            {
                field.setPrefWidth(70);
                field.setOnAction(e -> commit());
                field.focusedProperty().addListener((obs, o, n) -> {
                    if (!n) commit();
                });
            }

            private void commit() {
                if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) return;
                ReorderSuggestion s = getTableView().getItems().get(getIndex());
                try {
                    int v = Integer.parseInt(field.getText().trim());
                    s.setSuggestedQty(v);
                } catch (NumberFormatException ignored) {
                }
                refreshText();
            }

            private void refreshText() {
                ReorderSuggestion s = getTableView().getItems().get(getIndex());
                if (s != null) field.setText(String.valueOf(s.getSuggestedQty()));
            }

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0) {
                    setGraphic(null);
                } else {
                    refreshText();
                    setGraphic(field);
                }
            }
        });

        TableColumn<ReorderSuggestion, String> costCol = new TableColumn<>("Est. Cost");
        costCol.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(String.format("₹%.2f", p.getValue().getEstimatedCost())));

        TableColumn<ReorderSuggestion, String> urgencyCol = new TableColumn<>("Urgency");
        urgencyCol.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(p.getValue().getUrgency()));
        urgencyCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    switch (item) {
                        case ReorderSuggestion.URGENCY_OUT_OF_STOCK -> badge.getStyleClass().add("badge-inactive");
                        case ReorderSuggestion.URGENCY_CRITICAL -> badge.getStyleClass().add("badge-danger");
                        default -> badge.getStyleClass().add("badge-warning");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        table.getColumns().addAll(selCol, nameCol, supplierCol, stockCol, minCol, avgCol, daysCol, qtyCol, costCol, urgencyCol);
        table.setItems(suggestions);
        table.setPlaceholder(new Label("No products need reordering right now. Inventory looks healthy! 🎉"));

        refreshBtn.setOnAction(e -> refresh());
        generateBtn.setOnAction(e -> generateOrders());

        root.getChildren().addAll(title, subtitle, controlBar, table);
        refresh();
        return root;
    }

    private void refresh() {
        List<Product> products = productDAO.findAll();
        Map<Integer, Integer> sold = saleDAO.getProductSalesLastDays(SALES_WINDOW_DAYS);

        List<ReorderSuggestion> computed = new ArrayList<>();
        for (Product p : products) {
            double avgDaily = (sold.getOrDefault(p.getId(), 0) + 0.0) / SALES_WINDOW_DAYS;
            double daysLeft = avgDaily > 0 ? p.getQuantity() / avgDaily : Double.MAX_VALUE;

            boolean needsReorder = p.getQuantity() <= 0
                    || p.getQuantity() <= p.getMinStockLevel()
                    || (avgDaily > 0 && daysLeft < REORDER_THRESHOLD_DAYS);
            if (!needsReorder) continue;

            int target = Math.max((int) Math.ceil(avgDaily * COVER_DAYS), p.getMinStockLevel() * 2);
            int qty = Math.max(target - p.getQuantity(), 1);
            computed.add(new ReorderSuggestion(p, avgDaily, daysLeft, qty));
        }

        computed.sort(Comparator
                .comparingInt((ReorderSuggestion s) -> urgencyRank(s.getUrgency()))
                .thenComparingDouble(s -> s.getDaysOfStockLeft())
                .thenComparing(s -> s.getProduct().getName()));

        suggestions.setAll(computed);
    }

    private int urgencyRank(String urgency) {
        switch (urgency) {
            case ReorderSuggestion.URGENCY_OUT_OF_STOCK: return 0;
            case ReorderSuggestion.URGENCY_CRITICAL: return 1;
            default: return 2;
        }
    }

    private void updateSummary() {
        List<ReorderSuggestion> selected = suggestions.stream().filter(ReorderSuggestion::isSelected).toList();
        int units = selected.stream().mapToInt(ReorderSuggestion::getSuggestedQty).sum();
        double cost = selected.stream().mapToDouble(ReorderSuggestion::getEstimatedCost).sum();
        long supplierCount = selected.stream()
                .map(s -> s.getProduct().getSupplierId())
                .filter(id -> id > 0)
                .distinct().count();
        summaryLabel.setText(suggestions.size() + " product(s) need restock · "
                + selected.size() + " selected · " + units + " units · ₹"
                + String.format("%.2f", cost)
                + " · " + supplierCount + " supplier(s)");
    }

    private void generateOrders() {
        List<ReorderSuggestion> selected = suggestions.stream().filter(ReorderSuggestion::isSelected).toList();
        if (selected.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Nothing Selected", "Tick at least one product to reorder.");
            return;
        }

        List<Product> noSupplier = new ArrayList<>();
        Map<Integer, PurchaseOrder> ordersBySupplier = new LinkedHashMap<>();
        Map<Integer, Supplier> supplierById = new HashMap<>();
        for (Supplier s : supplierDAO.findAll()) supplierById.put(s.getId(), s);

        for (ReorderSuggestion s : selected) {
            Product p = s.getProduct();
            if (p.getSupplierId() <= 0) {
                noSupplier.add(p);
                continue;
            }
            PurchaseOrder po = ordersBySupplier.computeIfAbsent(p.getSupplierId(), k -> {
                PurchaseOrder npo = new PurchaseOrder();
                Supplier sup = supplierById.get(k);
                npo.setSupplierId(k);
                npo.setSupplierName(sup != null ? sup.getCompanyName() : "");
                npo.setStatus(PurchaseOrder.STATUS_PENDING);
                return npo;
            });
            po.getItems().add(new PurchaseOrderItem(p.getId(), p.getName(), p.getBarcode(),
                    s.getSuggestedQty(), p.getBuyPrice()));
            po.computeTotal();
        }

        if (ordersBySupplier.isEmpty()) {
            alert(Alert.AlertType.WARNING, "No Supplier Assigned",
                    "None of the selected products have a supplier. Assign a supplier in Inventory first.");
            return;
        }

        StringBuilder msg = new StringBuilder("This will create ").append(ordersBySupplier.size())
                .append(" purchase order(s):\n\n");
        for (PurchaseOrder po : ordersBySupplier.values()) {
            msg.append("• ").append(po.getSupplierName()).append(" — ")
                    .append(po.getItemCount()).append(" units, ₹")
                    .append(String.format("%.2f", po.getTotalCost())).append("\n");
        }
        if (!noSupplier.isEmpty()) {
            msg.append("\nSkipped (no supplier): ");
            List<String> names = new ArrayList<>();
            for (Product p : noSupplier) names.add(p.getName());
            msg.append(String.join(", ", names));
        }
        msg.append("\n\nContinue?");

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, msg.toString(),
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Generate Purchase Orders");
        confirm.showAndWait().ifPresent(resp -> {
            if (resp != ButtonType.YES) return;
            int created = 0;
            for (PurchaseOrder po : ordersBySupplier.values()) {
                if (purchaseOrderDAO.create(po)) created++;
            }
            alert(created > 0 ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                    created > 0 ? "Purchase Orders Created" : "Could Not Create Orders",
                    created > 0 ? created + " purchase order(s) created and marked Pending. "
                            + "Find them under \"Purchase Orders\" to mark as received."
                            : "No purchase orders could be created. Please try again.");
            refresh();
        });
    }

    private void alert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
