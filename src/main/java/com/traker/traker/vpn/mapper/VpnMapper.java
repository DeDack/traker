package com.traker.traker.vpn.mapper;

import com.traker.traker.vpn.dto.VpnAuditResponse;
import com.traker.traker.vpn.dto.VpnKeyResponse;
import com.traker.traker.vpn.entity.VpnAudit;
import com.traker.traker.vpn.entity.VpnKey;

public class VpnMapper {
    private VpnMapper() {
    }

    public static VpnKeyResponse toResponse(VpnKey key) {
        return VpnKeyResponse.builder()
                .id(key.getId())
                .name(key.getName())
                .publicKey(key.getPublicKey())
                .address(key.getAddress())
                .protocol(key.getProtocol())
                .clientUuid(key.getClientUuid())
                .realityShortId(key.getRealityShortId())
                .realitySni(key.getRealitySni())
                .realityDest(key.getRealityDest())
                .flow(key.getFlow())
                .expirationAt(key.getExpirationAt())
                .status(key.getStatus())
                .createdAt(key.getCreatedAt())
                .updatedAt(key.getUpdatedAt())
                .ownerUserId(key.getOwnerUserId())
                .build();
    }

    public static VpnAuditResponse toResponse(VpnAudit audit) {
        return VpnAuditResponse.builder()
                .id(audit.getId())
                .userId(audit.getUserId())
                .action(audit.getAction())
                .vpnKeyId(audit.getVpnKeyId())
                .timestamp(audit.getTimestamp())
                .details(audit.getDetails())
                .build();
    }
}
