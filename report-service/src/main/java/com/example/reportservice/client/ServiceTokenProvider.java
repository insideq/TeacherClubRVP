package com.example.reportservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Служебный JWT для внутренних вызовов.
 * report-service ходит через гейтвей (за списком руководителей и в файловое
 * хранилище), а гейтвей теперь защищён. Поэтому сервис логинится своей учётной
 * записью, кеширует токен и обновляет его заранее — упрощённый client credentials.
 * Без этого внутренние вызовы получали бы 401.
 */
@Component
@Slf4j
public class ServiceTokenProvider {

    private static final long REFRESH_MARGIN_SECONDS = 30;

    private final RestTemplate loginClient = new RestTemplate();

    private final String loginUrl;
    private final String username;
    private final String password;

    private final AtomicReference<CachedToken> cache = new AtomicReference<>();

    public ServiceTokenProvider(@Value("${service-auth.login-url}") String loginUrl,
                                @Value("${service-auth.username}") String username,
                                @Value("${service-auth.password}") String password) {
        this.loginUrl = loginUrl;
        this.username = username;
        this.password = password;
    }

    /** Актуальный токен: из кеша либо новый после логина. */
    public String currentToken() {
        CachedToken cached = cache.get();
        if (isFresh(cached)) {
            return cached.token();
        }
        synchronized (this) {
            cached = cache.get();
            if (isFresh(cached)) {
                return cached.token();
            }
            return login();
        }
    }

    private boolean isFresh(CachedToken cached) {
        return cached != null
                && cached.expiresAt().isAfter(Instant.now().plusSeconds(REFRESH_MARGIN_SECONDS));
    }

    private String login() {
        try {
            LoginResponse response = loginClient.postForObject(
                    loginUrl,
                    Map.of("username", username, "password", password),
                    LoginResponse.class);

            if (response == null || response.accessToken() == null) {
                throw new StorageUnavailableException("Гейтвей не вернул служебный токен", null);
            }

            Instant expiresAt = Instant.now().plusSeconds(response.expiresIn());
            cache.set(new CachedToken(response.accessToken(), expiresAt));
            log.info("Получен служебный JWT для {} (действует {} c)", username, response.expiresIn());
            return response.accessToken();
        } catch (RestClientException e) {
            throw new StorageUnavailableException(
                    "Не удалось авторизоваться в гейтвее для внутренних вызовов: " + e.getMessage(), e);
        }
    }

    private record CachedToken(String token, Instant expiresAt) {
    }

    private record LoginResponse(String accessToken, String tokenType, long expiresIn) {
    }
}
