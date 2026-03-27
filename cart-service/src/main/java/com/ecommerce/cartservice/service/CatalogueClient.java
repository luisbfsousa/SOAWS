package com.ecommerce.cartservice.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class CatalogueClient {
    private static final Logger log = LoggerFactory.getLogger(CatalogueClient.class);
private final RestTemplate restTemplate;
    private final String catalogueBaseUrl;

    public CatalogueClient(RestTemplate restTemplate,
                           @Value("${catalogue.service.url}") String catalogueBaseUrl) {
        this.restTemplate = restTemplate;
        this.catalogueBaseUrl = catalogueBaseUrl;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getProduct(Long productId) {
        try {
            return restTemplate.getForObject(catalogueBaseUrl + "/products/" + productId, Map.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }

    public boolean isProductAvailable(Long productId) {
        Map<String, Object> product = getProduct(productId);
        if (product == null) return false;
        int stock = (int) product.getOrDefault("stockQuantity", 0);
        return stock > 0;
    }

    public boolean decrementStock(Long productId, int quantity) {
        String url = catalogueBaseUrl + "/products/" + productId + "/stock";
        try {
            Map<String, Integer> request = Map.of("delta", -quantity);
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
}

