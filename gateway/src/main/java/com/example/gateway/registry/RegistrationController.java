package com.example.gateway.registry;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Гейтвей сам принимает динамическую регистрацию сервисов.
 */
@RestController
@RequestMapping("/registry")
@RequiredArgsConstructor
public class RegistrationController {

    private final ServiceRegistry registry;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegistrationRequest request) {
        registry.register(request.getServiceId(), request.getInstanceId(), request.getHost(), request.getPort());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(@RequestBody RegistrationRequest request) {
        registry.heartbeat(request.getServiceId(), request.getInstanceId(), request.getHost(), request.getPort());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/deregister/{serviceId}/{instanceId}")
    public ResponseEntity<Void> deregister(@PathVariable String serviceId, @PathVariable String instanceId) {
        registry.deregister(serviceId, instanceId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/instances")
    public ResponseEntity<List<RegisteredInstance>> instances() {
        return ResponseEntity.ok(registry.snapshot());
    }
}
