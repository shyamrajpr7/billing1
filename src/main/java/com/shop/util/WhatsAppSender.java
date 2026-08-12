package com.shop.util;

import com.shop.model.Sale;
import com.shop.model.SaleItem;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Sends e-bills to customers on WhatsApp by opening the official wa.me
 * deep link with the bill pre-filled in the message box. No external API
 * keys or backend are required — the cashier just presses Send.
 */
public class WhatsAppSender {

    private WhatsAppSender() {
    }

    public static boolean isSupported() {
        return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
    }

    /**
     * Builds a plain-text e-bill for a sale, suitable for WhatsApp.
     */
    public static String buildEBillText(Sale sale, String customerName) {
        String line = "--------------------------------";
        StringBuilder sb = new StringBuilder();
        sb.append("🛍️ SUPER STORE RETAIL\n");
        sb.append("123 Main Street, Commerce City\n");
        sb.append("Phone: +91 98765 43210\n");
        sb.append(line).append('\n');
        sb.append("Invoice : ").append(sale.getInvoiceNumber()).append('\n');
        sb.append("Date    : ").append(sale.getFormattedDate()).append('\n');
        sb.append("Customer: ").append(customerName == null ? "Walk-in" : customerName).append('\n');
        sb.append("Payment : ").append(sale.getPaymentMethod()).append('\n');
        sb.append(line).append('\n');
        for (SaleItem item : sale.getItems()) {
            sb.append(item.getProductName()).append(" x").append(item.getQuantity())
                    .append("   ").append(String.format("₹%.2f", item.getTotal())).append('\n');
        }
        sb.append(line).append('\n');
        sb.append(String.format("Subtotal    : ₹%.2f\n", sale.getSubtotal()));
        if (sale.getDiscountAmount() > 0) {
            sb.append(String.format("Discount    : -₹%.2f\n", sale.getDiscountAmount()));
        }
        if (sale.getPointsRedeemed() > 0) {
            sb.append(String.format("Loyalty     : -₹%.2f (%d pts)\n", (double) sale.getPointsRedeemed(), sale.getPointsRedeemed()));
        }
        if (sale.getGiftCardAmount() > 0) {
            sb.append(String.format("Gift Card   : -₹%.2f\n", sale.getGiftCardAmount()));
        }
        sb.append(String.format("Tax (5%%)    : ₹%.2f\n", sale.getTax()));
        sb.append(String.format("TOTAL       : ₹%.2f\n", sale.getTotal()));
        sb.append(line).append('\n');
        sb.append("Thank you for shopping with us! 🎉\n");
        return sb.toString();
    }

    /**
     * Normalises a phone number to the international format required by
     * wa.me (no '+'). 10-digit numbers are treated as Indian and prefixed
     * with 91. Leading '0' is replaced with 91.
     */
    public static String normalizePhone(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;
        if (digits.startsWith("91") && digits.length() == 12) {
            return digits;
        }
        if (digits.startsWith("0") && digits.length() == 11) {
            return "91" + digits.substring(1);
        }
        if (digits.length() == 10) {
            return "91" + digits;
        }
        return digits;
    }

    /**
     * Opens WhatsApp with the e-bill pre-filled for the given phone.
     *
     * @return true if the browser/WhatsApp was opened successfully
     */
    public static boolean sendEBill(String phone, Sale sale, String customerName) {
        String normalized = normalizePhone(phone);
        if (normalized == null || normalized.length() < 10) return false;
        if (!isSupported()) return false;
        try {
            String text = URLEncoder.encode(buildEBillText(sale, customerName), StandardCharsets.UTF_8);
            URI uri = URI.create("https://wa.me/" + normalized + "?text=" + text);
            Desktop.getDesktop().browse(uri);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
