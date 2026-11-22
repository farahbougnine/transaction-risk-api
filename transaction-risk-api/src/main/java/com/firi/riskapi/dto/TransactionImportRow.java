package com.firi.riskapi.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents one row of an incoming CSV file:
 * accountId,amount,timestamp,merchantCategory
 */
public record TransactionImportRow(
        String accountId,
        BigDecimal amount,
        Instant timestamp,
        String merchantCategory
) {
}
