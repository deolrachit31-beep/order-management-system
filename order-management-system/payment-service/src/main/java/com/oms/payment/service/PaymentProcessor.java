package com.oms.payment.service;

import com.oms.payment.domain.Payment;
import com.oms.payment.domain.PaymentOutcomePolicy;
import com.oms.payment.domain.PaymentOutcomePolicy.PaymentDecision;
import com.oms.payment.messaging.PaymentEventPublisher;
import com.oms.payment.messaging.PaymentEvents.OrderCreatedEvent;
import com.oms.payment.repo.PaymentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);

    private final PaymentRepository paymentRepository;
    private final PaymentOutcomePolicy paymentOutcomePolicy;
    private final PaymentEventPublisher paymentEventPublisher;

    public PaymentProcessor(
            PaymentRepository paymentRepository,
            PaymentOutcomePolicy paymentOutcomePolicy,
            PaymentEventPublisher paymentEventPublisher) {
        this.paymentRepository = paymentRepository;
        this.paymentOutcomePolicy = paymentOutcomePolicy;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    @Transactional
    public Payment process(OrderCreatedEvent event) {
        Payment existing = paymentRepository.findByOrderId(event.orderId()).orElse(null);
        if (existing != null) {
            log.info("Payment already recorded for order {}", event.orderId());
            publishIfNeeded(existing);
            return existing;
        }

        PaymentDecision decision = paymentOutcomePolicy.decide(event.totalAmount());
        Payment payment = new Payment();
        payment.setOrderId(event.orderId());
        payment.setUserId(event.userId());
        payment.setAmount(scale(event.totalAmount()));
        payment.setCurrency(event.currency() == null || event.currency().isBlank() ? "USD" : event.currency());
        payment.setStatus(decision.status());
        payment.setReason(decision.reason());
        payment.setEventPublished(false);
        payment = paymentRepository.saveAndFlush(payment);
        log.info("Simulated payment {} for order {} amount {}", decision.status(), event.orderId(), payment.getAmount());
        publishIfNeeded(payment);
        return payment;
    }

    private void publishIfNeeded(Payment payment) {
        if (payment.isEventPublished()) {
            return;
        }
        paymentEventPublisher.publish(payment);
        payment.setEventPublished(true);
        paymentRepository.save(payment);
    }

    private static BigDecimal scale(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
