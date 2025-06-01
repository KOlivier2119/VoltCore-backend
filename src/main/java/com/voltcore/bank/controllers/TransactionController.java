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
}