package com.shop.model;

public class ShiftReport {
    private int shiftId;
    private String cashierName;
    private String openedAtLabel;
    private String closedAtLabel;
    private int salesCount;
    private double cashSales;
    private double cardSales;
    private double upiSales;
    private double netBankingSales;
    private double creditSales;
    private double giftCardAmount;
    private double cashRefunds;
    private double cashExpenses;
    private double openingBalance;
    private double expectedCash;
    private double countedCash;
    private double variance;

    public double getTotalSales() {
        return cashSales + cardSales + upiSales + netBankingSales + creditSales + giftCardAmount;
    }

    public double getExpectedCash() { return expectedCash; }
    public void setExpectedCash(double expectedCash) { this.expectedCash = expectedCash; }

    public int getShiftId() { return shiftId; }
    public void setShiftId(int shiftId) { this.shiftId = shiftId; }

    public String getCashierName() { return cashierName; }
    public void setCashierName(String cashierName) { this.cashierName = cashierName; }

    public String getOpenedAtLabel() { return openedAtLabel; }
    public void setOpenedAtLabel(String openedAtLabel) { this.openedAtLabel = openedAtLabel; }

    public String getClosedAtLabel() { return closedAtLabel; }
    public void setClosedAtLabel(String closedAtLabel) { this.closedAtLabel = closedAtLabel; }

    public int getSalesCount() { return salesCount; }
    public void setSalesCount(int salesCount) { this.salesCount = salesCount; }

    public double getCashSales() { return cashSales; }
    public void setCashSales(double cashSales) { this.cashSales = cashSales; }

    public double getCardSales() { return cardSales; }
    public void setCardSales(double cardSales) { this.cardSales = cardSales; }

    public double getUpiSales() { return upiSales; }
    public void setUpiSales(double upiSales) { this.upiSales = upiSales; }

    public double getNetBankingSales() { return netBankingSales; }
    public void setNetBankingSales(double netBankingSales) { this.netBankingSales = netBankingSales; }

    public double getCreditSales() { return creditSales; }
    public void setCreditSales(double creditSales) { this.creditSales = creditSales; }

    public double getGiftCardAmount() { return giftCardAmount; }
    public void setGiftCardAmount(double giftCardAmount) { this.giftCardAmount = giftCardAmount; }

    public double getCashRefunds() { return cashRefunds; }
    public void setCashRefunds(double cashRefunds) { this.cashRefunds = cashRefunds; }

    public double getCashExpenses() { return cashExpenses; }
    public void setCashExpenses(double cashExpenses) { this.cashExpenses = cashExpenses; }

    public double getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(double openingBalance) { this.openingBalance = openingBalance; }

    public double getCountedCash() { return countedCash; }
    public void setCountedCash(double countedCash) { this.countedCash = countedCash; }

    public double getVariance() { return variance; }
    public void setVariance(double variance) { this.variance = variance; }
}
