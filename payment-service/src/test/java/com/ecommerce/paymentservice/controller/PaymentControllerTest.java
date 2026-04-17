package com.ecommerce.paymentservice.controller;

import com.ecommerce.paymentservice.model.Payment;
import com.ecommerce.paymentservice.service.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentRepository paymentRepository;

    @MockBean
    private JmsTemplate jmsTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private Payment epicTreasurePayment;
    private final String totallyRealSession = "epic-party-session-2026";
    private final String magicTransactionId = "TXN-GOLDEN-TICKET-777";

    @BeforeEach
    void setUp() {
        epicTreasurePayment = new Payment(
                magicTransactionId,
                totallyRealSession,
                999999.99,
                "stolen_gold_doubloons",
                Payment.PaymentStatus.SUCCESS
        );
        epicTreasurePayment.setId(1L);
        epicTreasurePayment.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void processPayment_ShouldSucceedWithLotsOfPirateGold() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "sessionId", totallyRealSession,
                "amount", 999999.99,
                "paymentMethod", "stolen_gold_doubloons",
                "customerAddress", "Top Secret Villain Base, Area 51"
        );

        when(paymentRepository.save(any(Payment.class))).thenReturn(epicTreasurePayment);
        // mocked JmsTemplate to do nothing when sending message to queue
        doNothing().when(jmsTemplate).convertAndSend(eq(PaymentController.ORDER_QUEUE), anyString());

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.amount").value(999999.99))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(jmsTemplate, times(1)).convertAndSend(eq(PaymentController.ORDER_QUEUE), anyString());
    }

    @Test
    void processPayment_ShouldFailWhenYouAreBroke() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "sessionId", "sad-broke-student-session",
                "amount", -50.00,
                "paymentMethod", "maxed_out_credit_card",
                "customerAddress", "A Cardboard Box"
        );

        Payment failedPayment = new Payment("TXN-BROKE-123", "sad-broke-student-session", -50.00, "maxed_out_credit_card", Payment.PaymentStatus.FAILED);
        when(paymentRepository.save(any(Payment.class))).thenReturn(failedPayment);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Invalid payment amount"));

        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(jmsTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    void getPayment_ShouldFindTheGoldenTicket() throws Exception {
        when(paymentRepository.findByTransactionId(magicTransactionId)).thenReturn(Optional.of(epicTreasurePayment));

        mockMvc.perform(get("/payments/{transactionId}", magicTransactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(magicTransactionId))
                .andExpect(jsonPath("$.amount").value(999999.99))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(paymentRepository).findByTransactionId(magicTransactionId);
    }

    @Test
    void getPayment_ShouldFailIfReceiptWasEatenByDog() throws Exception {
        when(paymentRepository.findByTransactionId("fake-receipt")).thenReturn(Optional.empty());

        mockMvc.perform(get("/payments/{transactionId}", "fake-receipt"))
                .andExpect(status().isNotFound());

        verify(paymentRepository).findByTransactionId("fake-receipt");
    }
}
