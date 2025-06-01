package com.voltcore.bank.mappers;

import com.voltcore.bank.dtos.TransactionDTO;
import com.voltcore.bank.entities.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper interface for converting between Transaction entity and TransactionDTO.
 */
@Mapper(componentModel = "spring")
public interface TransactionMapper {
    @Mapping(source = "account.id", target = "accountId")
    TransactionDTO toDTO(Transaction transaction);

    @Mapping(source = "accountId", target = "account.id")
    Transaction toEntity(TransactionDTO transactionDTO);
}