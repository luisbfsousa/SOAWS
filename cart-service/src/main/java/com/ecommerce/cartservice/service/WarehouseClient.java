package com.ecommerce.cartservice.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(WarehouseClient.class);
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

            log.info("DECREMENT - URL: {}", url);
            log.info("DECREMENT - Request: {}", request);
            log.info("DECREMENT - Sending PATCH request...");

            var response = restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );

            log.info("DECREMENT - Status: {}", response.getStatusCode());
            log.info("DECREMENT - Response body: {}", response.getBody());

            boolean success = response.getStatusCode().is2xxSuccessful();
            log.info("DECREMENT - Success: {}", success);

            if (!success) {
                log.error("ERROR: Decrement returned non-2xx status: {}", response.getStatusCode());
            }
            return success;
        } catch (Exception e) {
            log.error("EXCEPTION during decrementStock for product {}", productId);
            log.error("Exception type: {}", e.getClass().getName());
            log.error("Exception message: {}", e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public int getWarehouseStock(Long productId) {
        try {
            String url = warehouseBaseUrl + "/warehouse/inventory/" + productId;
            log.info("Fetching warehouse stock from: {}", url);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            log.info("Warehouse response for product {}: {}", productId, response);

            if (response != null) {
                Object stock = response.get("stock");
                if (stock instanceof Number) {
                    int stockValue = ((Number) stock).intValue();
                    log.info("Product {} warehouse stock: {}", productId, stockValue);
                    return stockValue;
                }
            }
            log.error("Invalid warehouse response for product {}: missing 'stock' field", productId);
            return -1;
        } catch (Exception e) {
            log.error("Failed to get warehouse stock for product {}: {} - {}", productId, e.getClass().getSimpleName(), e.getMessage());
            return -1;
        }
    }
}
