package com.example.authservice.service;

import com.example.authservice.config.SecurityProperties;
import com.example.authservice.entity.UserEntity;
import com.example.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Создаёт предустановленных пользователей при первом старте.
 *
 * Пароли берутся из конфига — в БД попадает уже BCrypt-хеш.
 * Если пользователь уже есть, ничего не трогаем: смена пароля в env больше не влияет.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties properties;

    @Override
    public void run(String... args) {
        SecurityProperties.Seed seed = properties.seed();

        seedUser("admin", password(seed == null ? null : seed.adminPassword(), "admin"), "ADMIN");
        seedUser("user", password(seed == null ? null : seed.userPassword(), "user"), "USER");
        seedUser("report-service",
                password(seed == null ? null : seed.servicePassword(), "report-service"), "SERVICE");
    }

    private void seedUser(String username, String rawPassword, String role) {
        if (users.existsByUsername(username)) {
            return;
        }
        users.save(new UserEntity(username, passwordEncoder.encode(rawPassword), role, true, Instant.now()));
        log.info("Создан предустановленный пользователь {} (роль {})", username, role);
    }

    private String password(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
