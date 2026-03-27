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
        String url = warehouseBaseUrl + "/warehouse/inventory/" + productId;
        try {
            Map<String, Integer> request = Map.of("quantity", -quantity);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Integer>> entity = new HttpEntity<>(request, headers);

            System.out.println("[WarehouseClient] DECREMENT - URL: " + url);
            System.out.println("[WarehouseClient] DECREMENT - Request: " + request);
            System.out.println("[WarehouseClient] DECREMENT - Sending PATCH request...");

            var response = restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );

            System.out.println("[WarehouseClient] DECREMENT - Status: " + response.getStatusCode());
            System.out.println("[WarehouseClient] DECREMENT - Response body: " + response.getBody());

            boolean success = response.getStatusCode().is2xxSuccessful();
            System.out.println("[WarehouseClient] DECREMENT - Success: " + success);

            if (!success) {
                System.err.println("[WarehouseClient] ERROR: Decrement returned non-2xx status: " + response.getStatusCode());
            }
            return success;
        } catch (Exception e) {
            System.err.println("[WarehouseClient] EXCEPTION during decrementStock for product " + productId);
            System.err.println("[WarehouseClient] Exception type: " + e.getClass().getName());
            System.err.println("[WarehouseClient] Exception message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public int getWarehouseStock(Long productId) {
        try {
            String url = warehouseBaseUrl + "/warehouse/inventory/" + productId;
            System.out.println("[CartService] Fetching warehouse stock from: " + url);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            System.out.println("[CartService] Warehouse response for product " + productId + ": " + response);

            if (response != null) {
                Object stock = response.get("stock");
                if (stock instanceof Number) {
                    int stockValue = ((Number) stock).intValue();
                    System.out.println("[CartService] Product " + productId + " warehouse stock: " + stockValue);
                    return stockValue;
                }
            }
            System.err.println("[CartService] Invalid warehouse response for product " + productId + ": missing 'stock' field");
            return -1;
        } catch (Exception e) {
            System.err.println("[CartService] Failed to get warehouse stock for product " + productId + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
}
