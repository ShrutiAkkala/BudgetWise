package com.budget.tracker.service;

import com.budget.tracker.model.Role;
import com.budget.tracker.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION = 86400000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION);
    }

    private User buildUser(String username) {
        return User.builder()
                .id(1L)
                .username(username)
                .email(username + "@test.com")
                .password("encoded")
                .role(Role.USER)
                .build();
    }

    @Test
    void generateToken_returnsNonNullToken() {
        User user = buildUser("alice");
        String token = jwtService.generateToken(user);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void extractUsername_returnsCorrectUsername() {
        User user = buildUser("alice");
        String token = jwtService.generateToken(user);
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void isTokenValid_withValidToken_returnsTrue() {
        User user = buildUser("alice");
        String token = jwtService.generateToken(user);
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValid_withWrongUser_returnsFalse() {
        User userA = buildUser("alice");
        User userB = buildUser("bob");
        String token = jwtService.generateToken(userA);
        assertThat(jwtService.isTokenValid(token, userB)).isFalse();
    }

    @Test
    void isTokenValid_withExpiredToken_returnsFalse() {
        JwtService expiredJwtService = new JwtService();
        ReflectionTestUtils.setField(expiredJwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(expiredJwtService, "jwtExpiration", -1000L);

        User user = buildUser("alice");
        String token = expiredJwtService.generateToken(user);
        assertThat(expiredJwtService.isTokenValid(token, user)).isFalse();
    }

    @Test
    void generateToken_withExtraClaims_extractsCorrectSubject() {
        User user = buildUser("alice");
        String token = jwtService.generateToken(Map.of("role", "USER"), user);
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
    }
}
