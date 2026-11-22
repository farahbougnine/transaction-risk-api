package com.firi.riskapi.service;

import com.firi.riskapi.model.Transaction;

import java.util.List;
import java.util.Optional;

/**
 * A single risk rule. Given the transaction being evaluated and the account's
 * recent transaction history (most recent first), returns a short reason code
 * if the rule fires, or empty if it doesn't.
 *
 * Kept as pure logic (no Spring, no DB) so rules are trivial to unit test.
 */
public interface RiskRule {

    Optional<String> evaluate(Transaction candidate, List<Transaction> recentHistory);
}
