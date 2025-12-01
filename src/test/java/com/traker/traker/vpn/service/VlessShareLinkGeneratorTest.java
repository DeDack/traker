package com.traker.traker.vpn.service;

import com.traker.traker.vpn.config.GlobalVpnConfig;
import com.traker.traker.vpn.entity.VpnKey;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VlessShareLinkGeneratorTest {

    @Test
    void buildsProperShareLink() {
        GlobalVpnConfig cfg = new GlobalVpnConfig();
        cfg.setServerHost("144.31.118.242");
        cfg.setServerPort(443);
        cfg.setPublicKey("PUBKEY");
        cfg.setProfileNamePrefix("tracker-vpn");
        cfg.setSni("www.cloudflare.com");
        VlessShareLinkGenerator generator = new VlessShareLinkGenerator(cfg);

        UUID clientId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        VpnKey key = VpnKey.builder()
                .clientUuid(clientId)
                .flow("xtls-rprx-vision")
                .realityShortId("1a2b3c4d")
                .realitySni("www.cloudflare.com")
                .build();

        String link = generator.generate(key);
        assertThat(link).isEqualTo(
                "vless://123e4567-e89b-12d3-a456-426614174000@144.31.118.242:443?encryption=none&flow=xtls-rprx-vision&security=reality&sni=www.cloudflare.com&fp=chrome&pbk=PUBKEY&type=tcp&headerType=none&sid=1a2b3c4d#tracker-vpn"
        );
    }
}
