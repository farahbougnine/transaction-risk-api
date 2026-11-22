package com.firi.riskapi.service;

import com.firi.riskapi.model.Transaction;
import com.firi.riskapi.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses a CSV of transactions and evaluates each one through the risk engine
 * in chronological order (so velocity/category rules see accurate "prior"
 * history as they go).
 *
 * Rather than assuming a fixed column order, this reads the header row and
 * matches columns by name against a set of known aliases - so a differently
 * shaped public dataset (e.g. PaySim's nameOrig/type/step columns) can be
 * imported without the user having to manually reshape the file first.
 */
@Service
public class CsvImportService {

    // Safety cap: the risk engine evaluates one row at a time, querying that
    // account's history on every row, so an unbounded multi-million-row file
    // would take far too long for a demo import.
    private static final int MAX_ROWS = 3000;

    // If no real timestamp column can be found, synthetic timestamps start here.
    private static final Instant SYNTHETIC_START = Instant.parse("2026-01-01T00:00:00Z");

    private static final Map<String, List<String>> COLUMN_ALIASES = Map.of(
            "accountId", List.of("accountid", "account_id", "nameorig", "senderid", "sender_id", "account", "customerid", "ccnum", "cc_num"),
            "amount", List.of("amount", "amt", "value", "transactionamount"),
            "merchantCategory", List.of("merchantcategory", "merchant_category", "category", "type", "transactiontype"),
            "timestamp", List.of("timestamp", "time", "date", "datetime", "transdatetranstime"),
            "step", List.of("step") // PaySim-style: an integer hour offset instead of a real date
    );

    private static final List<DateTimeFormatter> TIMESTAMP_FORMATS = List.of(
            DateTimeFormatter.ISO_INSTANT,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(java.time.ZoneOffset.UTC),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(java.time.ZoneOffset.UTC)
    );

    private final TransactionRepository transactionRepository;
    private final RiskRuleService riskRuleService;

    public CsvImportService(TransactionRepository transactionRepository, RiskRuleService riskRuleService) {
        this.transactionRepository = transactionRepository;
        this.riskRuleService = riskRuleService;
    }

    public ImportResult importCsv(MultipartFile file) throws IOException {
        List<Transaction> imported = new ArrayList<>();
        int skipped = 0;
        int truncatedAt = -1;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            ColumnMap columnMap = ColumnMap.fromHeader(splitCsvLine(headerLine));

            String line;
            int rowNumber = 1;

            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }
                if (imported.size() >= MAX_ROWS) {
                    truncatedAt = rowNumber;
                    break;
                }

                try {
                    Transaction saved = parseAndEvaluate(splitCsvLine(line), columnMap, imported.size());
                    imported.add(saved);
                } catch (Exception e) {
                    skipped++;
                }
            }
        }

        long flaggedCount = imported.stream().filter(Transaction::isFlagged).count();
        return new ImportResult(imported.size(), (int) flaggedCount, skipped, truncatedAt);
    }

    private Transaction parseAndEvaluate(String[] fields, ColumnMap columnMap, int rowIndex) {
        String accountId = columnMap.get(fields, "accountId");
        String merchantCategory = columnMap.get(fields, "merchantCategory");
        BigDecimal amount = parseAmount(columnMap.get(fields, "amount"));
        Instant timestamp = resolveTimestamp(fields, columnMap, rowIndex);

        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("Missing account identifier");
        }

        Transaction transaction = Transaction.builder()
                .accountId(accountId)
                .amount(amount)
                .timestamp(timestamp)
                .merchantCategory(merchantCategory == null || merchantCategory.isBlank() ? "UNKNOWN" : merchantCategory)
                .flagged(false)
                .build();

        Transaction saved = transactionRepository.save(transaction);
        return riskRuleService.evaluate(saved);
    }

    private BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return BigDecimal.ZERO;
        }
        // Strip currency symbols / thousands separators some datasets include, e.g. "$1,060.31"
        String cleaned = raw.replaceAll("[^0-9.\\-]", "");
        return cleaned.isBlank() ? BigDecimal.ZERO : new BigDecimal(cleaned);
    }

    /**
     * Figures out a real Instant for the row, trying in order:
     * 1. A genuine timestamp/date column, parsed as ISO-8601
     * 2. A "step" column (PaySim-style: integer hours since simulation start)
     * 3. A synthetic fallback: each row one second after the last, so
     *    velocity-style rules still have something meaningful to compare.
     */
    private Instant resolveTimestamp(String[] fields, ColumnMap columnMap, int rowIndex) {
        String rawTimestamp = columnMap.get(fields, "timestamp");
        if (rawTimestamp != null && !rawTimestamp.isBlank()) {
            for (DateTimeFormatter format : TIMESTAMP_FORMATS) {
                try {
                    return Instant.from(format.parse(rawTimestamp));
                } catch (DateTimeParseException ignored) {
                    // try the next format
                }
            }
        }

        String rawStep = columnMap.get(fields, "step");
        if (rawStep != null && !rawStep.isBlank()) {
            try {
                long hours = Long.parseLong(rawStep.trim());
                return SYNTHETIC_START.plusSeconds(hours * 3600);
            } catch (NumberFormatException ignored) {
                // fall through to synthetic fallback
            }
        }

        return SYNTHETIC_START.plusSeconds(rowIndex);
    }

    /**
     * Splits a CSV line on commas, respecting double-quoted fields (so a
     * quoted value containing a comma doesn't get split incorrectly).
     */
    private String[] splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());
        return fields.toArray(new String[0]);
    }

    /**
     * Maps our semantic field names (accountId, amount, etc.) to whichever
     * column index actually holds that data in this particular file, based
     * on matching the header row against known aliases.
     */
    private static class ColumnMap {
        private final Map<String, Integer> fieldToIndex;

        private ColumnMap(Map<String, Integer> fieldToIndex) {
            this.fieldToIndex = fieldToIndex;
        }

        static ColumnMap fromHeader(String[] headerFields) {
            Map<String, Integer> result = new java.util.HashMap<>();

            for (int i = 0; i < headerFields.length; i++) {
                String normalized = headerFields[i].toLowerCase().replaceAll("[^a-z0-9]", "");
                for (Map.Entry<String, List<String>> entry : COLUMN_ALIASES.entrySet()) {
                    if (result.containsKey(entry.getKey())) {
                        continue; // first match wins if a file has duplicate-ish headers
                    }
                    for (String alias : entry.getValue()) {
                        if (normalized.equals(alias)) {
                            result.put(entry.getKey(), i);
                        }
                    }
                }
            }

            return new ColumnMap(result);
        }

        String get(String[] fields, String semanticName) {
            Integer index = fieldToIndex.get(semanticName);
            if (index == null || index >= fields.length) {
                return null;
            }
            return fields[index];
        }
    }

    public record ImportResult(int totalImported, int totalFlagged, int totalSkipped, int truncatedAtRow) {
    }
}
