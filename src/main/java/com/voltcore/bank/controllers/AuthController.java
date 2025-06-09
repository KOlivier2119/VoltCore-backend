package com.voltcore.bank.controllers;

import com.voltcore.bank.config.JwtService;
import com.voltcore.bank.dtos.UserDTO;
import com.voltcore.bank.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * REST Controller for handling authentication operations with Swagger documentation.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "API for user registration and authentication")
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account with role and email.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Username already exists")
    })
    public ResponseEntity<UserDTO> register(@RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(authService.register(userDTO));
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates a user with username and password, returning a JWT and user details.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid username or password")
    })
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
            String token = jwtService.generateToken(authentication);
            UserDTO userDTO = new UserDTO();
            userDTO.setUsername(authentication.getName());
            userDTO.setRole(authentication.getAuthorities().stream()
                    .findFirst()
                    .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                    .orElse("USER"));
            return ResponseEntity.ok(new AuthResponse(token, userDTO));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(null);
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Returns details of the currently authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UserDTO> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(null);
        }
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(principal.getName());
        userDTO.setRole(authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(principal.getName(), null)
                ).getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .orElse("USER"));
        return ResponseEntity.ok(userDTO);
    }

    /**
     * Response body for login endpoint.
     */
    @Setter
    @Getter
    private static class AuthResponse {
        private String token;
        private UserDTO user;

        public AuthResponse(String token, UserDTO user) {
            this.token = token;
            this.user = user;
        }

    }

    /**
     * Request body for login endpoint.
     */
    @Setter
    @Getter
    private static class LoginRequest {
        private String username;
        private String password;

    }
}