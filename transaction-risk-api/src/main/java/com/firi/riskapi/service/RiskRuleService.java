package com.firi.riskapi.service;

import com.firi.riskapi.model.Transaction;
import com.firi.riskapi.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Runs every registered RiskRule against a transaction and records which,
 * if any, fired. Spring injects all RiskRule beans automatically (List<RiskRule>),
 * so adding a new rule is just a matter of adding a new @Component - no wiring
 * changes needed here.
 */
@Service
public class RiskRuleService {

    private final List<RiskRule> rules;
    private final TransactionRepository transactionRepository;

    public RiskRuleService(List<RiskRule> rules, TransactionRepository transactionRepository) {
        this.rules = rules;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Evaluates a single already-persisted transaction against account history
     * and updates its flagged/flagReason fields.
     */
    public Transaction evaluate(Transaction candidate) {
        List<Transaction> history = transactionRepository.findByAccountIdOrderByTimestampDesc(candidate.getAccountId());

        List<String> reasons = rules.stream()
                .map(rule -> rule.evaluate(candidate, history))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        candidate.setFlagged (reasons.isEmpty());
        candidate.setFlagReason(reasons.isEmpty() ? null : String.join(",", reasons));

        return transactionRepository.save(candidate);
    }
}
