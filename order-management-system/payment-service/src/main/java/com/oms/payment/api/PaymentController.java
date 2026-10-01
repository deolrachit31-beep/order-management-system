package com.oms.payment.api;

import com.oms.payment.api.dto.PaymentResponse;
import com.oms.payment.service.PaymentQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments")
public class PaymentController {

    private final PaymentQueryService paymentQueryService;

    public PaymentController(PaymentQueryService paymentQueryService) {
        this.paymentQueryService = paymentQueryService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a payment by id")
    public PaymentResponse get(@PathVariable UUID id) {
        return PaymentResponse.from(paymentQueryService.get(id));
    }

    @GetMapping
    @Operation(summary = "Get the payment recorded for an order")
    public PaymentResponse getByOrder(@RequestParam UUID orderId) {
        return PaymentResponse.from(paymentQueryService.getByOrderId(orderId));
    }
}
