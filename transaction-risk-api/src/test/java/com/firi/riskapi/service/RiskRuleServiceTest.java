package com.firi.riskapi.service;

import com.firi.riskapi.model.Transaction;
import com.firi.riskapi.repository.TransactionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RiskRuleServiceTest {

    @Test
    void transactionIsFlaggedWhenARuleFires() {
        TransactionRepository repo = mock(TransactionRepository.class);
        when(repo.findByAccountIdOrderByTimestampDesc(any())).thenReturn(List.of());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RiskRule rule = mock(RiskRule.class);
        when(rule.evaluate(any(), any())).thenReturn(Optional.of("TEST_RULE"));

        Transaction tx = new Transaction();
        tx.setAccountId("ACC-1");

        Transaction result = new RiskRuleService(List.of(rule), repo).evaluate(tx);

        assertTrue(result.isFlagged());          // a rule fired, so it must be flagged
        assertEquals("TEST_RULE", result.getFlagReason());
    }
}