package com.example.filestorageservice.registration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequest {

    private String serviceId;
    private String instanceId;
    private String host;
    private int port;
}