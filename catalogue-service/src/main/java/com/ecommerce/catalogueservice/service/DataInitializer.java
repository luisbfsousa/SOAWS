package com.ecommerce.catalogueservice.service;

import com.ecommerce.catalogueservice.model.Product;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner{
    private final ProductRepository productRepository;
    public DataInitializer(ProductRepository productRepository){
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args){
        productRepository.save(new Product("Classic White T-Shirt", "tops", 19.99, "Comfortable white cotton tee", 100, 1L));
        productRepository.save(new Product("Slim Fit Jeans", "bottoms", 49.99, "Dark blue slim jeans", 50, 1L));
        productRepository.save(new Product("Running Sneakers", "footwear", 89.99, "Lightweight running shoes", 30, 2L));
        productRepository.save(new Product("Wool Sweater", "tops", 59.99, "Warm merino wool sweater", 40, 2L));
        productRepository.save(new Product("Leather Belt", "accessories", 24.99, "Brown leather belt", 0, 1L));
    }
}
