package com.ecommerce.catalogueservice.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ecommerce.catalogueservice.model.Product;
import com.ecommerce.catalogueservice.service.ProductRepository;
import com.ecommerce.catalogueservice.service.WarehouseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/products")
public class CatalogueController {
    private static final Logger log = LoggerFactory.getLogger(CatalogueController.class);
    private final ProductRepository productRepository;
    private final WarehouseService warehouseService;

    public CatalogueController(ProductRepository productRepository, WarehouseService warehouseService) {
        this.productRepository = productRepository;
        this.warehouseService = warehouseService;
    }

    @GetMapping
    public List<Product> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        List<Product> products;
        if (category != null) {
            products = productRepository.findByCategory(category);
        } else if (search != null) {
            products = productRepository.findByNameContainingIgnoreCase(search);
        } else {
            products = productRepository.findAll();
        }
        addLiveStock(products);
        return products;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        return productRepository.findById(id).map(product -> {
            addLiveStock(product);
            return ResponseEntity.ok(product);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product savedProduct = productRepository.save(product);
        warehouseService.syncProductToWarehouse(
                savedProduct.getId(),
                savedProduct.getStockQuantity()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product updated) {
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
    public ResponseEntity<?> updateStock(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object deltaObj = body.get("delta");
        int deltaValue = 0;

        if (deltaObj instanceof Number) {
            deltaValue = ((Number) deltaObj).intValue();
        } else if (deltaObj instanceof String) {
            deltaValue = Integer.parseInt((String) deltaObj);
        }

        final int delta = deltaValue;
        log.info("Updating stock for product {} with delta: {}", id, delta);
        return productRepository.findById(id).map(p -> {
            int newStock = Math.max(0, p.getStockQuantity() + delta);
            p.setStockQuantity(newStock);
            productRepository.save(p);
            log.info("New stock for product {}: {}", id, newStock);
            return ResponseEntity.ok(Map.of("id", id, "newStock", p.getStockQuantity()));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void addLiveStock(Product product) {
        if (product == null) return;
        Integer liveStock = warehouseService.getStockFromWarehouse(product.getId());
        if (liveStock != null) {
            product.setStockQuantity(liveStock);
        }
    }

    private void addLiveStock(List<Product> products) {
        if (products == null) return;
        for (Product product : products) {
            addLiveStock(product);
        }
    }
}
