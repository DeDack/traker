package com.traker.traker.vpn.service;

import com.traker.traker.entity.Role;
import com.traker.traker.entity.User;
import com.traker.traker.vpn.config.GlobalVpnConfig;
import com.traker.traker.vpn.dto.VpnKeyRequest;
import com.traker.traker.vpn.entity.VpnKey;
import com.traker.traker.vpn.entity.VpnKeyStatus;
import com.traker.traker.vpn.repository.VpnKeyRepository;
import com.traker.traker.vpn.xray.XrayConfigManager;
import com.traker.traker.vpn.xray.XrayReloader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VpnKeyServiceTest {

    @Mock
    private VpnKeyRepository vpnKeyRepository;
    @Mock
    private AesEncryptionService encryptionService;
    @Mock
    private VpnAuditService auditService;
    @Mock
    private GlobalVpnConfig config;
    @Mock
    private XrayConfigManager xrayConfigManager;
    @Mock
    private XrayReloader xrayReloader;
    @Mock
    private VlessShareLinkGenerator shareLinkGenerator;

    @InjectMocks
    private VpnKeyService vpnKeyService;

    private User issuer;

    @BeforeEach
    void setup() {
        issuer = User.builder()
                .id(1L)
                .username("issuer")
                .roles(Set.of(Role.builder().name("VPN_ISSUER").build()))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(issuer, null, "ROLE_VPN_ISSUER"));

        when(config.getServerHost()).thenReturn("144.31.118.242");
        when(config.getServerPort()).thenReturn(443);
        when(config.getSni()).thenReturn("www.cloudflare.com");
        when(config.getFlow()).thenReturn("xtls-rprx-vision");
        when(config.getDest()).thenReturn("www.cloudflare.com:443");
        when(encryptionService.encryptPrivateKey(any())).thenReturn("enc");
        when(vpnKeyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPersistsVlessProfileAndReloads() throws Exception {
        VpnKeyRequest request = new VpnKeyRequest();
        request.setName("phone");
        request.setExpirationAt(LocalDateTime.now().plusDays(1));

        VpnKey response = vpnKeyService.create(request);

        ArgumentCaptor<VpnKey> saved = ArgumentCaptor.forClass(VpnKey.class);
        verify(vpnKeyRepository).save(saved.capture());
        verify(xrayConfigManager).addClient(any(UUID.class), eq("issuer"), eq("xtls-rprx-vision"));
        verify(xrayReloader).reload();
        assertThat(saved.getValue().getProtocol()).isEqualTo("VLESS_REALITY");
        assertThat(response.getFlow()).isEqualTo("xtls-rprx-vision");
    }

    @Test
    void revokeRemovesClient() throws Exception {
        UUID id = UUID.randomUUID();
        VpnKey key = VpnKey.builder()
                .id(id)
                .ownerUserId(UUID.nameUUIDFromBytes("1".getBytes()))
                .status(VpnKeyStatus.ACTIVE)
                .clientUuid(UUID.randomUUID())
                .build();
        when(vpnKeyRepository.findById(id)).thenReturn(Optional.of(key));

        vpnKeyService.revoke(id);

        verify(xrayConfigManager).removeClient(key.getClientUuid());
        verify(xrayReloader).reload();
        assertThat(key.getStatus()).isEqualTo(VpnKeyStatus.REVOKED);
    }

    @Test
    void downloadConfigFailsForRevoked() {
        UUID id = UUID.randomUUID();
        VpnKey key = VpnKey.builder()
                .id(id)
                .ownerUserId(UUID.nameUUIDFromBytes("1".getBytes()))
                .status(VpnKeyStatus.REVOKED)
                .build();
        when(vpnKeyRepository.findById(id)).thenReturn(Optional.of(key));

        assertThrows(IllegalStateException.class, () -> vpnKeyService.downloadConfig(id));
        verify(shareLinkGenerator, never()).generate(any());
    }
}
