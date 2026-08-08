package com.shop.util;

import com.shop.model.Sale;
import com.shop.model.SaleItem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import javafx.stage.Window;

public class ReceiptPrinter {

    private ReceiptPrinter() {
    }

    /**
     * Prints the given node (already shown, e.g. a receipt window root).
     * Scales it to fit one page.
     *
     * @return true if the job was sent to the printer successfully
     */
    public static boolean printNode(Window owner, String jobName, Node node) {
        Printer printer = Printer.getDefaultPrinter();
        if (printer == null) return false;

        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job == null) return false;

        job.getJobSettings().setJobName(jobName);
        PageLayout layout = printer.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.HARDWARE_MINIMUM);

        if (!job.showPrintDialog(owner)) return false;

        double contentWidth = layout.getPrintableWidth();
        double contentHeight = layout.getPrintableHeight();
        double nodeWidth = node.getBoundsInParent().getWidth();
        double nodeHeight = node.getBoundsInParent().getHeight();
        double scale = Math.min(contentWidth / nodeWidth, contentHeight / nodeHeight);
        if (scale > 1) scale = 1;

        Scale transform = new Scale(scale, scale, 0, 0);
        node.getTransforms().add(transform);
        try {
            boolean printed = job.printPage(layout, node);
            if (printed) {
                return job.endJob();
            }
            job.cancelJob();
            return false;
        } finally {
            node.getTransforms().remove(transform);
        }
    }

    /**
     * Builds a receipt node for a sale. Uses inline styles so it prints
     * consistently even when detached from the main scene.
     */
    public static VBox buildReceipt(Sale sale, String customerName) {
        VBox root = new VBox(8);
        root.setPadding(new Insets(16));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: white; -fx-font-family: 'Courier New', monospace;");

        Label shop = new Label("🛍️ SUPER STORE RETAIL");
        shop.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label address = new Label("123 Main Street, Commerce City\nPhone: +91 98765 43210");
        address.setStyle("-fx-font-size: 11px; -fx-text-alignment: center;");
        address.setWrapText(true);

        VBox meta = new VBox(2);
        meta.setStyle("-fx-font-size: 12px;");
        meta.getChildren().addAll(
                new Label("Invoice No: " + sale.getInvoiceNumber()),
                new Label("Date: " + sale.getFormattedDate()),
                new Label("Customer: " + (customerName == null ? "Walk-in" : customerName)),
                new Label("Cashier: " + sale.getUserName()),
                new Label("Payment: " + sale.getPaymentMethod())
        );

        VBox items = new VBox(4);
        items.setStyle("-fx-font-size: 12px;");
        for (SaleItem item : sale.getItems()) {
            HBox row = new HBox();
            Label name = new Label(item.getProductName() + "  x" + item.getQuantity());
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label total = new Label(item.getFormattedTotal());
            row.getChildren().addAll(name, spacer, total);
            items.getChildren().add(row);
        }

        VBox totals = new VBox(2);
        totals.setStyle("-fx-font-size: 12px;");
        Label grand = new Label(String.format("TOTAL: ₹%.2f", sale.getTotal()));
        grand.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        totals.getChildren().addAll(
                new Label(String.format("Subtotal: ₹%.2f", sale.getSubtotal())),
                new Label(String.format("Discount: -₹%.2f", sale.getDiscountAmount())),
                new Label(String.format("Tax (5%%): ₹%.2f", sale.getTax())),
                grand
        );

        Label thanks = new Label("Thank you for shopping with us!");
        thanks.setStyle("-fx-font-size: 11px;");
        Label tag = new Label("--- Powered by Shop Management ---");
        tag.setStyle("-fx-font-size: 10px;");

        root.getChildren().addAll(shop, address, new Separator(), meta, new Separator(), items, new Separator(), totals, thanks, tag);
        return root;
    }

    /**
     * Prints a receipt node that is NOT attached to any scene.
     * Attaches it to an off-screen scene to apply CSS and layout.
     */
    public static void printDetached(Window owner, String jobName, VBox receipt, String stylesheet) {
        Stage hidden = new Stage();
        Scene scene = new Scene(receipt);
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet);
        }
        hidden.setScene(scene);
        receipt.applyCss();
        receipt.autosize();
        printNode(owner, jobName, receipt);
        hidden.close();
    }

    /**
     * Helper to build a print action: prints the currently displayed node.
     * Returns true if printed.
     */
    public static boolean printButton(Node root, String invoiceNumber) {
        Window owner = root.getScene() != null ? root.getScene().getWindow() : null;
        return printNode(owner, "Receipt " + invoiceNumber, root);
    }
}
