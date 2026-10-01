package com.oms.payment.service;

import com.oms.payment.domain.DomainExceptions.NotFoundException;
import com.oms.payment.domain.Payment;
import com.oms.payment.repo.PaymentRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentQueryService {

    private final PaymentRepository paymentRepository;

    public PaymentQueryService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public Payment get(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + id));
    }

    @Transactional(readOnly = true)
    public Payment getByOrderId(UUID orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Payment not found for order: " + orderId));
    }
}
