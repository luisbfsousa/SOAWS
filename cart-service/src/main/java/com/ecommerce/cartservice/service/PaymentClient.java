package com.ecommerce.cartservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class PaymentClient {

    private final RestTemplate restTemplate;
    private final String paymentBaseUrl;

    public PaymentClient(RestTemplate restTemplate,
                         @Value("${payment.service.url}") String paymentBaseUrl) {
        this.restTemplate = restTemplate;
        this.paymentBaseUrl = paymentBaseUrl;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> processPayment(String sessionId, double amount,
                                              String paymentMethod, String customerAddress) {
        Map<String, Object> request = Map.of(
                "sessionId", sessionId,
                "amount", amount,
                "paymentMethod", paymentMethod,
                "customerAddress", customerAddress
        );
        try {
            return restTemplate.postForObject(paymentBaseUrl + "/payments", request, Map.class);
        } catch (Exception e) {
            System.err.println("[CartService] Payment Service unreachable: " + e.getMessage());
            return Map.of("success", false, "error", "Payment service unavailable");
        }
    }
}
