package com.firi.riskapi.service.rules;

import com.firi.riskapi.model.Transaction;
import com.firi.riskapi.service.RiskRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Flags a merchant category the account has never transacted in before,
 * but only once there is enough history to judge "unusual" from (avoids
 * flagging every brand-new account's very first purchases).
 */
@Component
public class UnusualCategoryRule implements RiskRule {

    public static final int MIN_HISTORY_SIZE = 5;
    public static final String REASON = "UNUSUAL_CATEGORY";

    @Override
    public Optional<String> evaluate(Transaction candidate, List<Transaction> recentHistory) {
        List<Transaction> priorOnly = recentHistory.stream()
                .filter(tx -> !tx.getId().equals(candidate.getId()))
                .filter(tx -> !tx.getTimestamp().isAfter(candidate.getTimestamp()))
                .collect(Collectors.toList());

        if (priorOnly.size() < MIN_HISTORY_SIZE) {
            return Optional.empty();
        }

        Set<String> knownCategories = priorOnly.stream()
                .map(Transaction::getMerchantCategory)
                .collect(Collectors.toSet());

        if (!knownCategories.contains(candidate.getMerchantCategory())) {
            return Optional.of(REASON);
        }
        return Optional.empty();
    }
}
