package com.ecommerce.warehouseservice.model;

import java.io.Serializable;

public record ShipmentMessage(
        String transactionId,
        String sessionId,
        String trackingCode,
        String customerAddress
) implements Serializable {
}