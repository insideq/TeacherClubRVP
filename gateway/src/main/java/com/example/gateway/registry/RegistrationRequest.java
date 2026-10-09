package com.example.gateway.registry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Тело запроса регистрации/heartbeat от сервиса.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequest {

    private String serviceId;
    private String instanceId;
    private String host;
    private int port;
}
