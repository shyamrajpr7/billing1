package com.shop.util;

import com.shop.dao.ProductDAO;
import com.shop.model.Product;

import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically checks inventory for low stock and expiring/expired products
 * and raises desktop tray notifications when the alert levels change.
 */
public class StockNotifier {
    private static final long INITIAL_DELAY_SECONDS = 20;
    private static final long PERIOD_SECONDS = 300;

    private static ScheduledExecutorService scheduler;
    private static TrayIcon trayIcon;
    private static boolean trayAvailable;

    private static int lastLowStock = -1;
    private static int lastExpiring = -1;
    private static int lastExpired = -1;

    private StockNotifier() {
    }

    public static synchronized void start() {
        if (scheduler != null) {
            return;
        }
        if (!createTrayIcon()) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "stock-notifier");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(StockNotifier::checkAndNotify, INITIAL_DELAY_SECONDS, PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    public static synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private static boolean createTrayIcon() {
        try {
            if (!SystemTray.isSupported()) {
                trayAvailable = false;
                return false;
            }
            SystemTray tray = SystemTray.getSystemTray();
            Image image = createIconImage();
            trayIcon = new TrayIcon(image, "Shop Management System");
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
            trayAvailable = true;
            return true;
        } catch (Exception e) {
            trayAvailable = false;
            return false;
        }
    }

    private static Image createIconImage() {
        try {
            int size = 32;
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(size, size,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(22, 199, 154));
            g.fillOval(2, 2, size - 4, size - 4);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2f));
            g.drawArc(13, 6, 7, 7, 0, 180);
            g.fillRoundRect(9, 11, 15, 15, 3, 3);
            g.setColor(new Color(22, 199, 154));
            g.fillRoundRect(13, 14, 7, 9, 2, 2);
            g.dispose();
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    private static void checkAndNotify() {
        try {
            ProductDAO dao = new ProductDAO();
            List<Product> lowStock = dao.findLowStock();
            List<Product> expiring = dao.findExpiring(7);
            List<Product> expired = dao.findExpired();

            int ls = lowStock.size();
            int exp = expiring.size();
            int exd = expired.size();

            if (lastLowStock == -1 && lastExpiring == -1 && lastExpired == -1) {
                lastLowStock = ls;
                lastExpiring = exp;
                lastExpired = exd;
                if (ls + exp + exd > 0) {
                    notifyAlert(buildSummary(ls, exp, exd), "Inventory needs attention");
                }
                return;
            }

            boolean lowChanged = ls != lastLowStock;
            boolean expChanged = exp != lastExpiring;
            boolean exdChanged = exd != lastExpired;

            if (lowChanged || expChanged || exdChanged) {
                lastLowStock = ls;
                lastExpiring = exp;
                lastExpired = exd;
                notifyAlert(buildSummary(ls, exp, exd), "Inventory alert");
            }
        } catch (Exception ignored) {
            // Never let the background notifier crash the app.
        }
    }

    private static String buildSummary(int lowStock, int expiring, int expired) {
        if (lowStock + expiring + expired == 0) {
            return "All inventory levels are healthy. 👍";
        }
        StringBuilder sb = new StringBuilder();
        if (lowStock > 0) sb.append(lowStock).append(" product(s) low on stock. ");
        if (expiring > 0) sb.append(expiring).append(" expiring within 7 days. ");
        if (expired > 0) sb.append(expired).append(" already expired!");
        return sb.toString().trim();
    }

    private static void notifyAlert(String message, String caption) {
        if (!trayAvailable || trayIcon == null) return;
        try {
            trayIcon.displayMessage(caption, message, MessageType.WARNING);
        } catch (Exception ignored) {
        }
    }
}
