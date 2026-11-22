package com.firi.riskapi.service;

import com.firi.riskapi.model.Transaction;
import com.firi.riskapi.service.rules.HighAmountRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HighAmountRuleTest {

    private final HighAmountRule rule = new HighAmountRule();

    @Test
    void flagsTransactionAboveThreshold() {
        Transaction tx = Transaction.builder()
                .id(1L)
                .accountId("acc-1")
                .amount(new BigDecimal("3000"))
                .timestamp(Instant.now())
                .merchantCategory("ELECTRONICS")
                .build();

        Optional<String> result = rule.evaluate(tx, Collections.emptyList());

        assertThat(result).contains(HighAmountRule.REASON);
    }

    @Test
    void doesNotFlagTransactionAtOrBelowThreshold() {
        Transaction tx = Transaction.builder()
                .id(1L)
                .accountId("acc-1")
                .amount(new BigDecimal("2500"))
                .timestamp(Instant.now())
                .merchantCategory("GROCERIES")
                .build();

        Optional<String> result = rule.evaluate(tx, Collections.emptyList());

        assertThat(result).isEmpty();
    }
}
