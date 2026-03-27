package com.ecommerce.warehouseservice.controller;

import com.ecommerce.warehouseservice.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/warehouse")
public class WarehouseController{
    private final InventoryService inventoryService;

    public WarehouseController(InventoryService inventoryService){
        this.inventoryService = inventoryService;
    }

    @GetMapping("/inventory")
    public ResponseEntity<Map<Long, Integer>> getInventory(){
        return ResponseEntity.ok(inventoryService.getInventory());
    }

    @GetMapping("/inventory/{productId}")
    public ResponseEntity<?> getStock(@PathVariable Long productId){
        Integer stock = inventoryService.getStock(productId);
        if (stock == null){
            return ResponseEntity.notFound().build();
        } 
        return ResponseEntity.ok(Map.of("productId", productId, "stock", stock));
    }

    @PostMapping("/inventory")
    public ResponseEntity<?> addProductToInventory(@RequestBody Map<String, Object> body){
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
        System.out.println("[WarehouseService] Added product " + productId + " to inventory with stock: " + stock);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(
            Map.of("productId", productId, "stock", stock)
        );
    }

    @PatchMapping("/inventory/{productId}")
    public ResponseEntity<?> updateStock(@PathVariable Long productId, @RequestBody Map<String, Object> body){
        System.out.println("[WarehouseService] PATCH /inventory/" + productId + " received with body: " + body);
        
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
            System.err.println("[WarehouseService] ERROR: Missing required field in PATCH request");
            return ResponseEntity.badRequest().body(
                Map.of("error", "Missing required field: either 'quantity' (delta) or 'newStock' (absolute value)")
            );
        }
        
        System.out.println("[WarehouseService] Updating inventory for product " + productId + 
            (isAbsoluteValue ? " with newStock: " : " with quantity delta: ") + quantity);
        
        if (isAbsoluteValue) {
            inventoryService.setStockAbsolute(productId, quantity);
        } else {
            inventoryService.updateStockByDelta(productId, quantity);
        }
        
        Integer newStock = inventoryService.getStock(productId);
        System.out.println("[WarehouseService] Product " + productId + " new stock: " + newStock);
        return ResponseEntity.ok(Map.of("productId", productId, "newStock", newStock));
    }
}

