package com.example.gateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Пробрасывает имя пользователя из проверенного JWT в заголовок X-Auth-User,
 * чтобы нижестоящие сервисы знали, кто инициировал запрос, и не парсили токен повторно.
 * Фильтр выполняется последним — уже после того, как Spring Security проверила токен.
 */
@Component
public class UserHeaderFilter implements GlobalFilter, Ordered {

    public static final String USER_HEADER = "X-Auth-User";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(auth -> exchange.mutate()
                        .request(request -> request.headers(headers ->
                                headers.set(USER_HEADER, auth.getToken().getSubject())))
                        .build())
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
