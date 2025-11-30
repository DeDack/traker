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
@RequestMapping("/api/vpn")
@RequiredArgsConstructor
public class VpnController {

    private final VpnKeyService vpnKeyService;
    private final VpnAuditService auditService;

    @PostMapping("/keys/create")
    @PreAuthorize("hasAnyRole('ADMIN','VPN_ISSUER')")
    public VpnKeyResponse create(@RequestBody VpnKeyRequest request) {
        return vpnKeyService.create(request);
    }

    @GetMapping("/keys/list")
    @PreAuthorize("hasAnyRole('ADMIN','VPN_ISSUER')")
    public List<VpnKeyResponse> list(@RequestParam(value = "all", defaultValue = "false") boolean all) {
        return vpnKeyService.listKeys(all);
    }

    @PutMapping("/keys/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VPN_ISSUER')")
    public VpnKeyResponse update(@PathVariable UUID id, @RequestBody VpnKeyRequest request, @RequestParam(value = "admin", defaultValue = "false") boolean admin) {
        return vpnKeyService.update(id, request, admin);
    }

    @DeleteMapping("/keys/delete/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VPN_ISSUER')")
    public void delete(@PathVariable UUID id, @RequestParam(value = "admin", defaultValue = "false") boolean admin) {
        vpnKeyService.revoke(id, admin);
    }

    @GetMapping("/keys/download-config/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VPN_ISSUER')")
    public ResponseEntity<String> download(@PathVariable UUID id, @RequestParam(value = "admin", defaultValue = "false") boolean admin) {
        String config = vpnKeyService.downloadConfig(id, admin);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=wg-" + id + ".conf")
                .contentType(MediaType.TEXT_PLAIN)
                .body(config);
    }

    @GetMapping("/audit/list")
    @PreAuthorize("hasRole('ADMIN')")
    public List<VpnAuditResponse> audit() {
        return auditService.findAll().stream().map(VpnMapper::toResponse).collect(Collectors.toList());
    }
}
