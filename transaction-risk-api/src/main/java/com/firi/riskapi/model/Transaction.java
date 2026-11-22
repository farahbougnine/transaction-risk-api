package com.firi.riskapi.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private String merchantCategory;

    @Column(nullable = false)
    private boolean flagged;

    // human-readable reason(s), comma separated, e.g. "HIGH_AMOUNT,VELOCITY"
    private String flagReason;

    // JPA requires a no-args constructor
    public Transaction() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getMerchantCategory() {
        return merchantCategory;
    }

    public void setMerchantCategory(String merchantCategory) {
        this.merchantCategory = merchantCategory;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    public String getFlagReason() {
        return flagReason;
    }

    public void setFlagReason(String flagReason) {
        this.flagReason = flagReason;
    }

    // Simple hand-written builder, so the rest of the code (which calls
    // Transaction.builder()....build()) doesn't need to change.
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Transaction transaction = new Transaction();

        public Builder id(Long id) {
            transaction.setId(id);
            return this;
        }

        public Builder accountId(String accountId) {
            transaction.setAccountId(accountId);
            return this;
        }

        public Builder amount(BigDecimal amount) {
            transaction.setAmount(amount);
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            transaction.setTimestamp(timestamp);
            return this;
        }

        public Builder merchantCategory(String merchantCategory) {
            transaction.setMerchantCategory(merchantCategory);
            return this;
        }

        public Builder flagged(boolean flagged) {
            transaction.setFlagged(flagged);
            return this;
        }

        public Builder flagReason(String flagReason) {
            transaction.setFlagReason(flagReason);
            return this;
        }

        public Transaction build() {
            return transaction;
        }
    }
}
