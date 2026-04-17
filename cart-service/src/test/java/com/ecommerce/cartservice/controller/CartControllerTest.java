package com.ecommerce.cartservice.controller;

import com.ecommerce.cartservice.model.CartItem;
import com.ecommerce.cartservice.service.CartItemRepository;
import com.ecommerce.cartservice.service.CatalogueClient;
import com.ecommerce.cartservice.service.PaymentClient;
import com.ecommerce.cartservice.service.WarehouseClient;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartItemRepository cartItemRepository;

    @MockBean
    private CatalogueClient catalogueClient;

    @MockBean
    private PaymentClient paymentClient;

    @MockBean
    private WarehouseClient warehouseClient;

    @Autowired
    private ObjectMapper objectMapper;

    private String totallyRealSession;
    private CartItem epicCartItem;

    @BeforeEach
    void setUp() {
        totallyRealSession = "super-secret-student-session";
        epicCartItem = new CartItem(totallyRealSession, 999L, "Super Cool Energy Drink", 2.50, 2);
        epicCartItem.setId(1L);
    }

    @Test
    void getCart_ShouldReturnItemsAndSubtotal() throws Exception {
        when(cartItemRepository.findBySessionId(totallyRealSession)).thenReturn(List.of(epicCartItem));

        mockMvc.perform(get("/cart/{sessionId}", totallyRealSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productName").value("Super Cool Energy Drink"))
                .andExpect(jsonPath("$.subtotal").value(5.0));

        verify(cartItemRepository).findBySessionId(totallyRealSession);
    }

    @Test
    void addItem_ShouldAddAwesomeProductToCart() throws Exception {
        Long coolProductId = 1337L;
        Map<String, Object> requestBody = Map.of(
                "productId", coolProductId,
                "quantity", 1
        );

        Map<String, Object> mockCatalogueProduct = Map.of(
                "id", coolProductId,
                "name", "Totally Rad Skateboard",
                "price", 49.99,
                "stockQuantity", 100
        );

        when(catalogueClient.getProduct(coolProductId)).thenReturn(mockCatalogueProduct);
        when(cartItemRepository.findBySessionIdAndProductId(totallyRealSession, coolProductId)).thenReturn(Optional.empty());

        CartItem savedRadSkateboard = new CartItem(totallyRealSession, coolProductId, "Totally Rad Skateboard", 49.99, 1);
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(savedRadSkateboard);

        mockMvc.perform(post("/cart/{sessionId}/items", totallyRealSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productName").value("Totally Rad Skateboard"))
                .andExpect(jsonPath("$.quantity").value(1));

        verify(catalogueClient).getProduct(coolProductId);
        verify(cartItemRepository).findBySessionIdAndProductId(totallyRealSession, coolProductId);
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addItem_ShouldFailWhenProductIsTotallyFake() throws Exception {
        Long fakeProductId = 404L;
        Map<String, Object> requestBody = Map.of(
                "productId", fakeProductId,
                "quantity", 1
        );

        when(catalogueClient.getProduct(fakeProductId)).thenReturn(null);

        mockMvc.perform(post("/cart/{sessionId}/items", totallyRealSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Product not found"));

        verify(catalogueClient).getProduct(fakeProductId);
    }

    @Test
    void removeItem_ShouldRemoveBoringProduct() throws Exception {
        Long productId = 999L;
        when(cartItemRepository.findBySessionIdAndProductId(totallyRealSession, productId)).thenReturn(Optional.of(epicCartItem));

        mockMvc.perform(delete("/cart/{sessionId}/items/{productId}", totallyRealSession, productId))
                .andExpect(status().isNoContent());

        verify(cartItemRepository).findBySessionIdAndProductId(totallyRealSession, productId);
    }

    @Test
    void checkout_ShouldSucceedWithLotsOfMoney() throws Exception {
        Map<String, String> requestBody = Map.of(
                "address", "123 Student Dormitory Rd",
                "paymentMethod", "moms_credit_card"
        );

        when(cartItemRepository.findBySessionId(totallyRealSession)).thenReturn(List.of(epicCartItem));

        Map<String, Object> mockCatalogueProduct = Map.of(
                "stockQuantity", 500
        );
        when(catalogueClient.getProduct(999L)).thenReturn(mockCatalogueProduct);
        when(warehouseClient.getWarehouseStock(999L)).thenReturn(500);

        Map<String, Object> successfulPayment = Map.of(
                "success", true,
                "transactionId", "TXN-MOMS-CARD-1234"
        );
        when(paymentClient.processPayment(eq(totallyRealSession), anyDouble(), eq("moms_credit_card"), eq("123 Student Dormitory Rd")))
                .thenReturn(successfulPayment);

        when(warehouseClient.decrementStock(eq(999L), anyInt())).thenReturn(true);
        when(catalogueClient.decrementStock(eq(999L), anyInt())).thenReturn(true);

        mockMvc.perform(post("/cart/{sessionId}/checkout", totallyRealSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order placed successfully"))
                .andExpect(jsonPath("$.transactionId").value("TXN-MOMS-CARD-1234"))
                .andExpect(jsonPath("$.total").value(10.99));

        verify(cartItemRepository).findBySessionId(totallyRealSession);
        verify(catalogueClient).getProduct(999L);
        verify(warehouseClient).getWarehouseStock(999L);
        verify(paymentClient).processPayment(eq(totallyRealSession), anyDouble(), eq("moms_credit_card"), eq("123 Student Dormitory Rd"));
        verify(warehouseClient).decrementStock(eq(999L), anyInt());
        verify(catalogueClient).decrementStock(eq(999L), anyInt());
    }
}
