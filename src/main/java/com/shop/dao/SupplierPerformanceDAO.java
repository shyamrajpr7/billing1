package com.shop.dao;

import com.shop.model.PurchaseOrder;
import com.shop.model.Supplier;
import com.shop.model.SupplierPerformance;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SupplierPerformanceDAO {

    private final PurchaseOrderDAO poDAO = new PurchaseOrderDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();

    public List<SupplierPerformance> getStats() {
        Map<Integer, SupplierPerformance> map = new HashMap<>();

        for (Supplier s : supplierDAO.findAll()) {
            SupplierPerformance sp = new SupplierPerformance();
            sp.setSupplierId(s.getId());
            sp.setSupplierName(s.getCompanyName());
            sp.setContactPerson(s.getContactPerson());
            sp.setPhone(s.getPhone());
            map.put(s.getId(), sp);
        }

        for (PurchaseOrder po : poDAO.findAll()) {
            SupplierPerformance sp = map.get(po.getSupplierId());
            if (sp == null) {
                sp = new SupplierPerformance();
                sp.setSupplierId(po.getSupplierId());
                sp.setSupplierName(po.getSupplierName());
                map.put(po.getSupplierId(), sp);
            }

            sp.setTotalOrders(sp.getTotalOrders() + 1);
            sp.setTotalSpend(sp.getTotalSpend() + po.getTotalCost());
            sp.setItemsReceived(sp.getItemsReceived() + po.getItemCount());

            String status = po.getStatus();
            if (PurchaseOrder.STATUS_RECEIVED.equals(status)) {
                sp.setReceivedOrders(sp.getReceivedOrders() + 1);
                if (po.getReceivedAt() != null && po.getCreatedAt() != null) {
                    double days = Duration.between(po.getCreatedAt(), po.getReceivedAt()).toHours() / 24.0;
                    sp.setAvgDaysToReceive(sp.getAvgDaysToReceive() + days);
                }
            } else if (PurchaseOrder.STATUS_PENDING.equals(status)) {
                sp.setPendingOrders(sp.getPendingOrders() + 1);
            } else if (PurchaseOrder.STATUS_CANCELLED.equals(status)) {
                sp.setCancelledOrders(sp.getCancelledOrders() + 1);
            }

            String created = po.getCreatedAtLabel().split(" ")[0];
            if (sp.getLastOrderDate() == null || created.compareTo(sp.getLastOrderDate()) > 0) {
                sp.setLastOrderDate(created);
            }
        }

        for (SupplierPerformance sp : map.values()) {
            if (sp.getReceivedOrders() > 0) {
                sp.setAvgDaysToReceive(sp.getAvgDaysToReceive() / sp.getReceivedOrders());
            }
        }

        List<SupplierPerformance> list = new ArrayList<>(map.values());
        list.sort((a, b) -> Double.compare(b.getTotalSpend(), a.getTotalSpend()));
        return list;
    }
}
