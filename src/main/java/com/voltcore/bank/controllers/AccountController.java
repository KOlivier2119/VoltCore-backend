package com.voltcore.bank.controllers;

import com.voltcore.bank.dtos.AccountDTO;
import com.voltcore.bank.dtos.TransactionDTO;
import com.voltcore.bank.services.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST Controller for handling account-related operations with Swagger documentation.
 */
@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Account Operations", description = "API for managing bank accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new account", description = "Creates a new bank account with a unique account number. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account created successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<AccountDTO> createAccount(@RequestBody AccountDTO accountDTO) {
        return ResponseEntity.ok(accountService.createAccount(accountDTO));
    }

    @PutMapping("/{accountNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update account details", description = "Updates account holder name, email, or interest rate. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account updated successfully"),
            @ApiResponse(responseCode = "400", description = "Account not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<AccountDTO> updateAccount(@PathVariable String accountNumber, @RequestBody AccountDTO accountDTO) {
        return ResponseEntity.ok(accountService.updateAccount(accountNumber, accountDTO));
    }

    @DeleteMapping("/{accountNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete account", description = "Permanently deletes an account if it’s closed and has no transactions. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Account not found or cannot be deleted"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Void> deleteAccount(@PathVariable String accountNumber) {
        accountService.deleteAccount(accountNumber);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{accountNumber}/deposit")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Deposit money", description = "Deposits money into the specified account with a payment method (PAYPAL, CREDIT_CARD, BANK_TRANSFER). Accessible to Users and Admins.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deposit successful"),
            @ApiResponse(responseCode = "400", description = "Invalid amount, payment method, or account not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<TransactionDTO> deposit(@PathVariable String accountNumber,
                                                  @RequestParam String paymentMethod,
                                                  @RequestBody BigDecimal amount) {
        return ResponseEntity.ok(accountService.deposit(accountNumber, amount, paymentMethod));
    }

    @PostMapping("/{accountNumber}/withdraw")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Withdraw money", description = "Withdraws money from the specified account with a payment method (PAYPAL, CREDIT_CARD, BANK_TRANSFER). Accessible to Users and Admins.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Withdrawal successful"),
            @ApiResponse(responseCode = "400", description = "Invalid amount, insufficient funds, payment method, or account not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<TransactionDTO> withdraw(@PathVariable String accountNumber,
                                                   @RequestParam String paymentMethod,
                                                   @RequestBody BigDecimal amount) {
        return ResponseEntity.ok(accountService.withdraw(accountNumber, amount, paymentMethod));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Transfer money", description = "Transfers money between two accounts with a payment method (PAYPAL, CREDIT_CARD, BANK_TRANSFER). Accessible to Users and Admins.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transfer successful"),
            @ApiResponse(responseCode = "400", description = "Invalid amount, payment method, or accounts not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<TransactionDTO> transfer(@RequestParam String fromAccountNumber,
                                                   @RequestParam String toAccountNumber,
                                                   @RequestParam String paymentMethod,
                                                   @RequestBody BigDecimal amount) {
        return ResponseEntity.ok(accountService.transfer(fromAccountNumber, toAccountNumber, amount, paymentMethod));
    }

    @PostMapping("/{accountNumber}/close")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Close account", description = "Closes an account if its balance is zero. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account closed successfully"),
            @ApiResponse(responseCode = "400", description = "Account not found or non-zero balance"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<AccountDTO> closeAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.closeAccount(accountNumber));
    }

    @PostMapping("/{accountNumber}/apply-interest")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Apply interest", description = "Applies interest to the specified account based on its interest rate. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Interest applied successfully"),
            @ApiResponse(responseCode = "400", description = "Account not found or no interest rate set"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<TransactionDTO> applyInterest(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.applyInterest(accountNumber));
    }

    @GetMapping("/{accountNumber}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get account details", description = "Retrieves details of the specified account. Accessible to Users and Admins.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account details retrieved"),
            @ApiResponse(responseCode = "400", description = "Account not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<AccountDTO> getAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get accounts by status", description = "Retrieves all accounts with the specified status (e.g., ACTIVE, CLOSED). Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<List<AccountDTO>> getAccountsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(accountService.getAccountsByStatus(status));
    }
}