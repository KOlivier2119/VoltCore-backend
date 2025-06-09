package com.voltcore.bank.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Transaction entity.
 */
@Data
public class TransactionDTO {
    @JsonIgnore
    private Long id;
    private Long accountId;
    private String accountNumber;
    private String toAccountNumber;
    private String transactionType;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String description;
    private Long relatedTransactionId;
    private String paymentMethod;

    @JsonIgnore
    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    @JsonIgnore
    public String getToAccountNumber() {
        return toAccountNumber;
    }

    public void setToAccountNumber(String toAccountNumber) {
        this.toAccountNumber = toAccountNumber;
    }
}