package com.traker.traker.vpn.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class VpnAuditResponse {
    private UUID id;
    private UUID userId;
    private String action;
    private UUID vpnKeyId;
    private LocalDateTime timestamp;
    private String details;
}
