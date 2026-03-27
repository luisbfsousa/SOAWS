package com.ecommerce.paymentservice.controller;

import com.ecommerce.paymentservice.model.OrderMessage;
import com.ecommerce.paymentservice.model.Payment;
import com.ecommerce.paymentservice.service.PaymentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/payments")
public class PaymentController{
    public static final String ORDER_QUEUE = "order.confirmed";
    private final PaymentRepository paymentRepository;
    private final JmsTemplate jmsTemplate;

    public PaymentController(PaymentRepository paymentRepository, JmsTemplate jmsTemplate){
        this.paymentRepository = paymentRepository;
        this.jmsTemplate = jmsTemplate;
    }

    @PostMapping
    public ResponseEntity<?> processPayment(@RequestBody Map<String,Object> body){
        String sessionId = (String) body.get("sessionId");
        double amount = ((Number) body.get("amount")).doubleValue();
        String paymentMethod = (String) body.getOrDefault("paymentMethod","card");
        String customerAddress = (String) body.getOrDefault("customerAddress","Unknown");

        boolean success = amount > 0;
        String transactionId = UUID.randomUUID().toString();
        Payment.PaymentStatus status = success
            ? Payment.PaymentStatus.SUCCESS
            : Payment.PaymentStatus.FAILED;

        Payment payment = new Payment(transactionId, sessionId, amount, paymentMethod, status);
        paymentRepository.save(payment);
        if(!success){
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Invalid payment amount"));
        }

        OrderMessage orderMessage = new OrderMessage(transactionId, sessionId, amount, customerAddress);
        jmsTemplate.convertAndSend(ORDER_QUEUE, orderMessage);
        System.out.println("[PaymentService] Published to queue: " + transactionId);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "transactionId", transactionId,
            "amount", amount,
            "status", status.name()
        ));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<?> getPayment(@PathVariable String transactionId){
        return paymentRepository.findByTransactionId(transactionId)
            .map(p -> ResponseEntity.ok(Map.of(
                "transactionId", p.getTransactionId(),
                "amount", p.getAmount(),
                "status", p.getStatus().name(),
                "createdAt", p.getCreatedAt().toString()
            ))).orElse(ResponseEntity.notFound().build());
    }
}
