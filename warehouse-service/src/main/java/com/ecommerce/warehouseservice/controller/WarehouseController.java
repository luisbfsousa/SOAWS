package com.ecommerce.warehouseservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/warehouse")
public class WarehouseController{
    private final Map<Long, Integer> inventory = new HashMap<>(Map.of(
        1L, 100,
        2L, 50,
        3L, 30,
        4L, 40
    ));

    @GetMapping("/inventory")
    public ResponseEntity<Map<Long, Integer>> getInventory(){
        return ResponseEntity.ok(inventory);
    }

    @GetMapping("/inventory/{productId}")
    public ResponseEntity<?> getStock(@PathVariable Long productId){
        Integer stock = inventory.get(productId);
        if (stock == null){
            return ResponseEntity.notFound().build();
        } 
        return ResponseEntity.ok(Map.of("productId", productId, "stock", stock));
    }

    @PatchMapping("/inventory/{productId}")
    public ResponseEntity<?> updateStock(@PathVariable Long productId, @RequestBody Map<String, Object> body){
        Object quantityObj = body.get("quantity");
        int quantity = 0;
        
        if (quantityObj instanceof Number) {
            quantity = ((Number) quantityObj).intValue();
        } else if (quantityObj instanceof String) {
            quantity = Integer.parseInt((String) quantityObj);
        }
        
        System.out.println("[WarehouseService] Updating inventory for product " + productId + " with quantity: " + quantity);
        int newStock = inventory.getOrDefault(productId, 0) + quantity;
        newStock = Math.max(0, newStock);
        inventory.put(productId, newStock);
        System.out.println("[WarehouseService] New stock for product " + productId + ": " + newStock);
        return ResponseEntity.ok(Map.of("productId", productId, "newStock", newStock));
    }
}
