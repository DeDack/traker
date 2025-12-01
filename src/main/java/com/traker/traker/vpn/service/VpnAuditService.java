package com.traker.traker.vpn.service;

import com.traker.traker.vpn.entity.VpnAudit;
import com.traker.traker.vpn.repository.VpnAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VpnAuditService {

    private final VpnAuditRepository repository;

    public void audit(UUID userId, String action, UUID vpnKeyId, String details) {
        VpnAudit audit = VpnAudit.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .action(action)
                .vpnKeyId(vpnKeyId)
                .timestamp(LocalDateTime.now())
                .details(details)
                .build();
        repository.save(audit);
    }

    public List<VpnAudit> findAll() {
        return repository.findAll();
    }
}
