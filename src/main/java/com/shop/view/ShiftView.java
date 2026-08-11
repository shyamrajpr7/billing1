package com.shop.view;

import com.shop.dao.ShiftDAO;
import com.shop.model.Shift;
import com.shop.model.ShiftReport;
import com.shop.model.User;
import com.shop.util.ReceiptPrinter;
import com.shop.util.SessionManager;
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

public class ShiftView {
    private final ShiftDAO shiftDAO = new ShiftDAO();
    private final User currentUser = SessionManager.getInstance().getCurrentUser();

    private final ObservableList<Shift> shiftList = FXCollections.observableArrayList();
    private final TableView<Shift> table = new TableView<>();
    private final VBox currentPanel = new VBox(10);
    private final VBox openPanel = new VBox(10);
    private final StackPane statusArea = new StackPane();
    private final Label statusText = new Label();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label title = new Label("🏦 Shift & Cash Drawer");
        title.getStyleClass().add("section-title");
        Label sub = new Label("Track cashier shifts, reconcile the cash drawer, and produce X/Z reports.");
        sub.getStyleClass().add("sub-label");
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> refresh());
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(sub, Priority.ALWAYS);
        headerRow.getChildren().addAll(sub, refreshBtn);
        headerCard.getChildren().addAll(title, headerRow);

        buildOpenPanel();
        buildCurrentPanel();

        statusText.getStyleClass().add("sub-label");
        statusText.setWrapText(true);
        statusArea.setAlignment(Pos.CENTER_LEFT);
        statusArea.getChildren().add(statusText);

        VBox tableCard = new VBox(8);
        tableCard.getStyleClass().add("card");
        Label tableTitle = new Label("🗂️ Shift History");
        tableTitle.getStyleClass().add("section-title");

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Shift, String> cashierCol = new TableColumn<>("Cashier");
        cashierCol.setCellValueFactory(new PropertyValueFactory<>("cashierName"));

        TableColumn<Shift, String> openedCol = new TableColumn<>("Opened At");
        openedCol.setCellValueFactory(new PropertyValueFactory<>("openedAtLabel"));

        TableColumn<Shift, String> closedCol = new TableColumn<>("Closed At");
        closedCol.setCellValueFactory(new PropertyValueFactory<>("closedAtLabel"));

        TableColumn<Shift, String> openingCol = new TableColumn<>("Opening");
        openingCol.setCellValueFactory(new PropertyValueFactory<>("formattedOpening"));

        TableColumn<Shift, String> expectedCol = new TableColumn<>("Expected Cash");
        expectedCol.setCellValueFactory(new PropertyValueFactory<>("formattedExpected"));

        TableColumn<Shift, String> countedCol = new TableColumn<>("Counted");
        countedCol.setCellValueFactory(new PropertyValueFactory<>("formattedCounted"));

        TableColumn<Shift, String> varianceCol = new TableColumn<>("Variance");
        varianceCol.setCellValueFactory(new PropertyValueFactory<>("formattedVariance"));
        varianceCol.setCellFactory(col -> new TableCell<Shift, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                getStyleClass().removeAll("cell-accent", "cell-danger");
                if (!empty && item != null) {
                    getStyleClass().add(item.startsWith("-") ? "cell-danger" : "cell-accent");
                }
            }
        });

        TableColumn<Shift, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<Shift, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("Open".equals(item) ? "badge-warning" : "badge-active");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        table.getColumns().addAll(cashierCol, openedCol, closedCol, openingCol, expectedCol, countedCol, varianceCol, statusCol);
        table.setItems(shiftList);
        table.setPlaceholder(new Label("No shifts recorded yet."));

        tableCard.getChildren().addAll(tableTitle, table);

        root.getChildren().addAll(headerCard, statusArea, openPanel, currentPanel, tableCard);
        refresh();
        return new ScrollPane(root) {{ setFitToWidth(true); setStyle("-fx-background-color: transparent;"); }};
    }

    private void buildOpenPanel() {
        openPanel.getStyleClass().add("card");
        openPanel.setPadding(new Insets(16));

        Label openTitle = new Label("▶️  Open New Shift");
        openTitle.getStyleClass().add("section-title");

        Label info = new Label("Cashier: " + (currentUser != null ? currentUser.getFullName() : "Unknown"));
        info.getStyleClass().add("sub-label");

        TextField openingField = new TextField("1000");
        openingField.setPromptText("Opening cash in drawer (₹)");
        openingField.setMaxWidth(220);

        Button openBtn = new Button("🔓  Open Shift");
        openBtn.getStyleClass().add("btn-primary");

        openBtn.setOnAction(e -> {
            try {
                double opening = Double.parseDouble(openingField.getText().trim().replace(",", ""));
                if (opening < 0) throw new NumberFormatException();
                Shift shift = new Shift();
                shift.setCashierId(currentUser != null ? currentUser.getId() : 0);
                shift.setCashierName(currentUser != null ? currentUser.getFullName() : "Unknown");
                shift.setOpeningBalance(opening);
                if (shiftDAO.openShift(shift)) {
                    refresh();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Could not open shift. Try again.").showAndWait();
                }
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.ERROR, "Opening balance must be a valid number.").showAndWait();
            }
        });

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(new Label("Starting cash:"), openingField, openBtn);

        openPanel.getChildren().addAll(openTitle, info, row);
    }

    private void buildCurrentPanel() {
        currentPanel.getStyleClass().add("card");
        currentPanel.setPadding(new Insets(16));

        Label curTitle = new Label("🧾  Current Shift — Live X Report");
        curTitle.getStyleClass().add("section-title");
        Label curSub = new Label();
        curSub.getStyleClass().add("sub-label");

        Button xBtn = new Button("📄  View X Report");
        xBtn.getStyleClass().add("btn-secondary");
        xBtn.setOnAction(e -> {
            Shift open = shiftDAO.findOpenShift();
            if (open != null) showReportDialog(shiftDAO.computeReport(open), false);
        });

        Button closeBtn = new Button("🔒  Close Shift (Z Report)");
        closeBtn.getStyleClass().add("btn-danger");
        closeBtn.setOnAction(e -> showCloseDialog());

        HBox btnRow = new HBox(12);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        btnRow.getChildren().addAll(xBtn, closeBtn);

        currentPanel.getChildren().addAll(curTitle, curSub, btnRow);
    }

    private void refresh() {
        List<Shift> all = shiftDAO.findAll();
        shiftList.clear();
        shiftList.addAll(all);

        Shift open = null;
        for (Shift s : all) {
            if (Shift.STATUS_OPEN.equals(s.getStatus())) {
                open = s;
                break;
            }
        }

        openPanel.setVisible(open == null);
        openPanel.setManaged(open == null);
        currentPanel.setVisible(open != null);
        currentPanel.setManaged(open != null);

        if (open != null) {
            Label curSub = (Label) currentPanel.getChildren().get(1);
            curSub.setText("Cashier: " + open.getCashierName() + "  •  Opened: " + open.getOpenedAtLabel()
                    + "  •  Starting Cash: " + open.getFormattedOpening());
            ShiftReport report = shiftDAO.computeReport(open);
            statusText.setText("💰 Live: " + report.getSalesCount() + " sale(s)  •  Cash in drawer: ₹"
                    + String.format("%,.2f", report.getOpeningBalance() + report.getCashSales()
                    - report.getCashRefunds() - report.getCashExpenses()));
        } else {
            statusText.setText("No shift is currently open. Open a shift to begin tracking the cash drawer.");
        }
    }

    private void showCloseDialog() {
        Shift open = shiftDAO.findOpenShift();
        if (open == null) return;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Close Shift #" + open.getId());

        VBox form = new VBox(14);
        form.setPadding(new Insets(20));
        form.getStyleClass().add("card");

        ShiftReport report = shiftDAO.computeReport(open);
        Label expected = new Label("Expected cash in drawer: ₹" + String.format("%,.2f", report.getExpectedCash()));
        expected.getStyleClass().add("form-label");

        TextField countedField = new TextField();
        countedField.setPromptText("Counted cash (₹)");

        TextField notesField = new TextField();
        notesField.setPromptText("Notes (optional)");

        Button closeBtn = new Button("🔒  Close & Generate Z Report");
        closeBtn.getStyleClass().add("btn-primary");
        closeBtn.setMaxWidth(Double.MAX_VALUE);

        closeBtn.setOnAction(e -> {
            try {
                double counted = Double.parseDouble(countedField.getText().trim().replace(",", ""));
                if (counted < 0) throw new NumberFormatException();
                if (shiftDAO.closeShift(open.getId(), counted, notesField.getText().trim())) {
                    dialog.close();
                    ShiftReport finalReport = shiftDAO.computeReport(shiftDAO.findAll().stream()
                            .filter(s -> s.getId() == open.getId()).findFirst().orElse(open));
                    finalReport.setCountedCash(counted);
                    finalReport.setVariance(counted - report.getExpectedCash());
                    showReportDialog(finalReport, true);
                    refresh();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Could not close shift.").showAndWait();
                }
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.ERROR, "Counted cash must be a valid number.").showAndWait();
            }
        });

        form.getChildren().addAll(
                new Label("Close Shift"),
                expected,
                new Label("Counted Cash (₹)"), countedField,
                new Label("Notes"), notesField,
                closeBtn);

        Scene scene = new Scene(form, 420, 320);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.show();
    }

    private void showReportDialog(ShiftReport report, boolean isZ) {
        VBox reportNode = buildReportNode(report, isZ);
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle((isZ ? "Z Report" : "X Report") + " — Shift #" + report.getShiftId());

        Button printBtn = new Button("🖨️  Print");
        printBtn.getStyleClass().add("btn-primary");
        printBtn.setOnAction(e -> ReceiptPrinter.printDetached(dialog, (isZ ? "Z" : "X") + "-Report", reportNode,
                getClass().getResource("/css/style.css").toExternalForm()));

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("btn-secondary");
        closeBtn.setOnAction(e -> dialog.close());

        HBox btnRow = new HBox(12);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.getChildren().addAll(printBtn, closeBtn);

        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        box.getChildren().addAll(reportNode, btnRow);

        Scene scene = new Scene(box, 460, 640);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.show();
    }

    private VBox buildReportNode(ShiftReport r, boolean isZ) {
        VBox root = new VBox(6);
        root.setPadding(new Insets(12));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: white; -fx-font-family: 'Courier New', monospace;");

        Label head = new Label((isZ ? "Z REPORT" : "X REPORT"));
        head.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label head2 = new Label("🛍️ SUPER STORE RETAIL");
        head2.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Label meta = new Label("Shift #" + r.getShiftId() + "  Cashier: " + r.getCashierName() + "\n"
                + "Opened: " + r.getOpenedAtLabel() + "\n" + "Closed: " + r.getClosedAtLabel());
        meta.setStyle("-fx-font-size: 11px;");

        VBox sales = new VBox(2);
        sales.setStyle("-fx-font-size: 12px;");
        sales.getChildren().addAll(
                line("Sales Count", String.valueOf(r.getSalesCount())),
                line("Cash Sales", fmt(r.getCashSales())),
                line("Card Sales", fmt(r.getCardSales())),
                line("UPI Sales", fmt(r.getUpiSales())),
                line("Net Banking", fmt(r.getNetBankingSales())),
                line("Credit Sales", fmt(r.getCreditSales())),
                line("Gift Card Redeemed", fmt(r.getGiftCardAmount())),
                line("TOTAL SALES", fmt(r.getTotalSales())));

        VBox drawer = new VBox(2);
        drawer.setStyle("-fx-font-size: 12px;");
        Label dHead = new Label("CASH DRAWER");
        dHead.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        drawer.getChildren().addAll(dHead,
                line("Opening Balance", fmt(r.getOpeningBalance())),
                line("Cash Refunds (-)", fmt(r.getCashRefunds())),
                line("Cash Expenses (-)", fmt(r.getCashExpenses())),
                line("Expected Cash", fmt(r.getExpectedCash())),
                line("Counted Cash", fmt(r.getCountedCash())),
                line("Variance", (r.getVariance() >= 0 ? "+" : "") + fmt(r.getVariance())));

        Label footer = new Label(isZ ? "Shift closed. Safe to reconcile drawer."
                : "Interim report. Shift still open.");
        footer.setStyle("-fx-font-size: 11px;");

        root.getChildren().addAll(head, head2, meta, new Separator(), sales, new Separator(), drawer, footer);
        return root;
    }

    private HBox line(String label, String value) {
        HBox row = new HBox();
        Label l = new Label(label);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label v = new Label(value);
        if (value.contains("TOTAL") || label.startsWith("TOTAL") || label.equals("Expected Cash")) {
            l.setStyle("-fx-font-weight: bold;");
            v.setStyle("-fx-font-weight: bold;");
        }
        row.getChildren().addAll(l, spacer, v);
        return row;
    }

    private String fmt(double v) { return String.format("₹%,.2f", v); }
}
