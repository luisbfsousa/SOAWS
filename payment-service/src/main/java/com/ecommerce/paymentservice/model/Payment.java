package com.ecommerce.paymentservice.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String transactionId;
    private String sessionId;
    private double amount;
    private String paymentMethod;
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    private LocalDateTime createdAt;
    public enum PaymentStatus {PENDING, SUCCESS, FAILED}
    public Payment() {

    }

    public Payment(String transactionId, String sessionId, double amount, String paymentMethod, PaymentStatus status){
        this.transactionId = transactionId;
        this.sessionId = sessionId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId(){ 
        return id;
    }

    public String getTransactionId(){ 
        return transactionId;
    }


    public String getSessionId(){ 
        return sessionId;
    }

    public double getAmount(){ 
        return amount;
    }

    public String getPaymentMethod(){ 
        return paymentMethod;
    }

    public PaymentStatus getStatus(){ 
        return status;
    }

    public void setStatus(PaymentStatus status){ 
        this.status = status;
    }

    public LocalDateTime getCreatedAt(){ 
        return createdAt;
    }
}
