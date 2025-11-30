package com.traker.traker.vpn.dto;

import com.traker.traker.vpn.entity.VpnKeyStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class VpnKeyResponse {
    private UUID id;
    private String name;
    private String publicKey;
    private String address;
    private LocalDateTime expirationAt;
    private VpnKeyStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID ownerUserId;
}
