package com.example.teacherrvp.registration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Тело запроса регистрации/heartbeat, отправляемого в гейтвей.
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
