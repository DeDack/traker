package com.traker.traker.vpn.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class WireGuardIntegrationService {

    public void addPeer(String publicKey, String address) {
        execute("/usr/local/bin/vpn_add_peer.sh", publicKey, address);
    }

    public void removePeer(String publicKey) {
        execute("/usr/local/bin/vpn_remove_peer.sh", publicKey);
    }

    private void execute(String script, String... args) {
        try {
            String[] cmd = new String[args.length + 2];
            cmd[0] = "sudo";
            cmd[1] = script;
            System.arraycopy(args, 0, cmd, 2, args.length);
            Process process = new ProcessBuilder(cmd).start();
            int exit = process.waitFor();
            if (exit != 0) {
                log.warn("WireGuard script {} exited with status {}", script, exit);
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to execute WireGuard script", e);
        }
    }
}
