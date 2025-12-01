package com.traker.traker.vpn.service;

import com.traker.traker.entity.Role;
import com.traker.traker.entity.User;
import com.traker.traker.vpn.config.GlobalVpnConfig;
import com.traker.traker.vpn.dto.VpnKeyRequest;
import com.traker.traker.vpn.dto.VpnKeyResponse;
import com.traker.traker.vpn.entity.VpnKey;
import com.traker.traker.vpn.entity.VpnKeyStatus;
import com.traker.traker.vpn.mapper.VpnMapper;
import com.traker.traker.vpn.repository.VpnKeyRepository;
import com.traker.traker.vpn.xray.XrayConfigManager;
import com.traker.traker.vpn.xray.XrayReloader;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VpnKeyService {

    private static final String PROTOCOL_VLESS = "VLESS_REALITY";

    private final VpnKeyRepository vpnKeyRepository;
    private final AesEncryptionService encryptionService;
    private final VpnAuditService auditService;
    private final GlobalVpnConfig config;
    private final XrayConfigManager xrayConfigManager;
    private final XrayReloader xrayReloader;
    private final VlessShareLinkGenerator shareLinkGenerator;

    @Transactional(readOnly = true)
    public List<VpnKeyResponse> listKeys() {
        User user = currentUser();
        UUID ownerId = userId(user);
        boolean admin = isAdmin(user);
        List<VpnKey> keys = admin ? vpnKeyRepository.findAllOrdered() : vpnKeyRepository.findAllByOwnerUserId(ownerId);
        auditService.audit(ownerId, "VIEW", null, "view vpn profiles");
        return keys.stream().map(VpnMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public VpnKeyResponse create(VpnKeyRequest request) {
        User user = currentUser();
        ensureIssuerOrAdmin(user);
        UUID ownerId = userId(user);

        LocalDateTime now = LocalDateTime.now();
        UUID clientUuid = UUID.randomUUID();
        String shortId = randomShortId();
        String realityDest = config.getDest() != null ? config.getDest() : config.getSni() + ":" + config.getServerPort();

        VpnKey key = VpnKey.builder()
                .id(UUID.randomUUID())
                .ownerUserId(ownerId)
                .name(request.getName())
                .publicKey("unused")
                .privateKeyEncrypted(encryptionService.encryptPrivateKey("unused"))
                .address(config.getServerHost() + ":" + config.getServerPort())
                .protocol(PROTOCOL_VLESS)
                .clientUuid(clientUuid)
                .realityShortId(shortId)
                .realitySni(config.getSni())
                .realityDest(realityDest)
                .flow(config.getFlow())
                .meta("{}")
                .expirationAt(request.getExpirationAt())
                .status(VpnKeyStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        vpnKeyRepository.save(key);
        syncAddClient(user, key);
        auditService.audit(ownerId, "CREATE", key.getId(), "created vless profile");
        return VpnMapper.toResponse(key);
    }

    @Transactional(readOnly = true)
    public VpnKeyResponse get(UUID id) {
        User user = currentUser();
        VpnKey key = vpnKeyRepository.findById(id).orElseThrow();
        ensureOwnerOrAdmin(user, key);
        auditService.audit(userId(user), "VIEW", key.getId(), "view vpn profile");
        return VpnMapper.toResponse(key);
    }

    @Transactional
    public VpnKeyResponse update(UUID id, VpnKeyRequest request) {
        User user = currentUser();
        VpnKey key = vpnKeyRepository.findById(id).orElseThrow();
        ensureOwnerOrAdmin(user, key);
        key.setName(request.getName());
        key.setExpirationAt(request.getExpirationAt());
        key.setUpdatedAt(LocalDateTime.now());
        vpnKeyRepository.save(key);
        auditService.audit(userId(user), "UPDATE", key.getId(), "update vpn profile");
        return VpnMapper.toResponse(key);
    }

    @Transactional
    public void revoke(UUID id) {
        User user = currentUser();
        VpnKey key = vpnKeyRepository.findById(id).orElseThrow();
        ensureOwnerOrAdmin(user, key);
        key.setStatus(VpnKeyStatus.REVOKED);
        key.setUpdatedAt(LocalDateTime.now());
        vpnKeyRepository.save(key);
        syncRemoveClient(key);
        auditService.audit(userId(user), "DELETE", key.getId(), "revoke vpn profile");
    }

    @Transactional(readOnly = true)
    public String downloadConfig(UUID id) {
        User user = currentUser();
        VpnKey key = vpnKeyRepository.findById(id).orElseThrow();
        ensureOwnerOrAdmin(user, key);
        if (VpnKeyStatus.REVOKED.equals(key.getStatus())) {
            throw new IllegalStateException("Profile revoked");
        }
        auditService.audit(userId(user), "DOWNLOAD", key.getId(), "download vless link");
        return shareLinkGenerator.generate(key);
    }

    private void syncAddClient(User user, VpnKey key) {
        try {
            xrayConfigManager.addClient(key.getClientUuid(), user.getUsername(), key.getFlow());
            xrayReloader.reload();
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Failed to register VLESS client", e);
        }
    }

    private void syncRemoveClient(VpnKey key) {
        try {
            xrayConfigManager.removeClient(key.getClientUuid());
            xrayReloader.reload();
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Failed to remove VLESS client", e);
        }
    }

    private String randomShortId() {
        byte[] bytes = new byte[4];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }

    private UUID userId(User user) {
        return UUID.nameUUIDFromBytes(String.valueOf(user.getId()).getBytes());
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream().map(Role::getName).anyMatch("ADMIN"::equals);
    }

    private void ensureOwnerOrAdmin(User user, VpnKey key) {
        UUID ownerId = userId(user);
        if (!isAdmin(user) && !ownerId.equals(key.getOwnerUserId())) {
            throw new IllegalStateException("Access denied");
        }
    }

    private void ensureIssuerOrAdmin(User user) {
        boolean allowed = user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(name -> "ADMIN".equals(name) || "VPN_ISSUER".equals(name));
        if (!allowed) {
            throw new IllegalStateException("Access denied");
        }
    }
}
