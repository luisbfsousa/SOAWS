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
        try {
            Map<String, Integer> request = Map.of("delta", -quantity);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Integer>> entity = new HttpEntity<>(request, headers);
            var response = restTemplate.exchange(
                    catalogueBaseUrl + "/products/" + productId + "/stock",
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );
            System.out.println("[CartService] Decremented catalogue stock for product " + productId + ". Status: " + response.getStatusCode() + ", Response: " + response.getBody());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.err.println("[CartService] Failed to decrement catalogue stock for product " + productId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
