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

    // SYNCHRONOUS call to Catalogue Service to verify product exists and is in stock
    @PostMapping("/{sessionId}/items")
    public ResponseEntity<?> addItem(@PathVariable String sessionId, @RequestBody Map<String, Object> body){
        Long productId = Long.valueOf(body.get("productId").toString());
        int quantity = Integer.parseInt(body.getOrDefault("quantity", 1).toString());
        //SYNCHRONOUS REST CALL: Cart -> Catalogue
        Map<String,Object> product = catalogueClient.getProduct(productId);
        if(product == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Product not found"));
        }
        int stock = (int) product.getOrDefault("stockQuantity", 0);
        if(stock <= 0){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Product is out of stock"));
        }
        
        // Check if requested quantity exceeds available stock
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

    // Sync: Cart -> Catalogue (stock check) -> Payment (charge)
    // Async: Payment -> [order.confirmed queue] -> Warehouse
    @Transactional
    @PostMapping("/{sessionId}/checkout")
    public ResponseEntity<?> checkout(@PathVariable String sessionId, @RequestBody Map<String, String> body){
        List<CartItem> items = cartItemRepository.findBySessionId(sessionId);
        if(items.isEmpty()){
            return ResponseEntity.badRequest().body(Map.of("error", "Empty cart"));
        }

        String customerAddress = body.getOrDefault("address", "Unknown");
        String paymentMethod = body.getOrDefault("paymentMethod", "card");

        //SYNCHRONOUS verify stock - check BOTH warehouse AND catalogue
        for(CartItem item : items){
            // Check catalogue stock
            Map<String, Object> product = catalogueClient.getProduct(item.getProductId());
            if(product == null){
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Item no longer available: " + item.getProductName()));
            }
            int catalogueStock = (int) product.getOrDefault("stockQuantity", 0);
            
            // Check warehouse stock
            int warehouseStock = warehouseClient.getWarehouseStock(item.getProductId());
            
            // Use the minimum of the two as the authoritative stock
            int availableStock = Math.min(catalogueStock, warehouseStock);
            
            System.out.println("[CartService] Product " + item.getProductId() + " - Catalogue: " + catalogueStock + ", Warehouse: " + warehouseStock + ", Available: " + availableStock);
            
            if(availableStock < item.getQuantity()){
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Insufficient stock for " + item.getProductName() + ". Available: " + availableStock + ", Requested: " + item.getQuantity()));
            }
        }

        double subtotal = items.stream().mapToDouble(i -> i.getProductPrice() * i.getQuantity()).sum();
        double total = subtotal + SHIPPING_COST;

        //SYNCHRONOUS process payment
        Map<String, Object> paymentResult = paymentClient.processPayment(sessionId, total, paymentMethod, customerAddress);
        boolean paymentSuccess = Boolean.TRUE.equals(paymentResult.get("success"));

        if(!paymentSuccess){
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(Map.of("error", "Payment failed", "details", paymentResult));
        }

        // DECREMENT STOCK after successful payment
        System.out.println("[CartService] Payment successful. Decrementing stock for " + items.size() + " items...");
        for(CartItem item : items){
            System.out.println("[CartService] Decrementing product " + item.getProductId() + " by " + item.getQuantity());
            boolean warehouseOk = warehouseClient.decrementStock(item.getProductId(), item.getQuantity());
            boolean catalogueOk = catalogueClient.decrementStock(item.getProductId(), item.getQuantity());
            System.out.println("[CartService] Product " + item.getProductId() + " - Warehouse: " + warehouseOk + ", Catalogue: " + catalogueOk);
        }

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
