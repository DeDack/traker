package com.traker.traker.vpn.repository;

import com.traker.traker.vpn.entity.VpnKey;
import com.traker.traker.vpn.entity.VpnKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VpnKeyRepository extends JpaRepository<VpnKey, UUID> {
    List<VpnKey> findAllByOwnerUserId(UUID ownerUserId);

    @Query("select v from VpnKey v order by v.createdAt desc")
    List<VpnKey> findAllOrdered();

    @Query("select max(v.address) from VpnKey v")
    String findMaxAddress();

    Optional<VpnKey> findByIdAndStatus(UUID id, VpnKeyStatus status);
}
