package com.voltcore.bank.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Data Transfer Object for Account entity.
 */
@Data
public class AccountDTO {
    @JsonIgnore
    private Long id;
    private String accountNumber;
    private String accountHolderName;
    private BigDecimal balance;
    private String accountType;
    private String status;
    private BigDecimal interestRate;
    private String email;
}