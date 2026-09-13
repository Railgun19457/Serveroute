package io.github.railgun19457.serveroute.service;

import java.net.InetSocketAddress;

public final class TransferAddresses {
    private TransferAddresses() {
    }

    public static InetSocketAddress unresolved(String host, int port) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Transfer host must not be blank");
        }
        return InetSocketAddress.createUnresolved(host.trim(), port);
    }
}
