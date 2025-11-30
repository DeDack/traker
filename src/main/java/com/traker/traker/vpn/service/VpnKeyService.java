package com.traker.traker.vpn.service;

import com.traker.traker.entity.User;
import com.traker.traker.vpn.config.VpnProperties;
import com.traker.traker.vpn.dto.VpnKeyRequest;
import com.traker.traker.vpn.dto.VpnKeyResponse;
import com.traker.traker.vpn.entity.VpnKey;
import com.traker.traker.vpn.entity.VpnKeyStatus;
import com.traker.traker.vpn.mapper.VpnMapper;
import com.traker.traker.vpn.repository.VpnKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VpnKeyService {

    private static final String NETWORK_PREFIX = "10.0.0.";
    private final VpnKeyRepository vpnKeyRepository;
    private final AesEncryptionService encryptionService;
    private final WireGuardIntegrationService wireGuardIntegrationService;
    private final VpnAuditService auditService;
    private final VpnProperties properties;

    public List<VpnKeyResponse> listKeys(boolean admin) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        UUID ownerId = userId(user);
        List<VpnKey> keys = admin ? vpnKeyRepository.findAllOrdered() : vpnKeyRepository.findAllByOwnerUserId(ownerId);
        auditService.audit(ownerId, "VIEW", null, "view keys");
        return keys.stream().map(VpnMapper::toResponse).collect(Collectors.toList());
    }

    public VpnKeyResponse create(VpnKeyRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        UUID userId = userId(user);

        String privateKey = generatePrivateKey();
        String publicKey = generatePublicKey(privateKey);
        String address = allocateAddress();
        String encryptedPrivate = encryptionService.encryptPrivateKey(privateKey);
        LocalDateTime now = LocalDateTime.now();
        VpnKey key = VpnKey.builder()
                .id(UUID.randomUUID())
                .ownerUserId(userId)
                .name(request.getName())
                .publicKey(publicKey)
                .privateKeyEncrypted(encryptedPrivate)
                .address(address)
                .expirationAt(request.getExpirationAt())
                .status(VpnKeyStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
        vpnKeyRepository.save(key);
        wireGuardIntegrationService.addPeer(publicKey, address);
        auditService.audit(userId, "CREATE", key.getId(), "created vpn key");
        return VpnMapper.toResponse(key);
    }

    public VpnKeyResponse update(UUID id, VpnKeyRequest request, boolean admin) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        UUID userId = userId(user);
        VpnKey key = vpnKeyRepository.findById(id).orElseThrow();
        if (!admin && !key.getOwnerUserId().equals(userId)) {
            throw new IllegalStateException("Access denied");
        }
        key.setName(request.getName());
        key.setExpirationAt(request.getExpirationAt());
        key.setUpdatedAt(LocalDateTime.now());
        vpnKeyRepository.save(key);
        auditService.audit(userId, "UPDATE", key.getId(), "update vpn key");
        return VpnMapper.toResponse(key);
    }

    public void revoke(UUID id, boolean admin) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        UUID userId = userId(user);
        VpnKey key = vpnKeyRepository.findById(id).orElseThrow();
        if (!admin && !key.getOwnerUserId().equals(userId)) {
            throw new IllegalStateException("Access denied");
        }
        key.setStatus(VpnKeyStatus.REVOKED);
        key.setUpdatedAt(LocalDateTime.now());
        vpnKeyRepository.save(key);
        wireGuardIntegrationService.removePeer(key.getPublicKey());
        auditService.audit(userId, "DELETE", key.getId(), "revoke vpn key");
    }

    public String downloadConfig(UUID id, boolean admin) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        UUID userId = userId(user);
        VpnKey key = vpnKeyRepository.findById(id).orElseThrow();
        if (!admin && !key.getOwnerUserId().equals(userId)) {
            throw new IllegalStateException("Access denied");
        }
        String privateKey = encryptionService.decryptPrivateKey(key.getPrivateKeyEncrypted());
        auditService.audit(userId, "VIEW", key.getId(), "download config");
        return String.format("[Interface]\nPrivateKey = %s\nAddress = %s\nDNS = 1.1.1.1\n\n[Peer]\nPublicKey = %s\nEndpoint = %s:51820\nAllowedIPs = 0.0.0.0/0\nPersistentKeepalive = 25\n",
                privateKey, key.getAddress(), properties.getServerPublicKey(), properties.getServerIp());
    }

    private String allocateAddress() {
        String maxAddress = vpnKeyRepository.findMaxAddress();
        int next = 1;
        if (maxAddress != null && maxAddress.startsWith(NETWORK_PREFIX)) {
            String[] parts = maxAddress.split("\\.");
            next = Integer.parseInt(parts[3].split("/")[0]) + 1;
        }
        return NETWORK_PREFIX + next + "/32";
    }

    private String generatePrivateKey() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String generatePublicKey(String privateKey) {
        // Placeholder derivation for demo purposes
        return Base64.getEncoder().encodeToString((privateKey + "pub").getBytes());
    }

    private UUID userId(User user) {
        return UUID.nameUUIDFromBytes(String.valueOf(user.getId()).getBytes());
    }
}
