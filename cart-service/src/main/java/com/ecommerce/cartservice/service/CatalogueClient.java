package com.ecommerce.cartservice.service;

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
            
            System.out.println("[CatalogueClient] DECREMENT - URL: " + url);
            System.out.println("[CatalogueClient] DECREMENT - Request: " + request);
            System.out.println("[CatalogueClient] DECREMENT - Sending PATCH request...");
            
            var response = restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );
            
            System.out.println("[CatalogueClient] DECREMENT - Status: " + response.getStatusCode());
            System.out.println("[CatalogueClient] DECREMENT - Response body: " + response.getBody());
            
            boolean success = response.getStatusCode().is2xxSuccessful();
            System.out.println("[CatalogueClient] DECREMENT - Success: " + success);
            
            if (!success) {
                System.err.println("[CatalogueClient] ERROR: Decrement returned non-2xx status: " + response.getStatusCode());
            }
            return success;
        } catch (Exception e) {
            System.err.println("[CatalogueClient] EXCEPTION during decrementStock for product " + productId);
            System.err.println("[CatalogueClient] Exception type: " + e.getClass().getName());
            System.err.println("[CatalogueClient] Exception message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}

