package com.shop.view;

import com.shop.dao.CustomerDAO;
import com.shop.dao.MembershipDAO;
import com.shop.model.Customer;
import com.shop.model.Membership;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.Optional;

public class MembershipsView {
    private final MembershipDAO membershipDAO = new MembershipDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final ObservableList<Membership> membershipList = FXCollections.observableArrayList();
    private final TableView<Membership> table = new TableView<>();
    private final Label statsLabel = new Label();

    private final ComboBox<Customer> customerCombo = new ComboBox<>();
    private final ComboBox<String> tierCombo = new ComboBox<>();

    public Node getView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(10));

        VBox headerCard = new VBox(6);
        headerCard.getStyleClass().add("card");
        Label title = new Label("⭐ Membership & Loyalty Program");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Reward repeat customers with tier-based benefits and loyalty points.");
        subtitle.getStyleClass().add("sub-label");
        statsLabel.getStyleClass().add("sub-label");
        headerCard.getChildren().addAll(title, subtitle, statsLabel);

        VBox enrollCard = new VBox(12);
        enrollCard.getStyleClass().add("card");
        Label enrollTitle = new Label("➕ Enroll a New Member");
        enrollTitle.getStyleClass().add("section-title");

        HBox enrollRow = new HBox(12);
        enrollRow.setAlignment(Pos.CENTER_LEFT);

        customerCombo.setPromptText("Select customer");
        customerCombo.setPrefWidth(280);
        customerCombo.getItems().addAll(customerDAO.findAll());

        tierCombo.getItems().addAll(Membership.TIER_SILVER, Membership.TIER_GOLD, Membership.TIER_PLATINUM);
        tierCombo.setValue(Membership.TIER_SILVER);
        tierCombo.setPrefWidth(140);

        Button enrollBtn = new Button("⭐  Enroll Member");
        enrollBtn.getStyleClass().add("btn-primary");
        enrollBtn.setOnAction(e -> enroll());

        enrollRow.getChildren().addAll(customerCombo, tierCombo, enrollBtn);
        enrollCard.getChildren().addAll(enrollTitle, enrollRow);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Membership, String> numCol = new TableColumn<>("Membership No.");
        numCol.setCellValueFactory(new PropertyValueFactory<>("membershipNumber"));

        TableColumn<Membership, String> custCol = new TableColumn<>("Customer");
        custCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getCustomerName() != null
                ? p.getValue().getCustomerName() : "—"));

        TableColumn<Membership, String> tierCol = new TableColumn<>("Tier");
        tierCol.setCellValueFactory(new PropertyValueFactory<>("tierLabel"));
        tierCol.setCellFactory(col -> new TableCell<Membership, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add(switch (item) {
                        case Membership.TIER_PLATINUM -> "badge-active";
                        case Membership.TIER_GOLD -> "badge-warning";
                        default -> "badge-inactive";
                    });
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Membership, Integer> pointsCol = new TableColumn<>("Points");
        pointsCol.setCellValueFactory(new PropertyValueFactory<>("points"));

        TableColumn<Membership, String> spentCol = new TableColumn<>("Total Spent");
        spentCol.setCellValueFactory(p -> new SimpleStringProperty(String.format("₹%.2f", p.getValue().getTotalSpent())));

        TableColumn<Membership, String> discountCol = new TableColumn<>("Discount");
        discountCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getDiscountPercent() + "%"));

        TableColumn<Membership, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<Membership, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add(Membership.STATUS_ACTIVE.equals(item) ? "badge-active" : "badge-inactive");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Membership, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(280);
        actionCol.setCellFactory(col -> new TableCell<Membership, Void>() {
            private final Button pointsBtn = new Button("⭐ Points");
            private final Button tierBtn = new Button("🔁 Change Tier");
            private final Button deactivateBtn = new Button("🚫 Expire");
            private final HBox box = new HBox(6, pointsBtn, tierBtn, deactivateBtn);
            {
                pointsBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                tierBtn.getStyleClass().addAll("btn-secondary", "btn-small");
                deactivateBtn.getStyleClass().addAll("btn-danger", "btn-small");
                pointsBtn.setOnAction(e -> managePoints(getTableRow().getItem()));
                tierBtn.setOnAction(e -> changeTier(getTableRow().getItem()));
                deactivateBtn.setOnAction(e -> deactivate(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    boolean active = getTableRow().getItem().isActive();
                    pointsBtn.setDisable(!active);
                    tierBtn.setDisable(!active);
                    deactivateBtn.setDisable(!active);
                    setGraphic(box);
                }
            }
        });

        table.getColumns().addAll(numCol, custCol, tierCol, pointsCol, spentCol, discountCol, statusCol, actionCol);
        table.setItems(membershipList);
        table.setPlaceholder(new Label("No memberships yet. Enroll your first member!"));

        root.getChildren().addAll(headerCard, enrollCard, table);
        refresh();
        return root;
    }

    private void enroll() {
        Customer c = customerCombo.getValue();
        if (c == null) {
            showAlert("Please select a customer to enroll.");
            return;
        }
        if (membershipDAO.findByCustomerId(c.getId()) != null) {
            showAlert(c.getName() + " is already a member.");
            return;
        }
        Membership m = new Membership();
        m.setCustomerId(c.getId());
        m.setCustomerName(c.getName());
        m.setTier(tierCombo.getValue());
        if (membershipDAO.create(m)) {
            showAlert(c.getName() + " enrolled as " + m.getTier() + " member!\n\n"
                    + "Membership No: " + m.getMembershipNumber() + "\n"
                    + "Standard discount: " + m.getDiscountPercent() + "%");
            customerCombo.setValue(null);
            refresh();
        } else {
            showAlert("Failed to enroll member. Please try again.");
        }
    }

    private void managePoints(Membership m) {
        if (m == null || !m.isActive()) return;
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Add Points", "Add Points", "Redeem Points");
        dialog.setTitle("Manage Loyalty Points");
        dialog.setHeaderText(m.getCustomerName() + " — current points: " + m.getPoints());
        dialog.setContentText("Action:");
        Optional<String> choice = dialog.showAndWait();
        choice.ifPresent(action -> {
            TextInputDialog input = new TextInputDialog();
            input.setTitle("Loyalty Points");
            input.setHeaderText(action + " for " + m.getCustomerName());
            input.setContentText("Number of points:");
            Optional<String> result = input.showAndWait();
            result.ifPresent(val -> {
                try {
                    int points = Integer.parseInt(val.trim());
                    boolean ok = "Add Points".equals(action)
                            ? membershipDAO.addPoints(m.getId(), points)
                            : membershipDAO.redeemPoints(m.getId(), points);
                    if (ok) {
                        refresh();
                    } else {
                        showAlert("Could not " + ("Add Points".equals(action) ? "add" : "redeem")
                                + " points. Check the amount and balance.");
                    }
                } catch (NumberFormatException ex) {
                    showAlert("Please enter a valid number of points.");
                }
            });
        });
    }

    private void changeTier(Membership m) {
        if (m == null || !m.isActive()) return;
        ChoiceDialog<String> dialog = new ChoiceDialog<>(m.getTier(),
                Membership.TIER_SILVER, Membership.TIER_GOLD, Membership.TIER_PLATINUM);
        dialog.setTitle("Change Membership Tier");
        dialog.setHeaderText(m.getCustomerName());
        dialog.setContentText("New tier:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(tier -> {
            if (membershipDAO.changeTier(m.getId(), tier)) {
                refresh();
            } else {
                showAlert("Failed to change tier.");
            }
        });
    }

    private void deactivate(Membership m) {
        if (m == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Expire Membership");
        confirm.setHeaderText(null);
        confirm.setContentText("Expire membership of " + m.getCustomerName() + "? They will lose tier benefits.");
        if (confirm.showAndWait().filter(b -> b == ButtonType.OK).isPresent()) {
            membershipDAO.deactivate(m.getId());
            refresh();
        }
    }

    private void refresh() {
        membershipList.clear();
        membershipList.addAll(membershipDAO.findAll());
        statsLabel.setText("Active Members: " + membershipDAO.countActive()
                + "  •  Total Members: " + membershipDAO.count()
                + "  •  Points in Circulation: " + membershipDAO.totalPoints());
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Membership");
        alert.showAndWait();
    }
}
