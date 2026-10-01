package com.oms.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oms.payment.domain.Payment;
import com.oms.payment.domain.PaymentOutcomePolicy;
import com.oms.payment.domain.PaymentStatus;
import com.oms.payment.messaging.PaymentEventPublisher;
import com.oms.payment.messaging.PaymentEvents.OrderCreatedEvent;
import com.oms.payment.repo.PaymentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentProcessorTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    private PaymentProcessor paymentProcessor;

    private final UUID orderId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        paymentProcessor = new PaymentProcessor(paymentRepository, new PaymentOutcomePolicy(), paymentEventPublisher);
    }

    @Test
    void completedPaymentPublishesPaymentCompleted() {
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentProcessor.process(event(new BigDecimal("25.00")));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getReason()).isNull();
        assertThat(payment.isEventPublished()).isTrue();
        verify(paymentEventPublisher).publish(payment);
        verify(paymentEventPublisher, never()).publishFailed(any());
    }

    @Test
    void amountOverTheLimitPublishesPaymentFailed() {
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentProcessor.process(event(new BigDecimal("5000.01")));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(captor.getValue().getReason()).contains("exceeds payment limit");
    }

    @Test
    void alreadyPublishedPaymentIsNotSentAgain() {
        Payment existing = new Payment();
        existing.setId(UUID.randomUUID());
        existing.setOrderId(orderId);
        existing.setStatus(PaymentStatus.COMPLETED);
        existing.setEventPublished(true);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));

        Payment result = paymentProcessor.process(event(new BigDecimal("10.00")));

        assertThat(result).isSameAs(existing);
        verify(paymentEventPublisher, never()).publish(any());
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    private OrderCreatedEvent event(BigDecimal amount) {
        return new OrderCreatedEvent(UUID.randomUUID(), orderId, userId, amount, "USD", Instant.now());
    }

    private static Payment withId(Payment payment) {
        payment.setId(UUID.randomUUID());
        return payment;
    }
}
