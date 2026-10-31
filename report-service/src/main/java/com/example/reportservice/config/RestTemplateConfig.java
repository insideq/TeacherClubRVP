package com.example.reportservice.config;

import com.example.reportservice.client.ServiceTokenProvider;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Все исходящие запросы report-service идут через защищённый гейтвей,
 * поэтому к каждому вызову добавляется служебный JWT.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, ServiceTokenProvider tokens) {
        return builder
                .additionalInterceptors((request, body, execution) -> {
                    request.getHeaders().setBearerAuth(tokens.currentToken());
                    return execution.execute(request, body);
                })
                .build();
    }
}
