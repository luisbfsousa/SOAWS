package com.ecommerce.warehouseservice.model;

import java.io.Serializable;

public class ShipmentMessage implements Serializable {
    private String transactionId;
    private String sessionId;
    private String trackingCode;
    private String customerAddress;

    public ShipmentMessage(String transactionId, String sessionId, String trackingCode, String customerAddress) {
        this.transactionId = transactionId;
        this.sessionId = sessionId;
        this.trackingCode = trackingCode;
        this.customerAddress = customerAddress;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String t) {
        this.transactionId = t;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String s) {
        this.sessionId = s;
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public void setTrackingCode(String t) {
        this.trackingCode = t;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String a) {
        this.customerAddress = a;
    }
}
