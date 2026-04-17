package com.ecommerce.warehouseservice.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(
        name = "warehouse.sync-on-startup",
        havingValue = "true",
        matchIfMissing = true)
public class WarehouseInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(WarehouseInitializer.class);
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
            log.info("Syncing initial products from catalogue...");

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    CATALOGUE_URL,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            List<Map<String, Object>> products = response.getBody();
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
                                inventoryService.putProduct(productId, stock);
                                log.info("Synced product {} with stock: {}", productId, stock);
                                count++;
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error syncing product: {}", e.getMessage());
                    }
                }
                log.info("Synced {} new products from catalogue", count);
            } else {
                log.info("No products found in catalogue or empty response");
            }
        } catch (RestClientException e) {
            log.error("Failed to sync products from catalogue: {}", e.getMessage());
            log.error("Proceeding with existing inventory");
        }
    }
}

