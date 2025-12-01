package com.traker.traker.vpn.xray;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traker.traker.vpn.config.GlobalVpnConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

class FileXrayConfigManagerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private Path tempFile;

    @AfterEach
    void cleanup() throws IOException {
        if (tempFile != null) {
            Files.deleteIfExists(tempFile);
            Files.deleteIfExists(tempFile.resolveSibling(tempFile.getFileName() + ".new"));
        }
    }

    @Test
    void addClientAppendsEntry() throws Exception {
        tempFile = createConfigFile("vless-reality-in");
        GlobalVpnConfig cfg = config(tempFile, "vless-reality-in");
        FileXrayConfigManager manager = new FileXrayConfigManager(mapper, cfg);

        UUID id = UUID.randomUUID();
        manager.addClient(id, "user", "xtls-rprx-vision");

        JsonNode root = mapper.readTree(Files.readAllBytes(tempFile));
        JsonNode clients = root.path("inbounds").get(0).path("settings").path("clients");
        assertTrue(StreamSupport.stream(clients.spliterator(), false)
                .anyMatch(node -> id.toString().equals(node.path("id").asText())));
    }

    @Test
    void removeClientDeletesEntry() throws Exception {
        tempFile = createConfigFile("vless-reality-in");
        GlobalVpnConfig cfg = config(tempFile, "vless-reality-in");
        FileXrayConfigManager manager = new FileXrayConfigManager(mapper, cfg);

        UUID id = UUID.randomUUID();
        manager.addClient(id, "user", "xtls-rprx-vision");
        manager.removeClient(id);

        JsonNode root = mapper.readTree(Files.readAllBytes(tempFile));
        JsonNode clients = root.path("inbounds").get(0).path("settings").path("clients");
        assertFalse(StreamSupport.stream(clients.spliterator(), false)
                .anyMatch(node -> id.toString().equals(node.path("id").asText())));
    }

    @Test
    void missingInboundThrows() throws Exception {
        tempFile = createConfigFile("other-tag");
        GlobalVpnConfig cfg = config(tempFile, "vless-reality-in");
        FileXrayConfigManager manager = new FileXrayConfigManager(mapper, cfg);
        assertThrows(IllegalStateException.class, () -> manager.addClient(UUID.randomUUID(), "user", "xtls-rprx-vision"));
    }

    private GlobalVpnConfig config(Path path, String inbound) {
        GlobalVpnConfig cfg = new GlobalVpnConfig();
        cfg.setConfigPath(path.toString());
        cfg.setInboundTag(inbound);
        cfg.setServerHost("example.com");
        cfg.setServerPort(443);
        cfg.setPublicKey("pk");
        cfg.setSni("sni");
        cfg.setFlow("xtls-rprx-vision");
        cfg.setReloadScript("/bin/true");
        cfg.setProfileNamePrefix("tracker");
        cfg.setDest("www.cloudflare.com:443");
        return cfg;
    }

    private Path createConfigFile(String inboundTag) throws IOException {
        String json = """
                {
                  \"inbounds\": [
                    {
                      \"tag\": \"%s\",
                      \"settings\": {
                        \"clients\": []
                      }
                    }
                  ]
                }
                """.formatted(inboundTag);
        Path file = Files.createTempFile("xray-config", ".json");
        Files.writeString(file, json);
        return file;
    }
}
