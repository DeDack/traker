package com.traker.traker.vpn.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "vpn.vless")
public class GlobalVpnConfig {
    private String serverHost;
    private int serverPort;
    private String publicKey;
    private String sni;
    private String flow;
    private String configPath;
    private String inboundTag;
    private String reloadScript;
    private String profileNamePrefix;
    private String dest;
}
