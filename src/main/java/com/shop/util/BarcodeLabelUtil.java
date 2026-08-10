package com.shop.util;

import com.shop.model.Product;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public class BarcodeLabelUtil {

    public static VBox buildLabel(Product p, int copies) {
        VBox root = new VBox(4);
        root.setPadding(new Insets(12));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 1;");

        Label nameLabel = new Label(p.getName());
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: black;");
        nameLabel.setWrapText(true);
        nameLabel.setAlignment(Pos.CENTER);

        Label barcodeLabel = new Label(p.getBarcode());
        barcodeLabel.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: black;");

        Label priceLabel = new Label("₹" + String.format("%.2f", p.getSellPrice()));
        priceLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: black;");

        VBox single = new VBox(2, nameLabel, barcodeLabel, priceLabel);
        single.setAlignment(Pos.CENTER);

        VBox page = new VBox(10);
        page.setPadding(new Insets(16));
        page.setAlignment(Pos.CENTER);
        for (int i = 0; i < copies; i++) {
            VBox copy = new VBox(0, single);
            copy.setPadding(new Insets(10));
            copy.setStyle("-fx-border-color: black; -fx-border-width: 0 0 1 0;");
            page.getChildren().add(copy);
        }
        return page;
    }

    public static void printLabel(Window owner, Product product, int copies) {
        VBox label = buildLabel(product, copies);
        ReceiptPrinter.printNode(owner, "Barcode " + product.getBarcode(), label);
    }
}
