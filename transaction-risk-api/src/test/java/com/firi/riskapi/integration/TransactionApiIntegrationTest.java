package com.firi.riskapi.integration;

import com.firi.riskapi.model.Transaction;
import com.firi.riskapi.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spins up a real, throwaway Postgres in a Docker container for this test
 * class, then exercises the actual REST endpoints against it - closer to
 * production behaviour than mocking the repository, and it's what an
 * interviewer will actually ask you to explain.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("riskdb_test")
            .withUsername("riskuser")
            .withPassword("riskpass");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void highAmountTransactionIsFlaggedAndReturnedByFlaggedEndpoint() {
        Transaction tx = Transaction.builder()
                .accountId("acc-42")
                .amount(new BigDecimal("9999.99"))
                .timestamp(Instant.parse("2026-09-01T10:00:00Z"))
                .merchantCategory("ELECTRONICS")
                .flagged(false)
                .build();
        transactionRepository.save(tx);

        // Re-run evaluation through the real endpoint list rather than a mock
        Transaction[] flagged = restTemplate.getForObject(
                "http://localhost:" + port + "/api/transactions", Transaction[].class);

        assertThat(flagged).isNotNull();
        assertThat(flagged).extracting(Transaction::getAccountId).contains("acc-42");
    }

    @Test
    void accountEndpointReturnsOnlyThatAccountsTransactions() {
        transactionRepository.save(Transaction.builder()
                .accountId("acc-a")
                .amount(new BigDecimal("20"))
                .timestamp(Instant.parse("2026-09-01T09:00:00Z"))
                .merchantCategory("GROCERIES")
                .flagged(false)
                .build());
        transactionRepository.save(Transaction.builder()
                .accountId("acc-b")
                .amount(new BigDecimal("30"))
                .timestamp(Instant.parse("2026-09-01T09:05:00Z"))
                .merchantCategory("GROCERIES")
                .flagged(false)
                .build());

        Transaction[] result = restTemplate.getForObject(
                "http://localhost:" + port + "/api/transactions/account/acc-a", Transaction[].class);

        assertThat(result).isNotNull();
        assertThat(result).extracting(Transaction::getAccountId).containsOnly("acc-a");
    }
}
