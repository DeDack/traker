package com.traker.traker.vpn.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class VpnWebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/vpn-keys").setViewName("forward:/vpn-keys.html");
        registry.addViewController("/vpn-keys/").setViewName("forward:/vpn-keys.html");
        registry.addViewController("/vpn-keys/audit").setViewName("forward:/vpn-keys/audit.html");
        registry.addViewController("/vpn-keys/audit/").setViewName("forward:/vpn-keys/audit.html");
    }
}

