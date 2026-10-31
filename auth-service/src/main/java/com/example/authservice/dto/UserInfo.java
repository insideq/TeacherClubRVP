package com.example.authservice.dto;

import java.time.Instant;

public record UserInfo(Long id, String username, String role, Instant createdAt) {
}
