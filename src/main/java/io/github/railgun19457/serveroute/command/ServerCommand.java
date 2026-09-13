package io.github.railgun19457.serveroute.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import io.github.railgun19457.serveroute.Permissions;
import io.github.railgun19457.serveroute.service.MessageService;
import io.github.railgun19457.serveroute.service.ServerRouter;

import java.util.List;
import java.util.Map;

public final class ServerCommand implements SimpleCommand {
    private final MessageService messages;
    private final ServerRouter serverRouter;

    public ServerCommand(MessageService messages, ServerRouter serverRouter) {
        this.messages = messages;
        this.serverRouter = serverRouter;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(messages.renderPrefixed(messages.messages().playersOnly(), Map.of()));
            return;
        }
        if (!Permissions.canUseServerCommand(player)) {
            player.sendMessage(messages.renderPrefixed(messages.messages().noPermission(), Map.of()));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length == 0) {
            serverRouter.list(player);
            return;
        }
        serverRouter.connect(player, String.join(" ", args));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!(invocation.source() instanceof Player player) || !Permissions.canUseServerCommand(player)) {
            return List.of();
        }
        String prefix = invocation.arguments().length == 0 ? "" : String.join(" ", invocation.arguments());
        return serverRouter.suggest(player, prefix);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return Permissions.canUseServerCommand(invocation.source());
    }
}
