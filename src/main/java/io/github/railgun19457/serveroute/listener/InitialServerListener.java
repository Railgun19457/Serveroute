package io.github.railgun19457.serveroute.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.github.railgun19457.serveroute.ConfigManager;
import io.github.railgun19457.serveroute.model.ServerEntry;
import io.github.railgun19457.serveroute.service.ReconnectTracker;
import io.github.railgun19457.serveroute.service.VirtualHosts;
import org.slf4j.Logger;

import java.util.Optional;

public final class InitialServerListener {
    private final ProxyServer proxyServer;
    private final ConfigManager configManager;
    private final ReconnectTracker reconnectTracker;
    private final Logger logger;

    public InitialServerListener(
            ProxyServer proxyServer,
            ConfigManager configManager,
            ReconnectTracker reconnectTracker,
            Logger logger
    ) {
        this.proxyServer = proxyServer;
        this.configManager = configManager;
        this.reconnectTracker = reconnectTracker;
        this.logger = logger;
    }

    @Subscribe
    public void onChooseInitial(PlayerChooseInitialServerEvent event) {
        ReconnectTracker.PendingReconnect pending = reconnectTracker.consume(event.getPlayer().getUniqueId());
        if (pending != null) {
            Optional<RegisteredServer> remembered = proxyServer.getServer(pending.backendName());
            if (remembered.isPresent()) {
                event.setInitialServer(remembered.get());
                logger.info("[Serveroute][reconnect] Restored backend. uuid={} target={}",
                        event.getPlayer().getUniqueId(), pending.backendName());
                return;
            }
            logger.warn("[Serveroute][reconnect] Remembered backend missing. uuid={} target={}",
                    event.getPlayer().getUniqueId(), pending.backendName());
        }

        Optional<String> host = VirtualHosts.of(event.getPlayer());
        if (host.isEmpty()) {
            return;
        }

        Optional<ServerEntry> matched = configManager.runtime().findInternalByHost(host.get());
        if (matched.isEmpty()) {
            return;
        }

        Optional<RegisteredServer> registered = proxyServer.getServer(matched.get().target());
        if (registered.isEmpty()) {
            logger.warn("[Serveroute][host] Internal host matched but backend missing. host={} target={}",
                    host.get(), matched.get().target());
            return;
        }
        event.setInitialServer(registered.get());
        logger.info("[Serveroute][host] Routed initial server. uuid={} host={} target={}",
                event.getPlayer().getUniqueId(), host.get(), matched.get().target());
    }
}
