package io.github.railgun19457.serveroute.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import io.github.railgun19457.serveroute.service.ReconnectTracker;

public final class DisconnectListener {
    private final ReconnectTracker reconnectTracker;

    public DisconnectListener(ReconnectTracker reconnectTracker) {
        this.reconnectTracker = reconnectTracker;
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (reconnectTracker.isPending(event.getPlayer().getUniqueId())) {
            return;
        }
        reconnectTracker.forget(event.getPlayer().getUniqueId());
    }
}
