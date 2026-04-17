package com.ecommerce.catalogueservice.controller;

import com.ecommerce.catalogueservice.model.Product;
import com.ecommerce.catalogueservice.service.ProductRepository;
import com.ecommerce.catalogueservice.service.WarehouseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CatalogueController.class)
class CatalogueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private WarehouseService warehouseService;

    @Autowired
    private ObjectMapper objectMapper;

    private Product magicalHomeworkMachine;

    @BeforeEach
    void setUp() {
        magicalHomeworkMachine = new Product();
        magicalHomeworkMachine.setId(101L);
        magicalHomeworkMachine.setName("Magical Homework Machine");
        magicalHomeworkMachine.setCategory("Miracles");
        magicalHomeworkMachine.setPrice(999.99);
        magicalHomeworkMachine.setDescription("Does all your assignments while you sleep.");
        magicalHomeworkMachine.setStockQuantity(5);
    }

    @Test
    void getAllProducts_ShouldReturnEverything() throws Exception {
        when(productRepository.findAll()).thenReturn(List.of(magicalHomeworkMachine));
        when(warehouseService.getStockFromWarehouse(101L)).thenReturn(5);

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Magical Homework Machine"))
                .andExpect(jsonPath("$[0].category").value("Miracles"));

        verify(productRepository).findAll();
        verify(warehouseService).getStockFromWarehouse(101L);
    }

    @Test
    void getProduct_ShouldReturnOneSuperCoolItem() throws Exception {
        when(productRepository.findById(101L)).thenReturn(Optional.of(magicalHomeworkMachine));
        when(warehouseService.getStockFromWarehouse(101L)).thenReturn(42);

        mockMvc.perform(get("/products/{id}", 101L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Magical Homework Machine"))
                .andExpect(jsonPath("$.stockQuantity").value(42));

        verify(productRepository).findById(101L);
        verify(warehouseService).getStockFromWarehouse(101L);
    }

    @Test
    void getProduct_ShouldFailForImaginaryProduct() throws Exception {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/products/{id}", 999L))
                .andExpect(status().isNotFound());

        verify(productRepository).findById(999L);
    }

    @Test
    void createProduct_ShouldSaveNewInvention() throws Exception {
        Product shinyNewGadget = new Product();
        shinyNewGadget.setId(404L);
        shinyNewGadget.setName("Infinite Pizza Box");
        shinyNewGadget.setPrice(19.99);
        shinyNewGadget.setStockQuantity(10);

        when(productRepository.save(any(Product.class))).thenReturn(shinyNewGadget);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shinyNewGadget)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Infinite Pizza Box"))
                .andExpect(jsonPath("$.price").value(19.99));

        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateStock_ShouldChangeStockOfMagicalMachine() throws Exception {
        when(productRepository.findById(101L)).thenReturn(Optional.of(magicalHomeworkMachine));
        when(productRepository.save(any(Product.class))).thenReturn(magicalHomeworkMachine);

        Map<String, Object> requestBody = Map.of("delta", -2);

        mockMvc.perform(patch("/products/{id}/stock", 101L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.newStock").value(3)); // 5 - 2 = 3

        verify(productRepository).findById(101L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void deleteProduct_ShouldDestroyEvidence() throws Exception {
        when(productRepository.existsById(101L)).thenReturn(true);

        mockMvc.perform(delete("/products/{id}", 101L))
                .andExpect(status().isNoContent());

        verify(productRepository).existsById(101L);
    }
}
