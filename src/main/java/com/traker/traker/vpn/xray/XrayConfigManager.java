package com.traker.traker.vpn.xray;

import java.io.IOException;
import java.util.UUID;

public interface XrayConfigManager {
    void addClient(UUID clientUuid, String email, String flow) throws IOException;

    void removeClient(UUID clientUuid) throws IOException;
}
