package com.firi.riskapi.service.rules;

import com.firi.riskapi.model.Transaction;
import com.firi.riskapi.service.RiskRule;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Flags an account making too many transactions in a short window
 * (a classic card-testing / fraud pattern).
 */
@Component
public class VelocityRule implements RiskRule {

    public static final Duration WINDOW = Duration.ofMinutes(10);
    public static final int MAX_TX_IN_WINDOW = 3; // not counting the candidate itself
    public static final String REASON = "VELOCITY";

    @Override
    public Optional<String> evaluate(Transaction candidate, List<Transaction> recentHistory) {
        Instant cutoff = candidate.getTimestamp().minus(WINDOW);

        long countInWindow = recentHistory.stream()
                .filter(tx -> !tx.getId().equals(candidate.getId()))
                .filter(tx -> tx.getTimestamp().isAfter(cutoff))
                .filter(tx -> !tx.getTimestamp().isAfter(candidate.getTimestamp()))
                .count();

        if (countInWindow >= MAX_TX_IN_WINDOW) {
            return Optional.of(REASON);
        }
        return Optional.empty();
    }
}
