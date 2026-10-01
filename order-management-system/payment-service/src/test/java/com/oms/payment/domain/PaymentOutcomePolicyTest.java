package com.oms.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentOutcomePolicyTest {

    private final PaymentOutcomePolicy policy = new PaymentOutcomePolicy();

    @Test
    void approvesAPositiveAmountAtTheLimit() {
        assertThat(policy.decide(new BigDecimal("0.01")).successful()).isTrue();
        assertThat(policy.decide(new BigDecimal("25.00")).status()).isEqualTo(PaymentStatus.COMPLETED);
        PaymentOutcomePolicy.PaymentDecision atLimit = policy.decide(new BigDecimal("5000.00"));
        assertThat(atLimit.successful()).isTrue();
        assertThat(atLimit.reason()).isNull();
    }

    @Test
    void declinesNonPositiveAmounts() {
        assertThat(policy.decide(null).reason()).isEqualTo("Amount must be positive");
        assertThat(policy.decide(BigDecimal.ZERO).status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(policy.decide(new BigDecimal("-1.00")).reason()).isEqualTo("Amount must be positive");
    }

    @Test
    void declinesAmountsAboveTheLimit() {
        PaymentOutcomePolicy.PaymentDecision decision = policy.decide(new BigDecimal("5000.01"));

        assertThat(decision.successful()).isFalse();
        assertThat(decision.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(decision.reason()).isEqualTo("Amount exceeds payment limit of 5000.00");
    }
}
