package io.github.railgun19457.serveroute.service;

import com.velocitypowered.api.proxy.Player;
import io.github.railgun19457.serveroute.ConfigManager;
import io.github.railgun19457.serveroute.Permissions;
import io.github.railgun19457.serveroute.model.LineEntry;
import io.github.railgun19457.serveroute.model.MessageConfig;
import io.github.railgun19457.serveroute.model.RuntimeConfig;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class LineSwitcher {
    private final ConfigManager configManager;
    private final MessageService messages;
    private final ReconnectTracker reconnectTracker;
    private final Logger logger;

    public LineSwitcher(
            ConfigManager configManager,
            MessageService messages,
            ReconnectTracker reconnectTracker,
            Logger logger
    ) {
        this.configManager = configManager;
        this.messages = messages;
        this.reconnectTracker = reconnectTracker;
        this.logger = logger;
    }

    public void list(Player player) {
        RuntimeConfig runtime = configManager.runtime();
        MessageConfig templates = messages.messages();
        Optional<LineEntry> current = currentLine(player, runtime);
        if (current.isEmpty()) {
            player.sendMessage(messages.renderPrefixed(templates.lineUnknownCurrent(), Map.of()));
        }

        List<LineEntry> visible = runtime.visibleLines(entry -> Permissions.canUseLine(player, entry));
        if (visible.isEmpty()) {
            player.sendMessage(messages.renderPrefixed(templates.lineListEmpty(), Map.of()));
            return;
        }

        player.sendMessage(messages.renderPrefixed(templates.lineListHeader(), Map.of()));
        for (LineEntry entry : visible) {
            boolean isCurrent = current.isPresent() && current.get().id().equals(entry.id());
            String template = isCurrent ? templates.lineListEntryCurrent() : templates.lineListEntry();
            player.sendMessage(messages.render(template, Map.of(
                    "display", MessageService.escape(entry.display()),
                    "id", MessageService.escape(entry.id())
            )));
        }
    }

    public void switchTo(Player player, String input) {
        RuntimeConfig runtime = configManager.runtime();
        MessageConfig templates = messages.messages();
        Optional<LineEntry> found = runtime.findLine(input);
        if (found.isEmpty() || !Permissions.canUseLine(player, found.get())) {
            player.sendMessage(messages.renderPrefixed(templates.lineNotFound(), Map.of(
                    "input", MessageService.escape(input)
            )));
            return;
        }

        LineEntry entry = found.get();
        int required = runtime.protocolFor(entry);
        if (player.getProtocolVersion().getProtocol() < required) {
            player.sendMessage(messages.renderPrefixed(templates.lineOldClient(), Map.of()));
            return;
        }

        Optional<LineEntry> current = currentLine(player, runtime);
        if (current.isPresent() && current.get().id().equals(entry.id())) {
            player.sendMessage(messages.renderPrefixed(templates.lineAlreadyOn(), Map.of(
                    "display", MessageService.escape(entry.display())
            )));
            return;
        }

        player.getCurrentServer().ifPresent(connection -> reconnectTracker.remember(
                player.getUniqueId(),
                connection.getServerInfo().getName(),
                TimeUnit.SECONDS.toMillis(runtime.reconnectTimeoutSeconds())
        ));

        String emitHost = entry.host();
        InetSocketAddress address = TransferAddresses.forTransfer(emitHost, entry.port());
        player.sendMessage(messages.renderPrefixed(templates.lineSwitching(), Map.of(
                "display", MessageService.escape(entry.display())
        )));
        logger.info("[Serveroute][line] Transfer. uuid={} id={} host={}:{}",
                player.getUniqueId(), entry.id(), address.getHostString(), address.getPort());
        player.transferToHost(address);
    }

    public List<String> suggest(Player player, String prefix) {
        return configManager.runtime().suggestLines(prefix, entry -> Permissions.canUseLine(player, entry));
    }

    public Optional<LineEntry> currentLine(Player player, RuntimeConfig runtime) {
        return VirtualHosts.of(player).flatMap(runtime::findLineByHost);
    }
}
