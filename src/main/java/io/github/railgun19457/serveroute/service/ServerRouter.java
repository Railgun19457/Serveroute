package io.github.railgun19457.serveroute.service;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.github.railgun19457.serveroute.ConfigManager;
import io.github.railgun19457.serveroute.Permissions;
import io.github.railgun19457.serveroute.model.MessageConfig;
import io.github.railgun19457.serveroute.model.RuntimeConfig;
import io.github.railgun19457.serveroute.model.ServerEntry;
import io.github.railgun19457.serveroute.model.ServerType;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ServerRouter {
    private final ProxyServer proxyServer;
    private final ConfigManager configManager;
    private final MessageService messages;
    private final Logger logger;

    public ServerRouter(ProxyServer proxyServer, ConfigManager configManager, MessageService messages, Logger logger) {
        this.proxyServer = proxyServer;
        this.configManager = configManager;
        this.messages = messages;
        this.logger = logger;
    }

    public void list(Player player) {
        RuntimeConfig runtime = configManager.runtime();
        MessageConfig templates = messages.messages();
        List<ServerEntry> visible = runtime.visibleServers(entry -> Permissions.canUseServer(player, entry));
        if (visible.isEmpty()) {
            player.sendMessage(messages.renderPrefixed(templates.serverListEmpty(), Map.of()));
            return;
        }

        String currentTarget = player.getCurrentServer()
                .map(connection -> connection.getServerInfo().getName())
                .orElse("");
        player.sendMessage(messages.renderPrefixed(templates.serverListHeader(), Map.of()));
        for (ServerEntry entry : visible) {
            boolean current = entry.type() == ServerType.INTERNAL && entry.target().equals(currentTarget);
            String template = current ? templates.serverListEntryCurrent() : templates.serverListEntry();
            player.sendMessage(messages.render(template, Map.of(
                    "display", MessageService.escape(entry.display()),
                    "id", MessageService.escape(entry.id())
            )));
        }
    }

    public void connect(Player player, String input) {
        RuntimeConfig runtime = configManager.runtime();
        MessageConfig templates = messages.messages();
        Optional<ServerEntry> found = runtime.findServer(input);
        if (found.isEmpty() || !Permissions.canUseServer(player, found.get())) {
            player.sendMessage(messages.renderPrefixed(templates.serverNotFound(), Map.of(
                    "input", MessageService.escape(input)
            )));
            return;
        }

        ServerEntry entry = found.get();
        if (entry.type() == ServerType.INTERNAL) {
            connectInternal(player, entry, templates);
            return;
        }
        transfer(player, entry, runtime, templates);
    }

    public List<String> suggest(Player player, String prefix) {
        return configManager.runtime().suggestServers(prefix, entry -> Permissions.canUseServer(player, entry));
    }

    private void connectInternal(Player player, ServerEntry entry, MessageConfig templates) {
        Optional<RegisteredServer> registered = proxyServer.getServer(entry.target());
        if (registered.isEmpty()) {
            player.sendMessage(messages.renderPrefixed(templates.serverInternalMissing(), Map.of(
                    "target", MessageService.escape(entry.target())
            )));
            return;
        }

        String current = player.getCurrentServer()
                .map(connection -> connection.getServerInfo().getName())
                .orElse(null);
        if (entry.target().equals(current)) {
            player.sendMessage(messages.renderPrefixed(templates.serverAlreadyConnected(), Map.of(
                    "display", MessageService.escape(entry.display())
            )));
            return;
        }

        player.createConnectionRequest(registered.get()).connect().whenComplete((result, throwable) -> {
            if (throwable != null) {
                player.sendMessage(messages.renderPrefixed(templates.serverConnectFail(), Map.of(
                        "display", MessageService.escape(entry.display()),
                        "reason", MessageService.escape(throwable.getClass().getSimpleName())
                )));
                logger.warn("[Serveroute][server] Internal connect failed. uuid={} id={} target={}",
                        player.getUniqueId(), entry.id(), entry.target(), throwable);
                return;
            }
            handleResult(player, entry, result, templates);
        });
    }

    private void handleResult(Player player, ServerEntry entry, ConnectionRequestBuilder.Result result, MessageConfig templates) {
        switch (result.getStatus()) {
            case SUCCESS -> {
                // Avoid stacking a success line on top of the vanilla transfer screen.
            }
            case ALREADY_CONNECTED -> player.sendMessage(messages.renderPrefixed(templates.serverAlreadyConnected(), Map.of(
                    "display", MessageService.escape(entry.display())
            )));
            case CONNECTION_IN_PROGRESS -> player.sendMessage(messages.renderPrefixed(templates.serverConnectFail(), Map.of(
                    "display", MessageService.escape(entry.display()),
                    "reason", "CONNECTION_IN_PROGRESS"
            )));
            default -> {
                String reason = result.getReasonComponent()
                        .map(MessageService::plain)
                        .filter(text -> !text.isBlank())
                        .orElse(result.getStatus().name());
                player.sendMessage(messages.renderPrefixed(templates.serverConnectFail(), Map.of(
                        "display", MessageService.escape(entry.display()),
                        "reason", MessageService.escape(reason)
                )));
            }
        }
    }

    private void transfer(Player player, ServerEntry entry, RuntimeConfig runtime, MessageConfig templates) {
        int required = runtime.protocolFor(entry);
        if (player.getProtocolVersion().getProtocol() < required) {
            player.sendMessage(messages.renderPrefixed(templates.serverTransferOldClient(), Map.of(
                    "display", MessageService.escape(entry.display())
            )));
            return;
        }

        String emitHost = entry.host();
        InetSocketAddress address = TransferAddresses.forTransfer(emitHost, entry.port());
        player.sendMessage(messages.renderPrefixed(templates.serverConnecting(), Map.of(
                "display", MessageService.escape(entry.display())
        )));
        ResourcePackCleaner.clearBeforeTransfer(player, runtime.clearPackBeforeTransfer(), logger);
        logger.info("[Serveroute][server] Transfer. uuid={} id={} host={}:{}",
                player.getUniqueId(), entry.id(), address.getHostString(), address.getPort());
        player.transferToHost(address);
    }
}
