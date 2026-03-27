package com.ecommerce.catalogueservice.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.springframework.http.HttpMethod.*;

@Service
public class WarehouseService {
    private static final Logger log = LoggerFactory.getLogger(WarehouseService.class);
    private final RestTemplate restTemplate;
    private static final String WAREHOUSE_URL = "http://localhost:8086/warehouse/inventory";

    public WarehouseService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void syncProductToWarehouse(Long productId, int stockQuantity) {
        if (productId == null) {
            log.error("Cannot sync product: productId is null");
            return;
        }

        try {
            Map<String, Object> payload = Map.of(
                    "productId", productId,
                    "stockQuantity", stockQuantity
            );

            log.info("Syncing product {} to warehouse with stock: {}", productId, stockQuantity);

            HttpEntity<Object> requestEntity = new HttpEntity<>(payload);
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    WAREHOUSE_URL,
                    POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            Map<String, Object> response = responseEntity.getBody();

            log.info("Successfully synced product {} to warehouse. Response: {}", productId, response);
        } catch (RestClientException e) {
            log.error("Failed to sync product {} to warehouse: {}", productId, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error syncing product {}: {}", productId, e.getMessage());
        }
    }

    public Integer getStockFromWarehouse(Long productId) {
        if (productId == null) {
            log.error("Cannot fetch stock: productId is null");
            return null;
        }

        try {
            String url = WAREHOUSE_URL + "/" + productId;

            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            Map<String, Object> response = responseEntity.getBody();

            if (response != null && response.containsKey("stock")) {
                Object stockObj = response.get("stock");
                if (stockObj instanceof Number) {
                    return ((Number) stockObj).intValue();
                }
            }
        } catch (RestClientException e) {
            log.error("Failed to fetch stock for product {} from warehouse: {}", productId, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error fetching stock for product {}: {}", productId, e.getMessage());
        }
        return null;
    }
}
