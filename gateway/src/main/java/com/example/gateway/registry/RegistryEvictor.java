package com.example.gateway.registry;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Периодически вычищает из реестра инстансы, переставшие слать heartbeat.
 */
@Component
@RequiredArgsConstructor
public class RegistryEvictor {

    private final ServiceRegistry registry;

    @Scheduled(fixedDelay = 5000)
    public void evictStaleInstances() {
        registry.evictStale(ServiceRegistry.INSTANCE_TTL_MILLIS);
    }
}
