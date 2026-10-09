package com.example.gateway.config;

import com.example.gateway.discovery.RegistryServiceInstanceListSupplier;
import com.example.gateway.registry.ServiceRegistry;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Конфигурация LoadBalancer для всех сервисов.
 * Имя сервиса берётся из контекста LoadBalancer, поэтому один бин
 * обслуживает и teacher-service, и report-service.
 */
public class RegistryLoadBalancerClientConfiguration {

    @Bean
    public ServiceInstanceListSupplier registryServiceInstanceListSupplier(ServiceRegistry registry, Environment environment) {
        String serviceId = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RegistryServiceInstanceListSupplier(registry, serviceId);
    }
}
