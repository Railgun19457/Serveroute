package io.github.railgun19457.serveroute.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import io.github.railgun19457.serveroute.Permissions;
import io.github.railgun19457.serveroute.service.LineSwitcher;
import io.github.railgun19457.serveroute.service.MessageService;

import java.util.List;
import java.util.Map;

public final class LineCommand implements SimpleCommand {
    private final MessageService messages;
    private final LineSwitcher lineSwitcher;

    public LineCommand(MessageService messages, LineSwitcher lineSwitcher) {
        this.messages = messages;
        this.lineSwitcher = lineSwitcher;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(messages.renderPrefixed(messages.messages().playersOnly(), Map.of()));
            return;
        }
        if (!Permissions.canUseLineCommand(player)) {
            player.sendMessage(messages.renderPrefixed(messages.messages().noPermission(), Map.of()));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length == 0) {
            lineSwitcher.list(player);
            return;
        }
        lineSwitcher.switchTo(player, String.join(" ", args));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!(invocation.source() instanceof Player player) || !Permissions.canUseLineCommand(player)) {
            return List.of();
        }
        String prefix = invocation.arguments().length == 0 ? "" : String.join(" ", invocation.arguments());
        return lineSwitcher.suggest(player, prefix);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return Permissions.canUseLineCommand(invocation.source());
    }
}
