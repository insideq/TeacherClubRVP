package com.example.gateway.discovery;

import com.example.gateway.registry.ServiceRegistry;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Источник списка инстансов для Spring Cloud LoadBalancer.
 * Вместо DNS-резолвинга берёт живые инстансы из внутреннего реестра гейтвея.
 */
public class RegistryServiceInstanceListSupplier implements ServiceInstanceListSupplier {

    private final ServiceRegistry registry;
    private final String serviceId;

    public RegistryServiceInstanceListSupplier(ServiceRegistry registry, String serviceId) {
        this.registry = registry;
        this.serviceId = serviceId;
    }

    @Override
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public Flux<List<ServiceInstance>> get() {
        return Flux.just(registry.getInstances(serviceId));
    }
}
