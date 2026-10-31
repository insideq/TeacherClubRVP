package com.example.authservice.service;

import com.example.authservice.config.SecurityProperties;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.RegisterRequest;
import com.example.authservice.dto.TokenResponse;
import com.example.authservice.dto.UserInfo;
import com.example.authservice.entity.UserEntity;
import com.example.authservice.exception.UserAlreadyExistsException;
import com.example.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MIN_PASSWORD_LENGTH = 5;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final SecurityProperties properties;

    /** Проверяем пароль по BCrypt-хешу из БД и выдаём JWT. */
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        String username = request.username() == null ? "" : request.username().trim();

        UserEntity user = users.findByUsername(username)
                .filter(UserEntity::isEnabled)
                .orElseThrow(() -> new BadCredentialsException("Неверный логин или пароль"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Неверный логин или пароль");
        }

        return issueToken(user);
    }

    /** Регистрация нового пользователя. Роль всегда USER — повысить себя нельзя. */
    @Transactional
    public UserInfo register(RegisterRequest request) {
        String username = validateUsername(request.username());
        String password = validatePassword(request.password());

        if (users.existsByUsername(username)) {
            throw new UserAlreadyExistsException(username);
        }

        UserEntity user = users.save(new UserEntity(
                username,
                passwordEncoder.encode(password),
                "USER",
                true,
                Instant.now()));

        log.info("Зарегистрирован новый пользователь {}", user.getUsername());
        return new UserInfo(user.getId(), user.getUsername(), user.getRole(), user.getCreatedAt());
    }

    private TokenResponse issueToken(UserEntity user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("rvplab-auth-service")
                .subject(user.getUsername())
                .issuedAt(now)
                .expiresAt(now.plus(properties.jwt().ttlMinutes(), ChronoUnit.MINUTES))
                .claim("roles", List.of(user.getRole()))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        log.info("Выдан JWT для {} (роль {})", user.getUsername(), user.getRole());
        return new TokenResponse(token, "Bearer", properties.jwt().ttlMinutes() * 60,
                user.getUsername(), user.getRole());
    }

    private String validateUsername(String raw) {
        String username = raw == null ? "" : raw.trim();
        if (username.length() < MIN_USERNAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Логин должен быть не короче " + MIN_USERNAME_LENGTH + " символов");
        }
        if (!username.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException(
                    "В логине допустимы только латинские буквы, цифры, точка, дефис и подчёркивание");
        }
        return username;
    }

    private String validatePassword(String raw) {
        if (raw == null || raw.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Пароль должен быть не короче " + MIN_PASSWORD_LENGTH + " символов");
        }
        return raw;
    }
}
