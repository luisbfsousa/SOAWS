package com.ecommerce.warehouseservice.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class InventoryService {
    private final Map<Long, Integer> inventory = new HashMap<>(Map.of(
            1L, 100,
            2L, 50,
            3L, 30,
            4L, 40,
            5L, 0
    ));

    public Map<Long, Integer> getInventory() {
        return new HashMap<>(inventory);
    }

    public Integer getStock(Long productId) {
        return inventory.get(productId);
    }

    public void putProduct(Long productId, int stock) {
        inventory.put(productId, Math.max(0, stock));
    }

    public void updateStock(Long productId, int delta) {
        int current = inventory.getOrDefault(productId, 0);
        int newStock = Math.max(0, current + delta);
        inventory.put(productId, newStock);
    }

    public void setStock(Long productId, int stock) {
        inventory.put(productId, Math.max(0, stock));
    }
}
