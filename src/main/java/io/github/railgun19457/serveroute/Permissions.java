package io.github.railgun19457.serveroute;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import io.github.railgun19457.serveroute.model.LineEntry;
import io.github.railgun19457.serveroute.model.ServerEntry;

public final class Permissions {
    public static final String COMMAND_SERVER = "serveroute.command.server";
    public static final String COMMAND_LINE = "serveroute.command.line";
    public static final String ADMIN_RELOAD = "serveroute.admin.reload";
    public static final String VELOCITY_SERVER = "velocity.command.server";

    private Permissions() {
    }

    public static boolean canUseServerCommand(CommandSource source) {
        return source.hasPermission(COMMAND_SERVER) || source.hasPermission(VELOCITY_SERVER);
    }

    public static boolean canUseLineCommand(CommandSource source) {
        return source.hasPermission(COMMAND_LINE);
    }

    public static boolean canReload(CommandSource source) {
        return !(source instanceof Player) || source.hasPermission(ADMIN_RELOAD);
    }

    public static boolean canUseServer(CommandSource source, ServerEntry entry) {
        if (!canUseServerCommand(source)) {
            return false;
        }
        return hasEntryPermission(source, entry.permission());
    }

    public static boolean canUseLine(CommandSource source, LineEntry entry) {
        if (!canUseLineCommand(source)) {
            return false;
        }
        return hasEntryPermission(source, entry.permission());
    }

    private static boolean hasEntryPermission(CommandSource source, String permission) {
        if (permission == null || permission.isBlank()) {
            return true;
        }
        return source.hasPermission(permission);
    }
}
