package com.ecommerce.warehouseservice.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ecommerce.warehouseservice.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/warehouse")
public class WarehouseController {
    private static final Logger log = LoggerFactory.getLogger(WarehouseController.class);
private final InventoryService inventoryService;

    public WarehouseController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/inventory")
    public ResponseEntity<Map<Long, Integer>> getInventory() {
        return ResponseEntity.ok(inventoryService.getInventory());
    }

    @GetMapping("/inventory/{productId}")
    public ResponseEntity<?> getStock(@PathVariable Long productId) {
        Integer stock = inventoryService.getStock(productId);
        if (stock == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("productId", productId, "stock", stock));
    }

    @PostMapping("/inventory")
    public ResponseEntity<?> addProductToInventory(@RequestBody Map<String, Object> body) {
        Object productIdObj = body.get("productId");
        Object stockObj = body.get("stockQuantity");

        if (productIdObj == null || stockObj == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Missing required fields: 'productId' and 'stockQuantity'")
            );
        }

        Long productId = ((Number) productIdObj).longValue();
        int stock = ((Number) stockObj).intValue();

        inventoryService.addOrUpdateProduct(productId, stock);
        log.info("Added product {} to inventory with stock: {}", productId, stock);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(
                Map.of("productId", productId, "stock", stock)
        );
    }

    @PatchMapping("/inventory/{productId}")
    public ResponseEntity<?> updateStock(@PathVariable Long productId, @RequestBody Map<String, Object> body) {
        log.info("PATCH /inventory/{} received with body: {}", productId, body);

        Object quantityObj = body.get("quantity");
        Object newStockObj = body.get("newStock");

        int quantity = 0;
        boolean isAbsoluteValue = newStockObj != null;
        Object valueObj = isAbsoluteValue ? newStockObj : quantityObj;

        if (valueObj instanceof Number) {
            quantity = ((Number) valueObj).intValue();
        } else if (valueObj instanceof String) {
            quantity = Integer.parseInt((String) valueObj);
        } else if (valueObj == null) {
            log.error("ERROR: Missing required field in PATCH request");
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Missing required field: either 'quantity' (delta) or 'newStock' (absolute value)")
            );
        }

        log.info("Updating inventory for product {}{}{}", productId, isAbsoluteValue ? " with newStock: " : " with quantity delta: ", quantity);

        if (isAbsoluteValue) {
            inventoryService.setStockAbsolute(productId, quantity);
        } else {
            inventoryService.updateStockByDelta(productId, quantity);
        }

        Integer newStock = inventoryService.getStock(productId);
        log.info("Product {} new stock: {}", productId, newStock);
        return ResponseEntity.ok(Map.of("productId", productId, "newStock", newStock));
    }
}

