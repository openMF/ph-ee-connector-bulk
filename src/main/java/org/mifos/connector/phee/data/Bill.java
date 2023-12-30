package org.mifos.connector.phee.data;

public class Bill {
    public String getBillerName() {
        return billerName;
    }

    public void setBillerName(String billerName) {
        this.billerName = billerName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Bill(String billerName, double amount) {
        this.billerName = billerName;
        this.amount = amount;
    }

    private String billerName;
    private double amount;
}
