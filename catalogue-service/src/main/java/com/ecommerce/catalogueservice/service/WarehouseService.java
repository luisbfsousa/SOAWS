package com.ecommerce.catalogueservice.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Service
public class WarehouseService {
    private final RestTemplate restTemplate;
    private static final String WAREHOUSE_URL = "http://localhost:8086/warehouse/inventory";

    public WarehouseService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void syncProductToWarehouse(Long productId, int stockQuantity, Long warehouseId) {
        if (productId == null) {
            System.err.println("[CatalogueService] Cannot sync product: productId is null");
            return;
        }
        
        try {
            Map<String, Object> payload = Map.of(
                "productId", productId,
                "stockQuantity", stockQuantity
            );
            
            System.out.println("[CatalogueService] Syncing product " + productId + " to warehouse with stock: " + stockQuantity);
            Map<String, Object> response = restTemplate.postForObject(WAREHOUSE_URL, payload, Map.class);
            System.out.println("[CatalogueService] Successfully synced product " + productId + " to warehouse. Response: " + response);
        } catch (RestClientException e) {
            System.err.println("[CatalogueService] Failed to sync product " + productId + " to warehouse: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[CatalogueService] Unexpected error syncing product " + productId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
