package com.shop.view;

import com.shop.dao.SettingDAO;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
public class SettingsView {
    private final SettingDAO settingDAO = new SettingDAO();

    private final TextField storeNameField = new TextField();
    private final TextField ownerField = new TextField();
    private final TextField addressField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField emailField = new TextField();
    private final TextField gstinField = new TextField();
    private final TextField currencyField = new TextField();
    private final TextField taxField = new TextField();
    private final TextField lowStockField = new TextField();
    private final TextArea footerArea = new TextArea();

    public Node getView() {
        loadSettings();

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label title = new Label("⚙️ Store Settings");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Configure your store profile, tax and receipt defaults.");
        subtitle.getStyleClass().add("sub-label");
        headerCard.getChildren().addAll(title, subtitle);

        VBox settingsCard = new VBox(14);
        settingsCard.getStyleClass().add("card");

        Label storeTitle = new Label("🏪 Store Profile");
        storeTitle.getStyleClass().add("section-title");

        storeNameField.setPromptText("Store name");
        ownerField.setPromptText("Owner name");
        addressField.setPromptText("Address");
        phoneField.setPromptText("Phone");
        emailField.setPromptText("Email");
        gstinField.setPromptText("GSTIN / Tax ID");

        VBox col1 = new VBox(8, fieldBox("Store Name", storeNameField),
                fieldBox("Owner", ownerField), fieldBox("Address", addressField));
        col1.setPrefWidth(400);
        VBox col2 = new VBox(8, fieldBox("Phone", phoneField),
                fieldBox("Email", emailField), fieldBox("GSTIN / Tax ID", gstinField));
        col2.setPrefWidth(400);

        HBox profileRow = new HBox(24, col1, col2);
        profileRow.setMaxWidth(Double.MAX_VALUE);

        Label billingTitle = new Label("🧾 Billing & Receipts");
        billingTitle.getStyleClass().add("section-title");

        currencyField.setPromptText("Currency symbol (e.g. ₹)");
        currencyField.setPrefWidth(160);

        taxField.setPromptText("Default tax %");
        taxField.setPrefWidth(160);

        lowStockField.setPromptText("Low stock alert level");
        lowStockField.setPrefWidth(160);

        HBox billingRow = new HBox(16, fieldBox("Currency", currencyField),
                fieldBox("Default Tax %", taxField), fieldBox("Low Stock Alert Level", lowStockField));

        footerArea.setPromptText("Footer note printed on receipts (e.g. 'Thank you for shopping!')");
        footerArea.setPrefRowCount(3);

        settingsCard.getChildren().addAll(storeTitle, profileRow, billingTitle, billingRow, fieldBox("Receipt Footer", footerArea));

        Button saveBtn = new Button("💾  Save Settings");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> saveSettings());

        root.getChildren().addAll(headerCard, settingsCard, saveBtn);
        scrollPane.setContent(root);
        return scrollPane;
    }

    private VBox fieldBox(String labelText, Region field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("sub-label");
        field.setMaxWidth(Double.MAX_VALUE);
        VBox box = new VBox(4, label, field);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private void loadSettings() {
        storeNameField.setText(settingDAO.get(SettingDAO.KEY_STORE_NAME, "My Shop"));
        ownerField.setText(settingDAO.get(SettingDAO.KEY_OWNER, ""));
        addressField.setText(settingDAO.get(SettingDAO.KEY_ADDRESS, ""));
        phoneField.setText(settingDAO.get(SettingDAO.KEY_PHONE, ""));
        emailField.setText(settingDAO.get(SettingDAO.KEY_EMAIL, ""));
        gstinField.setText(settingDAO.get(SettingDAO.KEY_GSTIN, ""));
        currencyField.setText(settingDAO.get(SettingDAO.KEY_CURRENCY, "₹"));
        taxField.setText(settingDAO.get(SettingDAO.KEY_TAX_PERCENT, "0"));
        lowStockField.setText(settingDAO.get(SettingDAO.KEY_LOW_STOCK_ALERT, "5"));
        footerArea.setText(settingDAO.get(SettingDAO.KEY_RECEIPT_FOOTER, "Thank you for shopping with us!"));
    }

    private void saveSettings() {
        try {
            Double.parseDouble(taxField.getText().trim());
            Integer.parseInt(lowStockField.getText().trim());
        } catch (NumberFormatException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Default tax % must be a number and Low stock alert level must be a whole number.", ButtonType.OK);
            alert.setHeaderText(null);
            alert.setTitle("Invalid Value");
            alert.showAndWait();
            return;
        }

        settingDAO.set(SettingDAO.KEY_STORE_NAME, storeNameField.getText().trim());
        settingDAO.set(SettingDAO.KEY_OWNER, ownerField.getText().trim());
        settingDAO.set(SettingDAO.KEY_ADDRESS, addressField.getText().trim());
        settingDAO.set(SettingDAO.KEY_PHONE, phoneField.getText().trim());
        settingDAO.set(SettingDAO.KEY_EMAIL, emailField.getText().trim());
        settingDAO.set(SettingDAO.KEY_GSTIN, gstinField.getText().trim());
        settingDAO.set(SettingDAO.KEY_CURRENCY, currencyField.getText().trim().isEmpty() ? "₹" : currencyField.getText().trim());
        settingDAO.set(SettingDAO.KEY_TAX_PERCENT, taxField.getText().trim());
        settingDAO.set(SettingDAO.KEY_LOW_STOCK_ALERT, lowStockField.getText().trim());
        settingDAO.set(SettingDAO.KEY_RECEIPT_FOOTER, footerArea.getText().trim());

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Store settings saved successfully!", ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Settings");
        alert.showAndWait();
    }
}
