package com.oms.payment.domain;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class PaymentOutcomePolicy {

    public static final BigDecimal LIMIT = new BigDecimal("5000.00");

    public PaymentDecision decide(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return PaymentDecision.failed("Amount must be positive");
        }
        if (amount.compareTo(LIMIT) > 0) {
            return PaymentDecision.failed("Amount exceeds payment limit of 5000.00");
        }
        return PaymentDecision.completed();
    }

    public record PaymentDecision(PaymentStatus status, String reason) {

        public static PaymentDecision completed() {
            return new PaymentDecision(PaymentStatus.COMPLETED, null);
        }

        public static PaymentDecision failed(String reason) {
            return new PaymentDecision(PaymentStatus.FAILED, reason);
        }

        public boolean successful() {
            return status == PaymentStatus.COMPLETED;
        }
    }
}
