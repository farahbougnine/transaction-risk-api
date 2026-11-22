package com.firi.riskapi.service.rules;

import com.firi.riskapi.model.Transaction;
import com.firi.riskapi.service.RiskRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Flags any single transaction over a fixed threshold.
 */
@Component
public class HighAmountRule implements RiskRule {

    public static final BigDecimal THRESHOLD = new BigDecimal("2500");
    public static final String REASON = "HIGH_AMOUNT";

    @Override
    public Optional<String> evaluate(Transaction candidate, List<Transaction> recentHistory) {
        if (candidate.getAmount() != null && candidate.getAmount().compareTo(THRESHOLD) > 0) {
            return Optional.of(REASON);
        }
        return Optional.empty();
    }
}
