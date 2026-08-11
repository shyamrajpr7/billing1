package com.shop.view;

import com.shop.dao.CustomerDAO;
import com.shop.dao.ProductDAO;
import com.shop.dao.SaleDAO;
import com.shop.dao.SupplierDAO;
import com.shop.dao.UserDAO;
import com.shop.model.Sale;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Global search overlay (Cmd/Ctrl + K) that searches products, customers,
 * invoices, suppliers and employees, then jumps to the relevant view.
 */
public class GlobalSearch {
    private static final KeyCodeCombination SHORTCUT =
            new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN);

    private GlobalSearch() {
    }

    public static void install(Scene scene, DashboardView dashboard) {
        scene.getAccelerators().put(SHORTCUT, () -> open(dashboard));
    }

    public static void open(DashboardView dashboard) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Global Search  (⌘/Ctrl + K)");

        ObservableList<SearchResult> results = FXCollections.observableArrayList();
        ListView<SearchResult> listView = new ListView<>(results);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SearchResult item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.category + "   " + item.label);
                }
            }
        });
        listView.setPrefHeight(320);

        TextField field = new TextField();
        field.setPromptText("🔍 Search products, customers, invoices, suppliers, employees...");
        field.getStyleClass().add("search-field");

        Label status = new Label("Type to search across the whole shop.");
        status.getStyleClass().add("sub-label");

        PauseTransition debounce = new PauseTransition(Duration.millis(200));
        field.textProperty().addListener((obs, o, n) -> {
            debounce.stop();
            debounce.setOnFinished(e -> search(n, results, status));
            debounce.playFromStart();
        });

        Runnable openSelected = () -> {
            SearchResult sel = listView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                stage.close();
                sel.navigate(dashboard);
            }
        };
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) openSelected.run();
        });
        field.setOnAction(e -> openSelected.run());
        listView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) openSelected.run();
        });

        VBox box = new VBox(10, field, listView, status);
        box.setPadding(new Insets(15));
        box.getStyleClass().add("card");

        Scene scene = new Scene(box, 600, 420);
        scene.getStylesheets().add(GlobalSearch.class.getResource("/css/style.css").toExternalForm());
        stage.setScene(scene);
        Platform.runLater(field::requestFocus);
        stage.showAndWait();
    }

    private static void search(String q, ObservableList<SearchResult> results, Label status) {
        results.clear();
        String query = q.trim();
        if (query.isEmpty()) {
            status.setText("Type to search across the whole shop.");
            return;
        }
        String lq = query.toLowerCase();
        int count = 0;

        for (var p : new ProductDAO().search(query)) {
            results.add(new SearchResult("Product", p.getName() + " — ₹" + String.format("%.2f", p.getSellPrice())
                    + " (stock " + p.getQuantity() + ")", p.getId(), 1));
            count++;
        }
        for (var c : new CustomerDAO().search(query)) {
            results.add(new SearchResult("Customer", c.getName()
                    + (c.getPhone() == null || c.getPhone().isEmpty() ? "" : " — " + c.getPhone()), c.getId(), 2));
            count++;
        }
        for (Sale s : new SaleDAO().findAll()) {
            if (s.getInvoiceNumber().toLowerCase().contains(lq)
                    || s.getCustomerName().toLowerCase().contains(lq)) {
                results.add(new SearchResult("Invoice", s.getInvoiceNumber() + " — " + s.getCustomerName()
                        + " — " + s.getFormattedTotal(), s.getId(), 3));
                if (++count > 50) break;
            }
        }
        for (var sup : new SupplierDAO().search(query)) {
            results.add(new SearchResult("Supplier", sup.getCompanyName()
                    + (sup.getPhone() == null || sup.getPhone().isEmpty() ? "" : " — " + sup.getPhone()), sup.getId(), 4));
            count++;
        }
        for (var u : new UserDAO().findAll()) {
            if (u.getFullName().toLowerCase().contains(lq)
                    || u.getUsername().toLowerCase().contains(lq)) {
                results.add(new SearchResult("Employee", u.getFullName() + " (" + u.getUsername() + ")", u.getId(), 5));
                count++;
            }
        }
        status.setText(results.isEmpty() ? "No results found."
                : count + " result(s) found. Double-click or press Enter to open.");
    }

    public static class SearchResult {
        public final String category;
        public final String label;
        public final int id;
        public final int type;

        SearchResult(String category, String label, int id, int type) {
            this.category = category;
            this.label = label;
            this.id = id;
            this.type = type;
        }

        void navigate(DashboardView dashboard) {
            switch (type) {
                case 1 -> dashboard.loadView("Inventory Management", new InventoryView().getView());
                case 2 -> dashboard.loadView("Customer Management", new CustomerView().getView());
                case 3 -> dashboard.loadView("Sales & Reports", new ReportsView().getView());
                case 4 -> dashboard.loadView("Supplier Management", new SupplierView().getView());
                default -> dashboard.loadView("Employee Management", new EmployeeView().getView());
            }
        }
    }
}
