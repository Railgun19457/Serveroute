package io.github.railgun19457.serveroute.command;

import com.velocitypowered.api.command.SimpleCommand;
import io.github.railgun19457.serveroute.ConfigManager;
import io.github.railgun19457.serveroute.Permissions;
import io.github.railgun19457.serveroute.service.MessageService;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AdminCommand implements SimpleCommand {
    private final ConfigManager configManager;
    private final MessageService messages;

    public AdminCommand(ConfigManager configManager, MessageService messages) {
        this.configManager = configManager;
        this.messages = messages;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!Permissions.canReload(invocation.source())) {
            invocation.source().sendMessage(messages.renderPrefixed(messages.messages().noPermission(), Map.of()));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length == 0 || !"reload".equalsIgnoreCase(args[0])) {
            return;
        }

        ConfigManager.ReloadResult result = configManager.reload();
        if (result.success()) {
            messages.apply(configManager.messages());
            invocation.source().sendMessage(messages.renderPrefixed(messages.messages().adminReloaded(), Map.of()));
            return;
        }

        invocation.source().sendMessage(messages.renderPrefixed(messages.messages().adminReloadFailed(), Map.of(
                "error", MessageService.escape(result.error())
        )));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!Permissions.canReload(invocation.source())) {
            return List.of();
        }
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            return "reload".startsWith(prefix) ? List.of("reload") : List.of();
        }
        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return Permissions.canReload(invocation.source());
    }
}
