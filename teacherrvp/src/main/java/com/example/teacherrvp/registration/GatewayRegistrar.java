package com.example.teacherrvp.registration;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;

/**
 * Регистрирует текущий инстанс в гейтвее и периодически шлёт heartbeat,
 * чтобы гейтвей знал, что этот инстанс жив и на него можно слать запросы.
 */
@Slf4j
@Component
public class GatewayRegistrar {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.application.name}")
    private String serviceId;

    @Value("${server.port}")
    private int port;

    @Value("${gateway.url:http://localhost:8080}")
    private String gatewayUrl;

    @Value("${HOSTNAME:localhost}")
    private String instanceId;

    @EventListener(ApplicationReadyEvent.class)
    public void register() {
        if (send("/registry/register")) {
            log.info("Registered in gateway: {} instance {} at {}:{}",
                    serviceId, instanceId, resolveHost(), port);
        }
    }

    @Scheduled(fixedDelay = 10000, initialDelay = 10000)
    public void heartbeat() {
        send("/registry/heartbeat");
    }

    @PreDestroy
    public void deregister() {
        try {
            restTemplate.delete(gatewayUrl + "/registry/deregister/" + serviceId + "/" + instanceId);
            log.info("Deregistered from gateway: {} instance {}", serviceId, instanceId);
        } catch (Exception e) {
            log.warn("Could not deregister from gateway: {}", e.getMessage());
        }
    }

    private boolean send(String path) {
        try {
            RegistrationRequest request = new RegistrationRequest(serviceId, instanceId, resolveHost(), port);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(gatewayUrl + path, new HttpEntity<>(request, headers), Void.class);
            log.debug("Gateway {} succeeded for {} instance {}", path, serviceId, instanceId);
            return true;
        } catch (Exception e) {
            log.warn("Gateway {} failed for {}: {}", path, serviceId, e.getMessage());
            return false;
        }
    }

    /**
     * Адрес, по которому гейтвей сможет достучаться до этого инстанса.
     * В Docker это IP контейнера; если вдруг вернулся loopback — берём HOSTNAME.
     */
    private String resolveHost() {
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            if (host != null && !host.startsWith("127.") && !host.startsWith("0.")) {
                return host;
            }
        } catch (Exception ignored) {
            // fallback ниже
        }
        return instanceId;
    }
}
