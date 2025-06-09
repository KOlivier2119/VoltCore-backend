package com.voltcore.bank.repositories;

import com.voltcore.bank.entities.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Transaction entity operations.
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountId(Long accountId);
    List<Transaction> findByTransactionType(String transactionType);
    List<Transaction> findByTransactionDateBetween(LocalDateTime startDate, LocalDateTime endDate);
}