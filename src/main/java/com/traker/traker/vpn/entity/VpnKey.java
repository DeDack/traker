package com.traker.traker.vpn.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vpn_keys", schema = "vpn")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VpnKey {

    @Id
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String name;

    @Column(name = "public_key", nullable = false)
    private String publicKey;

    @Column(name = "private_key_encrypted", nullable = false)
    private String privateKeyEncrypted;

    @Column(nullable = false)
    private String address;

    @Column(name = "expiration_at")
    private LocalDateTime expirationAt;

    @Enumerated(EnumType.STRING)
    private VpnKeyStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
