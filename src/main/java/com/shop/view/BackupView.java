package com.shop.view;

import com.shop.dao.ActivityLogDAO;
import com.shop.util.BackupUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;

public class BackupView {
    private final ActivityLogDAO logDAO = new ActivityLogDAO();
    private final Label statusLabel = new Label("Choose an option below.");

    public Node getView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setMaxWidth(560);

        Label title = new Label("🛡️  Backup & Restore");
        title.getStyleClass().add("section-title");

        Label sub = new Label("Export all MongoDB data (products, sales, customers, users, etc.) to a JSON file, or restore a shop from a backup file.");
        sub.getStyleClass().add("sub-label");
        sub.setWrapText(true);

        VBox exportCard = new VBox(10);
        exportCard.getStyleClass().add("card");

        Label exportTitle = new Label("Create Backup");
        exportTitle.getStyleClass().add("section-title");

        Label exportDesc = new Label("Saves a complete snapshot of the database to your computer.");
        exportDesc.getStyleClass().add("sub-label");

        Button exportBtn = new Button("💾  Export Backup to File");
        exportBtn.getStyleClass().add("btn-primary");
        exportBtn.setMaxWidth(Double.MAX_VALUE);
        exportBtn.setOnAction(e -> exportBackup());

        exportCard.getChildren().addAll(exportTitle, exportDesc, exportBtn);

        VBox restoreCard = new VBox(10);
        restoreCard.getStyleClass().add("card");

        Label restoreTitle = new Label("Restore From Backup");
        restoreTitle.getStyleClass().add("section-title");

        Label restoreDesc = new Label("⚠️ This will replace ALL current data with the contents of the selected backup file. This action cannot be undone.");
        restoreDesc.getStyleClass().add("sub-label");
        restoreDesc.setWrapText(true);

        Button restoreBtn = new Button("↩️  Restore Database from File");
        restoreBtn.getStyleClass().add("btn-danger");
        restoreBtn.setMaxWidth(Double.MAX_VALUE);
        restoreBtn.setOnAction(e -> restoreBackup());

        restoreCard.getChildren().addAll(restoreTitle, restoreDesc, restoreBtn);

        statusLabel.getStyleClass().add("form-label");
        statusLabel.setWrapText(true);

        root.getChildren().addAll(title, sub, exportCard, restoreCard, statusLabel);
        return root;
    }

    private void exportBackup() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose Backup Destination");
        File dir = chooser.showDialog(null);
        if (dir == null) return;
        try {
            String path = BackupUtil.exportDatabase(dir.toPath());
            statusLabel.setText("✅ Backup saved to: " + path);
            statusLabel.setStyle("-fx-text-fill: #1e8449;");
            logDAO.log("BACKUP", "Database exported to " + path);
        } catch (Exception ex) {
            statusLabel.setText("❌ Backup failed: " + ex.getMessage());
            statusLabel.setStyle("-fx-text-fill: #c0392b;");
        }
    }

    private void restoreBackup() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Backup File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Backup", "*.json"));
        File file = chooser.showOpenDialog(null);
        if (file == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Restore from " + file.getName() + "?\nALL current data will be replaced. Continue?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(resp -> {
            if (resp != ButtonType.YES) return;
            try {
                int count = BackupUtil.restoreDatabase(file.toPath());
                statusLabel.setText("✅ Restore complete: " + count + " records loaded from " + file.getName());
                statusLabel.setStyle("-fx-text-fill: #1e8449;");
                logDAO.log("RESTORE", "Database restored from " + file.getName() + " (" + count + " records)");
            } catch (Exception ex) {
                statusLabel.setText("❌ Restore failed: " + ex.getMessage());
                statusLabel.setStyle("-fx-text-fill: #c0392b;");
            }
        });
    }
}
