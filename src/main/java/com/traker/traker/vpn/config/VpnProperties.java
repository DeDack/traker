package com.traker.traker.vpn.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "vpn")
public class VpnProperties {
    /** Key used to encrypt private keys. */
    private String encryptionKey;
    /** Public key of the WireGuard server. */
    private String serverPublicKey;
    /** Public endpoint IP of the WireGuard server. */
    private String serverIp;
}
