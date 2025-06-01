package com.voltcore.bank.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * Data Transfer Object for User entity.
 */
@Data
public class UserDTO {
    @JsonIgnore
    private Long id;
    private String username;
    private String password;
    private String role;
    private String email;
}