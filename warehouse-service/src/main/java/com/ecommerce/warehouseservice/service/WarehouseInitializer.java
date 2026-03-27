package com.ecommerce.warehouseservice.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "warehouse.sync-on-startup", havingValue = "true", matchIfMissing = true)
public class WarehouseInitializer implements CommandLineRunner {
    private final RestTemplate restTemplate;
    private final InventoryService inventoryService;
    private static final String CATALOGUE_URL = "http://localhost:8082/products";

    public WarehouseInitializer(RestTemplate restTemplate, InventoryService inventoryService) {
        this.restTemplate = restTemplate;
        this.inventoryService = inventoryService;
    }

    @Override
    public void run(String... args) throws Exception {
        Thread.sleep(2000);
        
        try {
            System.out.println("[WarehouseService] Syncing initial products from catalogue...");
            
            List<Map<String, Object>> products = restTemplate.getForObject(CATALOGUE_URL, List.class);
            
            if (products != null && !products.isEmpty()) {
                int count = 0;
                for (Map<String, Object> product : products) {
                    try {
                        Object idObj = product.get("id");
                        Object stockObj = product.get("stockQuantity");
                        
                        if (idObj != null && stockObj != null) {
                            Long productId = ((Number) idObj).longValue();
                            int stock = ((Number) stockObj).intValue();
                            
                            if (inventoryService.getStock(productId) == null) {
                                inventoryService.addOrUpdateProduct(productId, stock);
                                System.out.println("[WarehouseService] Synced product " + productId + " with stock: " + stock);
                                count++;
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[WarehouseService] Error syncing product: " + e.getMessage());
                    }
                }
                System.out.println("[WarehouseService] Synced " + count + " new products from catalogue");
            } else {
                System.out.println("[WarehouseService] No products found in catalogue or empty response");
            }
        } catch (RestClientException e) {
            System.err.println("[WarehouseService] Failed to sync products from catalogue: " + e.getMessage());
            System.err.println("[WarehouseService] Proceeding with existing inventory");
        }
    }
}

