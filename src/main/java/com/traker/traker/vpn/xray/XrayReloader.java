package com.traker.traker.vpn.xray;

import java.io.IOException;

public interface XrayReloader {
    void reload() throws IOException, InterruptedException;
}
