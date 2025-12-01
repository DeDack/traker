package com.traker.traker.vpn.xray;

import com.traker.traker.vpn.config.GlobalVpnConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShellXrayReloader implements XrayReloader {

    private final GlobalVpnConfig config;

    @Override
    public void reload() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("sudo", config.getReloadScript());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("xray reload: {}", line);
            }
        }
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IllegalStateException("xray reload failed, exit=" + exit);
        }
        log.info("xray configuration reloaded successfully");
    }
}
