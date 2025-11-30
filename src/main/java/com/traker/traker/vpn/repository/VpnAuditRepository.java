package com.traker.traker.vpn.repository;

import com.traker.traker.vpn.entity.VpnAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VpnAuditRepository extends JpaRepository<VpnAudit, UUID> {
}
