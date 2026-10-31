package com.example.authservice.dto;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String subject,
        String role) {
}
