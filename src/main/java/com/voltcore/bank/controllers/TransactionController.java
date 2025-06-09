package com.voltcore.bank.controllers;

import com.voltcore.bank.dtos.TransactionDTO;
import com.voltcore.bank.services.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST Controller for handling transaction-related operations with Swagger documentation.
 */
@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transaction Operations", description = "API for managing bank transactions")
public class TransactionController {
    private final AccountService accountService;

    public TransactionController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all transactions", description = "Retrieves all transactions in the system. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<List<TransactionDTO>> getAllTransactions() {
        return ResponseEntity.ok(accountService.getAllTransactions());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Create a transaction", description = "Creates a new transaction (deposit, withdrawal, or transfer). Accessible to Users and Admins.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid transaction data"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<TransactionDTO> createTransaction(@RequestBody TransactionDTO transactionDTO) {
        return ResponseEntity.ok(accountService.createTransaction(transactionDTO));
    }

    @PutMapping("/{transactionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a transaction", description = "Updates an existing transaction (deposit or withdrawal only). Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid transaction data or transaction not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<TransactionDTO> updateTransaction(@PathVariable Long transactionId, @RequestBody TransactionDTO transactionDTO) {
        return ResponseEntity.ok(accountService.updateTransaction(transactionId, transactionDTO));
    }

    @PostMapping("/{transactionId}/reverse")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reverse transaction", description = "Reverses a deposit or withdrawal transaction. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction reversed successfully"),
            @ApiResponse(responseCode = "400", description = "Transaction not found or cannot be reversed"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<TransactionDTO> reverseTransaction(@PathVariable Long transactionId) {
        return ResponseEntity.ok(accountService.reverseTransaction(transactionId));
    }

    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get transaction details", description = "Retrieves details of a specific transaction. Accessible to Users and Admins for their accounts.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction details retrieved"),
            @ApiResponse(responseCode = "400", description = "Transaction not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<TransactionDTO> getTransaction(@PathVariable Long transactionId) {
        return ResponseEntity.ok(accountService.getTransaction(transactionId));
    }

    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get transaction history", description = "Retrieves transaction history for the specified account. Accessible to Users and Admins.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction history retrieved"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<List<TransactionDTO>> getTransactionHistory(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.getTransactionHistory(accountId));
    }

    @DeleteMapping("/{transactionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete transaction", description = "Deletes a transaction if it’s a reversal or interest transaction. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Transaction deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Transaction not found or cannot be deleted"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long transactionId) {
        accountService.deleteTransaction(transactionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/type/{transactionType}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get transactions by type", description = "Retrieves all transactions of a specific type (e.g., DEPOSIT, WITHDRAWAL). Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid transaction type"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<List<TransactionDTO>> getTransactionsByType(@PathVariable String transactionType) {
        return ResponseEntity.ok(accountService.getTransactionsByType(transactionType));
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get transactions by date range", description = "Retrieves all transactions within a specified date range. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid date range"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<List<TransactionDTO>> getTransactionsByDateRange(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);
        return ResponseEntity.ok(accountService.getTransactionsByDateRange(start, end));
    }
}