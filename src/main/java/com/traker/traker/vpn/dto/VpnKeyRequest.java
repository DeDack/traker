package com.traker.traker.vpn.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VpnKeyRequest {
    private String name;
    private LocalDateTime expirationAt;
}
