package com.example.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Параметры JWT и пароли для первичного создания пользователей.
 */
@ConfigurationProperties(prefix = "security")
public record SecurityProperties(Jwt jwt, Seed seed) {

    public record Jwt(String secret, long ttlMinutes) {
    }

    /** Пароли только для сидирования: дальше меняются в БД. */
    public record Seed(String adminPassword, String userPassword, String servicePassword) {
    }
}
