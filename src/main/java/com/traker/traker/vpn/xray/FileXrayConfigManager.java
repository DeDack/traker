package com.traker.traker.vpn.xray;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.traker.traker.vpn.config.GlobalVpnConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileXrayConfigManager implements XrayConfigManager {

    private final ObjectMapper objectMapper;
    private final GlobalVpnConfig config;

    @Override
    public synchronized void addClient(UUID clientUuid, String email, String flow) throws IOException {
        Path configPath = Path.of(config.getConfigPath());
        ObjectNode root = readConfig(configPath);
        ObjectNode inboundNode = findInbound(root).orElseThrow(() -> new IllegalStateException("Inbound with tag '" + config.getInboundTag() + "' not found"));
        ObjectNode settings = inboundNode.with("settings");
        ArrayNode clients = settings.withArray("clients");

        ObjectNode clientNode = objectMapper.createObjectNode();
        clientNode.put("id", clientUuid.toString());
        clientNode.put("flow", flow);
        clientNode.put("email", email);
        clients.add(clientNode);

        writeConfig(configPath, root);
        log.info("Added VLESS client {} to inbound {}", clientUuid, config.getInboundTag());
    }

    @Override
    public synchronized void removeClient(UUID clientUuid) throws IOException {
        Path configPath = Path.of(config.getConfigPath());
        ObjectNode root = readConfig(configPath);
        ObjectNode inboundNode = findInbound(root).orElseThrow(() -> new IllegalStateException("Inbound with tag '" + config.getInboundTag() + "' not found"));
        ObjectNode settings = inboundNode.with("settings");
        ArrayNode clients = settings.withArray("clients");
        ArrayNode updated = objectMapper.createArrayNode();
        clients.forEach(node -> {
            JsonNode idNode = node.get("id");
            if (idNode == null || !clientUuid.toString().equals(idNode.asText())) {
                updated.add(node);
            }
        });
        settings.set("clients", updated);
        writeConfig(configPath, root);
        log.info("Removed VLESS client {} from inbound {}", clientUuid, config.getInboundTag());
    }

    private ObjectNode readConfig(Path configPath) throws IOException {
        byte[] bytes = Files.readAllBytes(configPath);
        return (ObjectNode) objectMapper.readTree(bytes);
    }

    private void writeConfig(Path configPath, ObjectNode root) throws IOException {
        Path tempFile = configPath.resolveSibling(configPath.getFileName() + ".new");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempFile.toFile(), root);
        Files.move(tempFile, configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    private Optional<ObjectNode> findInbound(ObjectNode root) {
        JsonNode inbounds = root.get("inbounds");
        if (inbounds == null || !inbounds.isArray()) {
            return Optional.empty();
        }
        for (JsonNode node : inbounds) {
            if (node.isObject() && config.getInboundTag().equals(node.path("tag").asText())) {
                return Optional.of((ObjectNode) node);
            }
        }
        return Optional.empty();
    }
}
