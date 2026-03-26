package com.ecommerce.catalogueservice.controller;

import com.ecommerce.catalogueservice.model.Product;
import com.ecommerce.catalogueservice.service.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/products")
public class CatalogueController{
    private final ProductRepository productRepository;
    public CatalogueController(ProductRepository productRepository){
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<Product> getAllProducts(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String search){
            if(category != null){
                return productRepository.findByCategory(category); 
            }
            if(search != null){
                return productRepository.findByNameContainingIgnoreCase(search);
            }
        return productRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id){
        return productRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product){
        return ResponseEntity.status(HttpStatus.CREATED).body(productRepository.save(product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product updated){
        return productRepository.findById(id).map(p -> {
            p.setName(updated.getName());
            p.setCategory(updated.getCategory());
            p.setPrice(updated.getPrice());
            p.setDescription(updated.getDescription());
            p.setStockQuantity(updated.getStockQuantity());
            return ResponseEntity.ok(productRepository.save(p));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/stock")
    @Transactional
    public ResponseEntity<?> updateStock(@PathVariable Long id, @RequestBody Map<String, Object> body){
        Object deltaObj = body.get("delta");
        int deltaValue = 0;
        
        if (deltaObj instanceof Number) {
            deltaValue = ((Number) deltaObj).intValue();
        } else if (deltaObj instanceof String) {
            deltaValue = Integer.parseInt((String) deltaObj);
        }
        
        final int delta = deltaValue; // Make it final for lambda capture
        System.out.println("[CatalogueService] Updating stock for product " + id + " with delta: " + delta);
        return productRepository.findById(id).map(p -> {
            int newStock = Math.max(0, p.getStockQuantity() + delta);
            p.setStockQuantity(newStock);
            productRepository.save(p);
            System.out.println("[CatalogueService] New stock for product " + id + ": " + newStock);
            return ResponseEntity.ok(Map.of("id", id, "newStock", p.getStockQuantity()));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id){
        if(!productRepository.existsById(id)){
            return ResponseEntity.notFound().build();
        } 
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
