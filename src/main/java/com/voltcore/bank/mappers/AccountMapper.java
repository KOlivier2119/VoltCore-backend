package com.voltcore.bank.mappers;

import com.voltcore.bank.dtos.AccountDTO;
import com.voltcore.bank.entities.Account;
import org.mapstruct.Mapper;

/**
 * Mapper interface for converting between Account entity and AccountDTO.
 */
@Mapper(componentModel = "spring")
public interface AccountMapper {
    AccountDTO toDTO(Account account);
    Account toEntity(AccountDTO accountDTO);
}