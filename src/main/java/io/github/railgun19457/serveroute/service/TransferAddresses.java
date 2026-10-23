package io.github.railgun19457.serveroute.service;

import java.net.InetSocketAddress;

public final class TransferAddresses {
    public static final int DEFAULT_MINECRAFT_PORT = 25565;

    private TransferAddresses() {
    }

    /**
     * No proxy-side DNS. The host and port are emitted exactly as configured; the vanilla client
     * resolves {@code _minecraft._tcp} SRV records itself when the port is 25565. Resolving SRV here
     * would emit a non-default port for a host that may only exist as an SRV record, which the client
     * then reports as an unknown host.
     */
    public static InetSocketAddress unresolved(String host, int port) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Transfer host must not be blank");
        }
        return InetSocketAddress.createUnresolved(host.trim(), port);
    }
}
