package com.ecommerce.paymentservice.model;

import java.io.Serializable;

public class OrderMessage implements Serializable{
    private String transactionId;
    private String sessionId;
    private double totalAmount;
    private String customerAddress;
    public OrderMessage(){

    }

    public OrderMessage(String transactionId, String sessionId, double totalAmount, String customerAddress){
        this.transactionId = transactionId;
        this.sessionId = sessionId;
        this.totalAmount = totalAmount;
        this.customerAddress = customerAddress;
    }

    public String getTransactionId(){ 
        return transactionId;
    }

    public void setTransactionId(String transactionId){ 
        this.transactionId = transactionId;
    }

    public String getSessionId(){ 
        return sessionId;
    }

    public void setSessionId(String sessionId){ 
        this.sessionId = sessionId;
    }

    public double getTotalAmount(){ 
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount){ 
        this.totalAmount = totalAmount;
    }

    public String getCustomerAddress(){ 
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress){ 
        this.customerAddress = customerAddress;
    }
}
