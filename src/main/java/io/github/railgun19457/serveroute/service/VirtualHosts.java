package io.github.railgun19457.serveroute.service;

import com.velocitypowered.api.proxy.Player;

import java.util.Optional;

public final class VirtualHosts {
    private VirtualHosts() {
    }

    public static Optional<String> of(Player player) {
        return HostMatcher.virtualHostOf(player.getRawVirtualHost(), player.getVirtualHost());
    }
}
