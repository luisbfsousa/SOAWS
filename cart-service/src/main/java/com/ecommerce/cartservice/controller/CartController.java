package com.ecommerce.cartservice.controller;

import com.ecommerce.cartservice.model.CartItem;
import com.ecommerce.cartservice.service.CartItemRepository;
import com.ecommerce.cartservice.service.CatalogueClient;
import com.ecommerce.cartservice.service.PaymentClient;
import com.ecommerce.cartservice.service.WarehouseClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/cart")
public class CartController{
    private static final double SHIPPING_COST = 5.99;
    private final CartItemRepository cartItemRepository;
    private final CatalogueClient catalogueClient;
    private final PaymentClient paymentClient;
    private final WarehouseClient warehouseClient;

    public CartController(CartItemRepository cartItemRepository, CatalogueClient catalogueClient, PaymentClient paymentClient, WarehouseClient warehouseClient){
        this.cartItemRepository = cartItemRepository;
        this.catalogueClient = catalogueClient;
        this.paymentClient = paymentClient;
        this.warehouseClient = warehouseClient;
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getCart(@PathVariable String sessionId){
        List<CartItem> items = cartItemRepository.findBySessionId(sessionId);
        double subtotal = items.stream().mapToDouble(i -> i.getProductPrice() * i.getQuantity()).sum();
        return ResponseEntity.ok(Map.of("items", items, "subtotal", subtotal));
    }

    @PostMapping("/{sessionId}/items")
    public ResponseEntity<?> addItem(@PathVariable String sessionId, @RequestBody Map<String, Object> body){
        Long productId = Long.valueOf(body.get("productId").toString());
        int quantity = Integer.parseInt(body.getOrDefault("quantity", 1).toString());
        Map<String,Object> product = catalogueClient.getProduct(productId);
        if(product == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Product not found"));
        }
        int stock = (int) product.getOrDefault("stockQuantity", 0);
        if(stock <= 0){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Product is out of stock"));
        }
        
        if(quantity > stock){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Insufficient stock. Available: " + stock + ", Requested: " + quantity));
        }

        String name = (String) product.get("name");
        double price = ((Number) product.get("price")).doubleValue();

        Optional<CartItem> existing = cartItemRepository.findBySessionIdAndProductId(sessionId, productId);
        if(existing.isPresent()){
            CartItem item = existing.get();
            int newQuantity = item.getQuantity() + quantity;
            if(newQuantity > stock){
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Total quantity exceeds stock. Available: " + stock + ", Requested total: " + newQuantity));
            }
            item.setQuantity(newQuantity);
            return ResponseEntity.ok(cartItemRepository.save(item));
        }

        CartItem item = new CartItem(sessionId, productId, name, price, quantity);
        return ResponseEntity.status(HttpStatus.CREATED).body(cartItemRepository.save(item));
    }

    @DeleteMapping("/{sessionId}/items/{productId}")
    public ResponseEntity<?> removeItem(@PathVariable String sessionId, @PathVariable Long productId){
        Optional<CartItem> item = cartItemRepository.findBySessionIdAndProductId(sessionId, productId);
        if (item.isEmpty()) return ResponseEntity.notFound().build();
        cartItemRepository.delete(item.get());
        return ResponseEntity.noContent().build();
    }

    @Transactional
    @PostMapping("/{sessionId}/checkout")
    public ResponseEntity<?> checkout(@PathVariable String sessionId, @RequestBody Map<String, String> body){
        System.out.println("[CartService] ========== CHECKOUT STARTED for session: " + sessionId + " ==========");
        List<CartItem> items = cartItemRepository.findBySessionId(sessionId);
        System.out.println("[CartService] Found " + items.size() + " items in cart");
        
        if(items.isEmpty()){
            return ResponseEntity.badRequest().body(Map.of("error", "Empty cart"));
        }

        String customerAddress = body.getOrDefault("address", "Unknown");
        String paymentMethod = body.getOrDefault("paymentMethod", "card");

        for(CartItem item : items){
            Map<String, Object> product = catalogueClient.getProduct(item.getProductId());
            if(product == null){
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Item no longer available: " + item.getProductName()));
            }
            int catalogueStock = (int) product.getOrDefault("stockQuantity", 0);
            
            int warehouseStock = warehouseClient.getWarehouseStock(item.getProductId());
            
            int availableStock = (warehouseStock >= 0) ? Math.min(catalogueStock, warehouseStock) : catalogueStock;
            
            System.out.println("[CartService] Product " + item.getProductId() + " - Catalogue: " + catalogueStock + ", Warehouse: " + warehouseStock + ", Available: " + availableStock);
            
            if(availableStock < item.getQuantity()){
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Insufficient stock for " + item.getProductName() + ". Available: " + availableStock + ", Requested: " + item.getQuantity()));
            }
        }

        double subtotal = items.stream().mapToDouble(i -> i.getProductPrice() * i.getQuantity()).sum();
        double total = subtotal + SHIPPING_COST;

        System.out.println("[CartService] Processing payment: subtotal=" + subtotal + ", total=" + total);
        Map<String, Object> paymentResult = paymentClient.processPayment(sessionId, total, paymentMethod, customerAddress);
        boolean paymentSuccess = Boolean.TRUE.equals(paymentResult.get("success"));
        System.out.println("[CartService] Payment response: " + paymentResult);
        System.out.println("[CartService] Payment success: " + paymentSuccess);

        if(!paymentSuccess){
            System.err.println("[CartService] Payment failed, aborting checkout");
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(Map.of("error", "Payment failed", "details", paymentResult));
        }

        System.out.println("[CartService] ========== STARTING STOCK DECREMENTS ==========");
        boolean allDecrementsOk = true;
        for(CartItem item : items){
            System.out.println("[CartService] ========== DECREMENTING PRODUCT " + item.getProductId() + " ==========");
            System.out.println("[CartService] Calling warehouseClient.decrementStock(" + item.getProductId() + ", " + item.getQuantity() + ")");
            boolean warehouseOk = warehouseClient.decrementStock(item.getProductId(), item.getQuantity());
            System.out.println("[CartService] Warehouse decrement result: " + (warehouseOk ? "SUCCESS" : "FAILED"));
            
            System.out.println("[CartService] Calling catalogueClient.decrementStock(" + item.getProductId() + ", " + item.getQuantity() + ")");
            boolean catalogueOk = catalogueClient.decrementStock(item.getProductId(), item.getQuantity());
            System.out.println("[CartService] Catalogue decrement result: " + (catalogueOk ? "SUCCESS" : "FAILED"));
            
            System.out.println("[CartService] Product " + item.getProductId() + " - Warehouse: " + (warehouseOk ? "SUCCESS" : "FAILED") + ", Catalogue: " + (catalogueOk ? "SUCCESS" : "FAILED"));
            
            if (!warehouseOk) {
                System.err.println("[CartService] WARNING: Failed to decrement warehouse stock for product " + item.getProductId());
                allDecrementsOk = false;
            }
            if (!catalogueOk) {
                System.err.println("[CartService] WARNING: Failed to decrement catalogue stock for product " + item.getProductId());
                allDecrementsOk = false;
            }
        }
        
        if (!allDecrementsOk) {
            System.err.println("[CartService] WARNING: Some stock decrements failed, but order was already processed");
        }
        System.out.println("[CartService] ========== STOCK DECREMENTS COMPLETE ==========");

        cartItemRepository.deleteBySessionId(sessionId);

        return ResponseEntity.ok(Map.of(
            "message", "Order placed successfully",
            "transactionId", paymentResult.getOrDefault("transactionId", "N/A"),
            "subtotal", subtotal,
            "shippingCost", SHIPPING_COST,
            "total", total
        ));
    }
}
