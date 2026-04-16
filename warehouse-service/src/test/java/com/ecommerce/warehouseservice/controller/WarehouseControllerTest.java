package com.ecommerce.warehouseservice.controller;

import com.ecommerce.warehouseservice.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WarehouseController.class)
class WarehouseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryService inventoryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getInventory_ShouldReturnTheWholeMessyGarage() throws Exception {
        Map<Long, Integer> mockGarageInventory = Map.of(
                9001L, 50,
                42L, 1
        );

        when(inventoryService.getInventory()).thenReturn(mockGarageInventory);

        mockMvc.perform(get("/warehouse/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.9001").value(50))
                .andExpect(jsonPath("$.42").value(1));
    }

    @Test
    void getStock_ShouldFindTheHiddenStash() throws Exception {
        Long secretStashId = 1337L;
        when(inventoryService.getStock(secretStashId)).thenReturn(999);

        mockMvc.perform(get("/warehouse/inventory/{productId}", secretStashId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1337))
                .andExpect(jsonPath("$.stock").value(999));
    }

    @Test
    void getStock_ShouldFailWhenSomeoneStoleIt() throws Exception {
        Long stolenItem = 404L;
        when(inventoryService.getStock(stolenItem)).thenReturn(null);

        mockMvc.perform(get("/warehouse/inventory/{productId}", stolenItem))
                .andExpect(status().isNotFound());
    }

    @Test
    void addProductToInventory_ShouldStockUpOnEnergyDrinks() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "productId", 9000L,
                "stockQuantity", 9001
        );

        doNothing().when(inventoryService).addOrUpdateProduct(9000L, 9001);

        mockMvc.perform(post("/warehouse/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(9000))
                .andExpect(jsonPath("$.stock").value(9001));
    }

    @Test
    void updateStock_ShouldDeductBecauseSomeoneSpilledCoffeeOnIt() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "quantity", -5
        );

        doNothing().when(inventoryService).updateStockByDelta(101L, -5);
        when(inventoryService.getStock(101L)).thenReturn(45); // originally 50

        mockMvc.perform(patch("/warehouse/inventory/{productId}", 101L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(101))
                .andExpect(jsonPath("$.newStock").value(45));
    }

    @Test
    void updateStock_ShouldAbsoluteSetBecauseOfMagicInventoryFairy() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "newStock", 100
        );

        doNothing().when(inventoryService).setStockAbsolute(202L, 100);
        when(inventoryService.getStock(202L)).thenReturn(100);

        mockMvc.perform(patch("/warehouse/inventory/{productId}", 202L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(202))
                .andExpect(jsonPath("$.newStock").value(100));
    }
}
