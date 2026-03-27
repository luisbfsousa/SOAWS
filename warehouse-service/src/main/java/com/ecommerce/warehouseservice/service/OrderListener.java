package com.ecommerce.warehouseservice.service;

import com.ecommerce.warehouseservice.model.ShipmentMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class OrderListener {
    public static final String ORDER_QUEUE = "order.confirmed";
    public static final String SHIPMENT_QUEUE = "order.shipped";
    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;

    public OrderListener(JmsTemplate jmsTemplate, ObjectMapper objectMapper) {
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
    }

    @JmsListener(destination = ORDER_QUEUE)
    public void onOrderReceived(String rawMessage) {
        try {
            Map<String, Object> order = objectMapper.readValue(rawMessage, Map.class);
            String transactionId = (String) order.get("transactionId");
            String sessionId = (String) order.get("sessionId");
            String customerAddress = (String) order.getOrDefault("customerAddress", "Unknown");

            System.out.println("[WarehouseService] Received order for transaction: " + transactionId);
            System.out.println("[WarehouseService] Preparing shipment to: " + customerAddress);
            String trackingCode = "TRACK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            ShipmentMessage shipment = new ShipmentMessage(transactionId, sessionId, trackingCode, customerAddress);
            jmsTemplate.convertAndSend(SHIPMENT_QUEUE, shipment);
            System.out.println("[WarehouseService] Order shipped. Tracking: " + trackingCode);
        } catch (Exception e) {
            System.err.println("[WarehouseService] Failed to process order message: " + e.getMessage());
        }
    }
}
