package com.example.gateway.registry;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Запись об одном зарегистрированном инстансе сервиса.
 */
public class RegisteredInstance {

    @Getter
    private final String serviceId;
    @Getter
    private final String instanceId;
    @Getter
    private final String host;
    @Getter
    private final int port;
    private final AtomicLong lastSeen = new AtomicLong(System.currentTimeMillis());

    public RegisteredInstance(String serviceId, String instanceId, String host, int port) {
        this.serviceId = serviceId;
        this.instanceId = instanceId;
        this.host = host;
        this.port = port;
    }

    public void touch() {
        lastSeen.set(System.currentTimeMillis());
    }

    public boolean isAlive(long now, long ttlMillis) {
        return now - lastSeen.get() <= ttlMillis;
    }

    public long getLastSeen() {
        return lastSeen.get();
    }
}
