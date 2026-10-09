package com.example.gateway.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реестр инстансов, который живёт прямо внутри гейтвея.
 * Сервисы регистрируются/обновляются через REST-эндпоинты RegistrationController,
 * а балансировщик читает отсюда список живых инстансов.
 */
@Component
public class ServiceRegistry {

    private static final Logger log = LoggerFactory.getLogger(ServiceRegistry.class);

    /** Если heartbeat не приходил дольше этого времени — инстанс считается мёртвым. */
    public static final long INSTANCE_TTL_MILLIS = 30_000L;

    /** serviceId -> (instanceId -> инстанс) */
    private final Map<String, Map<String, RegisteredInstance>> instances = new ConcurrentHashMap<>();

    public void register(String serviceId, String instanceId, String host, int port) {
        Map<String, RegisteredInstance> byInstance =
                instances.computeIfAbsent(serviceId, k -> new ConcurrentHashMap<>());
        byInstance.put(instanceId, new RegisteredInstance(serviceId, instanceId, host, port));
        log.info("Registered {} instance {} at {}:{}", serviceId, instanceId, host, port);
    }

    public void heartbeat(String serviceId, String instanceId, String host, int port) {
        Map<String, RegisteredInstance> byInstance =
                instances.computeIfAbsent(serviceId, k -> new ConcurrentHashMap<>());
        RegisteredInstance existing = byInstance.get(instanceId);
        if (existing == null) {
            // Регистрация могла не пройти (гейтвей ещё стартовал) — дозаписываем по heartbeat.
            byInstance.put(instanceId, new RegisteredInstance(serviceId, instanceId, host, port));
            log.info("Registered {} instance {} at {}:{} (recovered via heartbeat)",
                    serviceId, instanceId, host, port);
        } else {
            existing.touch();
            log.debug("Heartbeat from {} instance {}", serviceId, instanceId);
        }
    }

    public void deregister(String serviceId, String instanceId) {
        Map<String, RegisteredInstance> byInstance = instances.get(serviceId);
        if (byInstance != null) {
            byInstance.remove(instanceId);
            if (byInstance.isEmpty()) {
                instances.remove(serviceId);
            }
            log.info("Deregistered {} instance {}", serviceId, instanceId);
        }
    }

    /** Возвращает только живые инстансы (для балансировщика). */
    public List<ServiceInstance> getInstances(String serviceId) {
        Map<String, RegisteredInstance> byInstance = instances.get(serviceId);
        if (byInstance == null) {
            return List.of();
        }
        long now = System.currentTimeMillis();
        List<ServiceInstance> result = new ArrayList<>();
        for (RegisteredInstance instance : byInstance.values()) {
            if (instance.isAlive(now, INSTANCE_TTL_MILLIS)) {
                result.add(new DefaultServiceInstance(
                        instance.getInstanceId(),
                        instance.getServiceId(),
                        instance.getHost(),
                        instance.getPort(),
                        false));
            }
        }
        return result;
    }

    /** Удаляет инстансы, от которых давно не было heartbeat. */
    public void evictStale(long ttlMillis) {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Map<String, RegisteredInstance>> entry : instances.entrySet()) {
            entry.getValue().entrySet().removeIf(e -> {
                boolean stale = !e.getValue().isAlive(now, ttlMillis);
                if (stale) {
                    log.info("Evicting stale instance {} of {}", e.getKey(), entry.getKey());
                }
                return stale;
            });
            if (entry.getValue().isEmpty()) {
                instances.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    /** Снимок реестра для отладки. */
    public List<RegisteredInstance> snapshot() {
        List<RegisteredInstance> result = new ArrayList<>();
        instances.values().forEach(map -> result.addAll(map.values()));
        return result;
    }
}
