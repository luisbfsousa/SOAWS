package com.ecommerce.warehouseservice.service;


import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ecommerce.warehouseservice.model.ShipmentMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class OrderListener {
    private static final Logger log = LoggerFactory.getLogger(OrderListener.class);
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
            Map<String, Object> order = objectMapper.readValue(
                    rawMessage,
                    new TypeReference<>() {
                    }
            );
            String transactionId = (String) order.get("transactionId");
            String sessionId = (String) order.get("sessionId");
            String customerAddress = (String) order.getOrDefault("customerAddress", "Unknown");

            log.info("Received order for transaction: {}", transactionId);
            log.info("Preparing shipment to: {}", customerAddress);
            String trackingCode = "TRACK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            ShipmentMessage shipment = new ShipmentMessage(transactionId, sessionId, trackingCode, customerAddress);
            jmsTemplate.convertAndSend(SHIPMENT_QUEUE, shipment);
            log.info("Order shipped. Tracking: {}", trackingCode);
        } catch (Exception e) {
            log.error("Failed to process order message: {}", e.getMessage());
        }
    }
}
