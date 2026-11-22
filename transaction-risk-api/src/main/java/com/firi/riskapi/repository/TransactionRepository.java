package com.firi.riskapi.repository;

import com.firi.riskapi.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByFlaggedTrue();

    List<Transaction> findByAccountIdAndTimestampBetween(String accountId, Instant from, Instant to);

    List<Transaction> findByAccountIdOrderByTimestampDesc(String accountId);
}
