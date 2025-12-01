package com.traker.traker.vpn.controller;

import com.traker.traker.vpn.dto.VpnAuditResponse;
import com.traker.traker.vpn.dto.VpnKeyRequest;
import com.traker.traker.vpn.dto.VpnKeyResponse;
import com.traker.traker.vpn.mapper.VpnMapper;
import com.traker.traker.vpn.service.VpnAuditService;
import com.traker.traker.vpn.service.VpnKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/vpn")
@RequiredArgsConstructor
public class VpnController {

    private final VpnKeyService vpnKeyService;
    private final VpnAuditService auditService;

    @PostMapping("/keys")
    @PreAuthorize("hasAnyRole('ADMIN','VPN_ISSUER')")
    public VpnKeyResponse create(@RequestBody VpnKeyRequest request) {
        return vpnKeyService.create(request);
    }

    @GetMapping("/keys")
    @PreAuthorize("hasAnyRole('ADMIN','VPN_ISSUER')")
    public List<VpnKeyResponse> list() {
        return vpnKeyService.listKeys();
    }

    @GetMapping("/keys/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VPN_ISSUER')")
    public VpnKeyResponse get(@PathVariable UUID id) {
        return vpnKeyService.get(id);
    }

    @PutMapping("/keys/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VPN_ISSUER')")
    public VpnKeyResponse update(@PathVariable UUID id, @RequestBody VpnKeyRequest request) {
        return vpnKeyService.update(id, request);
    }

    @DeleteMapping("/keys/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VPN_ISSUER')")
    public void delete(@PathVariable UUID id) {
        vpnKeyService.revoke(id);
    }

    @PostMapping("/keys/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> download(@PathVariable UUID id) {
        String config = vpnKeyService.downloadConfig(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"vpn-profile-" + id + ".txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(config);
    }

    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public List<VpnAuditResponse> audit() {
        return auditService.findAll().stream().map(VpnMapper::toResponse).collect(Collectors.toList());
    }
}
