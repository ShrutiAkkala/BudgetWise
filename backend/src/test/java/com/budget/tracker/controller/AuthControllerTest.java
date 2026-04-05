package com.budget.tracker.controller;

import com.budget.tracker.dto.auth.AuthResponse;
import com.budget.tracker.filter.JwtAuthenticationFilter;
import com.budget.tracker.service.AuthService;
import com.budget.tracker.service.JwtService;
import com.budget.tracker.service.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for AuthController using @WebMvcTest.
 *
 * Security notes:
 *   - SecurityConfig is auto-loaded by @WebMvcTest; it requires JwtAuthenticationFilter and
 *     UserDetailsServiceImpl beans, so both are provided as @MockBean.
 *   - /api/auth/** is permitAll() in SecurityConfig, so no user principal is needed here.
 *   - CSRF is disabled in SecurityConfig, so no csrf() post-processor is needed.
 *   - BadCredentialsException is caught by GlobalExceptionHandler's generic Exception handler
 *     and mapped to HTTP 500 (INTERNAL_SERVER_ERROR).
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    // Required by SecurityConfig constructor injection
    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // -------------------------------------------------------------------------
    // Register
    // -------------------------------------------------------------------------

    @Test
    void register_withValidRequest_returns201() throws Exception {
        AuthResponse response = new AuthResponse("token123", "alice", "alice@test.com");
        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "alice",
                                "email", "alice@test.com",
                                "password", "password123"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token123"))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@test.com"));
    }

    @Test
    void register_withBlankUsername_returns400() throws Exception {
        // RegisterRequest: @NotBlank + @Size(min=3) on username
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "",
                                "email", "alice@test.com",
                                "password", "password123"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withShortPassword_returns400() throws Exception {
        // RegisterRequest: @Size(min=6) on password — "123" has length 3
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "alice",
                                "email", "alice@test.com",
                                "password", "123"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withInvalidEmail_returns400() throws Exception {
        // RegisterRequest: @Email on email
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "alice",
                                "email", "notanemail",
                                "password", "password123"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withDuplicateUsername_returns400() throws Exception {
        // AuthService throws IllegalArgumentException → GlobalExceptionHandler returns 400
        when(authService.register(any()))
                .thenThrow(new IllegalArgumentException("Username already taken: alice"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "alice",
                                "email", "alice@test.com",
                                "password", "password123"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username already taken: alice"));
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    @Test
    void login_withValidCredentials_returns200() throws Exception {
        AuthResponse response = new AuthResponse("jwt-token", "alice", "alice@test.com");
        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "alice",
                                "password", "password123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@test.com"));
    }

    @Test
    void login_withBlankPassword_returns400() throws Exception {
        // LoginRequest: @NotBlank on password
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "alice",
                                "password", ""
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_withBadCredentials_returns500() throws Exception {
        // BadCredentialsException is not explicitly handled by GlobalExceptionHandler;
        // it falls through to the generic Exception handler which returns 500.
        when(authService.login(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "alice",
                                "password", "wrongpassword"
                        ))))
                .andExpect(status().isInternalServerError());
    }
}
