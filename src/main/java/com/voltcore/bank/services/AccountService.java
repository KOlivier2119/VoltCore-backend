package com.voltcore.bank.services;

import com.voltcore.bank.dtos.AccountDTO;
import com.voltcore.bank.dtos.TransactionDTO;
import com.voltcore.bank.entities.Account;
import com.voltcore.bank.entities.Transaction;
import com.voltcore.bank.mappers.AccountMapper;
import com.voltcore.bank.mappers.TransactionMapper;
import com.voltcore.bank.repositories.AccountRepository;
import com.voltcore.bank.repositories.TransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for managing comprehensive banking operations with email notifications.
 */
@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountMapper accountMapper;
    private final TransactionMapper transactionMapper;
    private final EmailService emailService;

    public AccountService(AccountRepository accountRepository,
                          TransactionRepository transactionRepository,
                          AccountMapper accountMapper,
                          TransactionMapper transactionMapper,
                          EmailService emailService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountMapper = accountMapper;
        this.transactionMapper = transactionMapper;
        this.emailService = emailService;
    }

    public List<AccountDTO> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(accountMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountDTO createAccount(AccountDTO accountDTO) {
        Account account = accountMapper.toEntity(accountDTO);
        account.setAccountNumber(UUID.randomUUID().toString());
        account.setBalance(BigDecimal.ZERO);
        account.setStatus("ACTIVE");
        Account savedAccount = accountRepository.save(account);
        return accountMapper.toDTO(savedAccount);
    }

    @Transactional
    public AccountDTO updateAccount(String accountNumber, AccountDTO accountDTO) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        account.setAccountHolderName(accountDTO.getAccountHolderName());
        account.setEmail(accountDTO.getEmail());
        account.setInterestRate(accountDTO.getInterestRate() != null ? accountDTO.getInterestRate() : BigDecimal.ZERO);
        Account savedAccount = accountRepository.save(account);
        return accountMapper.toDTO(savedAccount);
    }

    @Transactional
    public void deleteAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (!"CLOSED".equals(account.getStatus())) {
            throw new IllegalArgumentException("Account must be closed before deletion");
        }
        List<Transaction> transactions = transactionRepository.findByAccountId(account.getId());
        if (!transactions.isEmpty()) {
            throw new IllegalArgumentException("Cannot delete account with existing transactions");
        }
        accountRepository.delete(account);
    }

    @Transactional
    public TransactionDTO deposit(String accountNumber, BigDecimal amount, String paymentMethod) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        if (!isValidPaymentMethod(paymentMethod)) {
            throw new IllegalArgumentException("Invalid payment method");
        }
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType("DEPOSIT");
        transaction.setAmount(amount);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setDescription("Deposit to account " + accountNumber);
        transaction.setPaymentMethod(paymentMethod);
        Transaction savedTransaction = transactionRepository.save(transaction);

        emailService.sendTransactionEmail(savedTransaction);
        return transactionMapper.toDTO(savedTransaction);
    }

    @Transactional
    public TransactionDTO withdraw(String accountNumber, BigDecimal amount, String paymentMethod) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (!isValidPaymentMethod(paymentMethod)) {
            throw new IllegalArgumentException("Invalid payment method");
        }
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType("WITHDRAWAL");
        transaction.setAmount(amount);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setDescription("Withdrawal from account " + accountNumber);
        transaction.setPaymentMethod(paymentMethod);
        Transaction savedTransaction = transactionRepository.save(transaction);

        emailService.sendTransactionEmail(savedTransaction);
        return transactionMapper.toDTO(savedTransaction);
    }

    @Transactional
    public TransactionDTO transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount, String paymentMethod) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        if (!isValidPaymentMethod(paymentMethod)) {
            throw new IllegalArgumentException("Invalid payment method");
        }
        Account fromAccount = accountRepository.findByAccountNumber(fromAccountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Source account not found"));
        Account toAccount = accountRepository.findByAccountNumber(toAccountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Destination account not found"));
        if (!"ACTIVE".equals(fromAccount.getStatus()) || !"ACTIVE".equals(toAccount.getStatus())) {
            throw new IllegalArgumentException("One or both accounts are not active");
        }
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.saveAll(List.of(fromAccount, toAccount));

        Transaction transaction = new Transaction();
        transaction.setAccount(fromAccount);
        transaction.setTransactionType("TRANSFER");
        transaction.setAmount(amount);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setDescription("Transfer from " + fromAccountNumber + " to " + toAccountNumber);
        transaction.setPaymentMethod(paymentMethod);
        Transaction savedTransaction = transactionRepository.save(transaction);

        emailService.sendTransactionEmail(savedTransaction);
        return transactionMapper.toDTO(savedTransaction);
    }

    @Transactional
    public AccountDTO closeAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new IllegalArgumentException("Account is already closed");
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Account balance must be zero to close");
        }
        account.setStatus("CLOSED");
        Account savedAccount = accountRepository.save(account);
        return accountMapper.toDTO(savedAccount);
    }

    @Transactional
    public TransactionDTO applyInterest(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }
        if (account.getInterestRate() == null || account.getInterestRate().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("No interest rate set for this account");
        }

        BigDecimal interest = account.getBalance()
                .multiply(account.getInterestRate())
                .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
        account.setBalance(account.getBalance().add(interest));
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType("INTEREST");
        transaction.setAmount(interest);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setDescription("Interest applied to account " + accountNumber);
        Transaction savedTransaction = transactionRepository.save(transaction);

        emailService.sendTransactionEmail(savedTransaction);
        return transactionMapper.toDTO(savedTransaction);
    }

    @Transactional
    public TransactionDTO reverseTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        if ("REVERSAL".equals(transaction.getTransactionType())) {
            throw new IllegalArgumentException("Cannot reverse a reversal transaction");
        }
        if (transaction.getRelatedTransactionId() != null) {
            throw new IllegalArgumentException("Transaction already reversed");
        }

        Account account = transaction.getAccount();
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }

        BigDecimal amount = transaction.getAmount();
        String transactionType = transaction.getTransactionType();
        Transaction reversal = new Transaction();
        reversal.setAccount(account);
        reversal.setTransactionType("REVERSAL");
        reversal.setAmount(amount);
        reversal.setTransactionDate(LocalDateTime.now());
        reversal.setRelatedTransactionId(transactionId);
        reversal.setPaymentMethod(transaction.getPaymentMethod());

        if ("DEPOSIT".equals(transactionType)) {
            if (account.getBalance().compareTo(amount) < 0) {
                throw new IllegalArgumentException("Insufficient funds for reversal");
            }
            account.setBalance(account.getBalance().subtract(amount));
            reversal.setDescription("Reversal of deposit to account " + account.getAccountNumber());
        } else if ("WITHDRAWAL".equals(transactionType)) {
            account.setBalance(account.getBalance().add(amount));
            reversal.setDescription("Reversal of withdrawal from account " + account.getAccountNumber());
        } else if ("TRANSFER".equals(transactionType)) {
            throw new IllegalArgumentException("Transfer reversals require manual handling");
        } else {
            throw new IllegalArgumentException("Unsupported transaction type for reversal");
        }

        accountRepository.save(account);
        Transaction savedReversal = transactionRepository.save(reversal);
        emailService.sendTransactionEmail(savedReversal);
        return transactionMapper.toDTO(savedReversal);
    }

    public TransactionDTO getTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        return transactionMapper.toDTO(transaction);
    }

    @Transactional
    public void deleteTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        if (!List.of("REVERSAL", "INTEREST").contains(transaction.getTransactionType())) {
            throw new IllegalArgumentException("Only reversal or interest transactions can be deleted");
        }
        transactionRepository.delete(transaction);
    }

    public AccountDTO getAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        return accountMapper.toDTO(account);
    }

    public List<AccountDTO> getAccountsByStatus(String status) {
        return accountRepository.findByStatus(status).stream()
                .map(accountMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<TransactionDTO> getTransactionHistory(Long accountId) {
        return transactionRepository.findByAccountId(accountId).stream()
                .map(transactionMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<TransactionDTO> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(transactionMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransactionDTO createTransaction(TransactionDTO transactionDTO) {
        if (transactionDTO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive");
        }
        if (!isValidPaymentMethod(transactionDTO.getPaymentMethod())) {
            throw new IllegalArgumentException("Invalid payment method");
        }
        if (!List.of("DEPOSIT", "WITHDRAWAL", "TRANSFER").contains(transactionDTO.getTransactionType())) {
            throw new IllegalArgumentException("Invalid transaction type");
        }

        Account account = accountRepository.findByAccountNumber(transactionDTO.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }

        Transaction transaction = transactionMapper.toEntity(transactionDTO);
        transaction.setTransactionDate(LocalDateTime.now());

        if ("DEPOSIT".equals(transactionDTO.getTransactionType())) {
            account.setBalance(account.getBalance().add(transactionDTO.getAmount()));
            transaction.setDescription("Deposit to account " + transactionDTO.getAccountNumber());
        } else if ("WITHDRAWAL".equals(transactionDTO.getTransactionType())) {
            if (account.getBalance().compareTo(transactionDTO.getAmount()) < 0) {
                throw new IllegalArgumentException("Insufficient funds");
            }
            account.setBalance(account.getBalance().subtract(transactionDTO.getAmount()));
            transaction.setDescription("Withdrawal from account " + transactionDTO.getAccountNumber());
        } else if ("TRANSFER".equals(transactionDTO.getTransactionType())) {
            if (transactionDTO.getToAccountNumber() == null) {
                throw new IllegalArgumentException("Destination account number required for transfer");
            }
            Account toAccount = accountRepository.findByAccountNumber(transactionDTO.getToAccountNumber())
                    .orElseThrow(() -> new IllegalArgumentException("Destination account not found"));
            if (!"ACTIVE".equals(toAccount.getStatus())) {
                throw new IllegalArgumentException("Destination account is not active");
            }
            if (account.getBalance().compareTo(transactionDTO.getAmount()) < 0) {
                throw new IllegalArgumentException("Insufficient funds");
            }
            account.setBalance(account.getBalance().subtract(transactionDTO.getAmount()));
            toAccount.setBalance(toAccount.getBalance().add(transactionDTO.getAmount()));
            accountRepository.save(toAccount);
            transaction.setDescription("Transfer from " + transactionDTO.getAccountNumber() + " to " + transactionDTO.getToAccountNumber());
        }

        accountRepository.save(account);
        Transaction savedTransaction = transactionRepository.save(transaction);
        emailService.sendTransactionEmail(savedTransaction);
        return transactionMapper.toDTO(savedTransaction);
    }

    @Transactional
    public TransactionDTO updateTransaction(Long transactionId, TransactionDTO transactionDTO) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        if (List.of("REVERSAL", "INTEREST").contains(transaction.getTransactionType())) {
            throw new IllegalArgumentException("Cannot update reversal or interest transactions");
        }
        if (transaction.getRelatedTransactionId() != null) {
            throw new IllegalArgumentException("Cannot update reversed transactions");
        }

        Account account = transaction.getAccount();
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }

        // Revert the original transaction effect
        BigDecimal originalAmount = transaction.getAmount();
        String originalType = transaction.getTransactionType();
        if ("DEPOSIT".equals(originalType)) {
            if (account.getBalance().compareTo(originalAmount) < 0) {
                throw new IllegalArgumentException("Insufficient funds to revert original deposit");
            }
            account.setBalance(account.getBalance().subtract(originalAmount));
        } else if ("WITHDRAWAL".equals(originalType)) {
            account.setBalance(account.getBalance().add(originalAmount));
        } else if ("TRANSFER".equals(originalType)) {
            throw new IllegalArgumentException("Transfer updates require manual handling");
        }

        // Apply new transaction details
        if (!isValidPaymentMethod(transactionDTO.getPaymentMethod())) {
            throw new IllegalArgumentException("Invalid payment method");
        }
        if (transactionDTO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive");
        }

        transaction.setPaymentMethod(transactionDTO.getPaymentMethod());
        transaction.setAmount(transactionDTO.getAmount());
        transaction.setTransactionDate(LocalDateTime.now());

        if ("DEPOSIT".equals(transactionDTO.getTransactionType())) {
            account.setBalance(account.getBalance().add(transactionDTO.getAmount()));
            transaction.setDescription("Updated deposit to account " + account.getAccountNumber());
        } else if ("WITHDRAWAL".equals(transactionDTO.getTransactionType())) {
            if (account.getBalance().compareTo(transactionDTO.getAmount()) < 0) {
                throw new IllegalArgumentException("Insufficient funds for updated withdrawal");
            }
            account.setBalance(account.getBalance().subtract(transactionDTO.getAmount()));
            transaction.setDescription("Updated withdrawal from account " + account.getAccountNumber());
        } else {
            throw new IllegalArgumentException("Invalid transaction type for update");
        }

        accountRepository.save(account);
        Transaction savedTransaction = transactionRepository.save(transaction);
        emailService.sendTransactionEmail(savedTransaction);
        return transactionMapper.toDTO(savedTransaction);
    }

    public List<TransactionDTO> getTransactionsByType(String transactionType) {
        if (!List.of("DEPOSIT", "WITHDRAWAL", "TRANSFER", "INTEREST", "REVERSAL").contains(transactionType)) {
            throw new IllegalArgumentException("Invalid transaction type");
        }
        return transactionRepository.findByTransactionType(transactionType).stream()
                .map(transactionMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<TransactionDTO> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        return transactionRepository.findByTransactionDateBetween(startDate, endDate).stream()
                .map(transactionMapper::toDTO)
                .collect(Collectors.toList());
    }

    private boolean isValidPaymentMethod(String paymentMethod) {
        return paymentMethod != null && (
                paymentMethod.equals("PAYPAL") ||
                        paymentMethod.equals("CREDIT_CARD") ||
                        paymentMethod.equals("BANK_TRANSFER")
        );
    }
}