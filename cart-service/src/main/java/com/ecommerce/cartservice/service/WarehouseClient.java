package com.ecommerce.cartservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class WarehouseClient {

    private final RestTemplate restTemplate;
    private final String warehouseBaseUrl;

    public WarehouseClient(RestTemplate restTemplate,
                           @Value("${warehouse.service.url}") String warehouseBaseUrl) {
        this.restTemplate = restTemplate;
        this.warehouseBaseUrl = warehouseBaseUrl;
    }

    public boolean decrementStock(Long productId, int quantity) {
        try {
            Map<String, Integer> request = Map.of("quantity", -quantity);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Integer>> entity = new HttpEntity<>(request, headers);
            var response = restTemplate.exchange(
                    warehouseBaseUrl + "/inventory/" + productId,
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );
            System.out.println("[CartService] Decremented warehouse stock for product " + productId + ". Status: " + response.getStatusCode() + ", Response: " + response.getBody());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.err.println("[CartService] Failed to decrement warehouse stock for product " + productId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets the current stock for a product in the warehouse.
     * Returns -1 if product not found or on error.
     */
    @SuppressWarnings("unchecked")
    public int getWarehouseStock(Long productId) {
        try {
            Map<String, Object> response = restTemplate.getForObject(
                    warehouseBaseUrl + "/inventory/" + productId,
                    Map.class
            );
            if (response != null) {
                Object stock = response.get("stock");
                if (stock instanceof Number) {
                    return ((Number) stock).intValue();
                }
            }
            return -1;
        } catch (Exception e) {
            System.err.println("[CartService] Failed to get warehouse stock for product " + productId + ": " + e.getMessage());
            return -1;
        }
    }
}
