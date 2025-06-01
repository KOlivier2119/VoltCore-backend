package com.voltcore.bank.mappers;

import com.voltcore.bank.dtos.UserDTO;
import com.voltcore.bank.entities.User;
import org.mapstruct.Mapper;

/**
 * Mapper interface for converting between User entity and UserDTO.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDTO toDTO(User user);
    User toEntity(UserDTO userDTO);
}