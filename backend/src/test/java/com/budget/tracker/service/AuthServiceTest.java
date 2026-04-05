package com.budget.tracker.service;

import com.budget.tracker.dto.auth.AuthResponse;
import com.budget.tracker.dto.auth.LoginRequest;
import com.budget.tracker.dto.auth.RegisterRequest;
import com.budget.tracker.model.Role;
import com.budget.tracker.model.User;
import com.budget.tracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthService authService;

    private RegisterRequest buildRegisterRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setEmail("alice@test.com");
        req.setPassword("password123");
        return req;
    }

    private User buildSavedUser() {
        return User.builder()
                .id(1L)
                .username("alice")
                .email("alice@test.com")
                .password("encodedPassword")
                .role(Role.USER)
                .build();
    }

    @Test
    void register_withValidRequest_returnsAuthResponse() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(buildSavedUser());
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(buildRegisterRequest());

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("alice");
        assertThat(response.getEmail()).isEqualTo("alice@test.com");
    }

    @Test
    void register_withDuplicateUsername_throwsIllegalArgumentException() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(buildRegisterRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    void register_withDuplicateEmail_throwsIllegalArgumentException() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(buildRegisterRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void register_encodesPassword() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any())).thenReturn(buildSavedUser());
        when(jwtService.generateToken(any())).thenReturn("token");

        authService.register(buildRegisterRequest());

        verify(passwordEncoder).encode("password123");
    }

    @Test
    void login_withValidCredentials_returnsAuthResponse() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("password123");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(buildSavedUser()));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.login(req);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        assertThat(response.getToken()).isEqualTo("jwt-token");
    }

    @Test
    void login_returnsCorrectUsernameAndEmail() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("password123");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(buildSavedUser()));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.login(req);

        assertThat(response.getUsername()).isEqualTo("alice");
        assertThat(response.getEmail()).isEqualTo("alice@test.com");
    }
}
