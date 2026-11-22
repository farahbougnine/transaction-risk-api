package com.firi.riskapi.service;

import com.firi.riskapi.model.Transaction;
import com.firi.riskapi.service.rules.VelocityRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class VelocityRuleTest {

    private final VelocityRule rule = new VelocityRule();

    private Transaction txAt(long id, Instant time) {
        return Transaction.builder()
                .id(id)
                .accountId("acc-1")
                .amount(new BigDecimal("50"))
                .timestamp(time)
                .merchantCategory("GROCERIES")
                .build();
    }

    @Test
    void flagsWhenTooManyTransactionsWithinWindow() {
        Instant now = Instant.parse("2026-09-01T12:00:00Z");
        Transaction candidate = txAt(5L, now);

        List<Transaction> history = List.of(
                candidate,
                txAt(4L, now.minusSeconds(60)),
                txAt(3L, now.minusSeconds(120)),
                txAt(2L, now.minusSeconds(300)),
                txAt(1L, now.minusSeconds(3600)) // outside the 10-minute window
        );

        Optional<String> result = rule.evaluate(candidate, history);

        assertThat(result).contains(VelocityRule.REASON);
    }

    @Test
    void doesNotFlagWhenTransactionsAreSpreadOut() {
        Instant now = Instant.parse("2026-09-01T12:00:00Z");
        Transaction candidate = txAt(3L, now);

        List<Transaction> history = List.of(
                candidate,
                txAt(2L, now.minusSeconds(3600)),
                txAt(1L, now.minusSeconds(7200))
        );

        Optional<String> result = rule.evaluate(candidate, history);

        assertThat(result).isEmpty();
    }
}
