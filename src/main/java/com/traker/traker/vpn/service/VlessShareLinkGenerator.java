package com.traker.traker.vpn.service;

import com.traker.traker.vpn.config.GlobalVpnConfig;
import com.traker.traker.vpn.entity.VpnKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VlessShareLinkGenerator {

    private final GlobalVpnConfig config;

    public String generate(VpnKey key) {
        String host = config.getServerHost();
        int port = config.getServerPort();
        String sni = key.getRealitySni();
        String shortId = key.getRealityShortId();
        return String.format(
                "vless://%s@%s:%d?encryption=none&flow=%s&security=reality&sni=%s&fp=chrome&pbk=%s&type=tcp&headerType=none&sid=%s#%s",
                key.getClientUuid(),
                host,
                port,
                key.getFlow(),
                sni,
                config.getPublicKey(),
                shortId,
                config.getProfileNamePrefix());
    }
}
