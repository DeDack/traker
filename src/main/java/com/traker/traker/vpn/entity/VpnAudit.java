package com.traker.traker.vpn.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vpn_audit", schema = "vpn")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VpnAudit {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String action;

    @Column(name = "vpn_key_id")
    private UUID vpnKeyId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private String details;
}
