package io.github.railgun19457.serveroute;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import io.github.railgun19457.serveroute.model.LineEntry;
import io.github.railgun19457.serveroute.model.ServerEntry;
import io.github.railgun19457.serveroute.model.ServerType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionsTest {
    @Test
    void commandPermissionsAllowUndefinedLikeVanilla() {
        CommandSource source = source(Map.of());
        assertTrue(Permissions.canUseServerCommand(source));
        assertTrue(Permissions.canUseLineCommand(source));
    }

    @Test
    void commandPermissionsRejectExplicitDeny() {
        CommandSource source = source(Map.of(
                Permissions.COMMAND_SERVER, Tristate.FALSE,
                Permissions.COMMAND_LINE, Tristate.FALSE
        ));
        assertFalse(Permissions.canUseServerCommand(source));
        assertFalse(Permissions.canUseLineCommand(source));
    }

    @Test
    void velocityPermissionStillUnlocksServerCommand() {
        CommandSource source = source(Map.of(
                Permissions.COMMAND_SERVER, Tristate.FALSE,
                Permissions.VELOCITY_SERVER, Tristate.TRUE
        ));
        assertTrue(Permissions.canUseServerCommand(source));
    }

    @Test
    void entryPermissionRequiresExplicitGrant() {
        CommandSource source = source(Map.of());
        ServerEntry entry = new ServerEntry("lobby", "大厅", ServerType.INTERNAL, "lobby", null, 0, "serveroute.server.lobby", null, false);
        assertFalse(Permissions.canUseServer(source, entry));

        CommandSource granted = source(Map.of("serveroute.server.lobby", Tristate.TRUE));
        assertTrue(Permissions.canUseServer(granted, entry));

        ServerEntry open = new ServerEntry("lobby", "大厅", ServerType.INTERNAL, "lobby", null, 0, null, null, false);
        assertTrue(Permissions.canUseServer(source, open));
    }

    @Test
    void lineEntryPermissionRequiresExplicitGrant() {
        CommandSource source = source(Map.of());
        LineEntry entry = new LineEntry("jp", "日本", "jp.example.com", 25565, "serveroute.line.jp", null);
        assertFalse(Permissions.canUseLine(source, entry));

        CommandSource granted = source(Map.of("serveroute.line.jp", Tristate.TRUE));
        assertTrue(Permissions.canUseLine(granted, entry));
    }

    @Test
    void consoleCanAlwaysReloadButPlayersNeedExplicitGrant() {
        assertTrue(Permissions.canReload(source(Map.of())));

        CommandSource player = player(Map.of());
        assertFalse(Permissions.canReload(player));
        assertTrue(Permissions.canReload(player(Map.of(Permissions.ADMIN_RELOAD, Tristate.TRUE))));
    }

    private static CommandSource source(Map<String, Tristate> permissions) {
        return (CommandSource) proxy(new Class<?>[]{CommandSource.class}, permissions);
    }

    private static CommandSource player(Map<String, Tristate> permissions) {
        return (CommandSource) proxy(new Class<?>[]{CommandSource.class, Player.class}, permissions);
    }

    private static Object proxy(Class<?>[] interfaces, Map<String, Tristate> permissions) {
        return Proxy.newProxyInstance(
                PermissionsTest.class.getClassLoader(),
                interfaces,
                new FakeSubject(permissions)
        );
    }

    private record FakeSubject(Map<String, Tristate> permissions) implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "getPermissionValue" -> permissions.getOrDefault((String) args[0], Tristate.UNDEFINED);
                case "hasPermission" -> permissions.getOrDefault((String) args[0], Tristate.UNDEFINED).asBoolean();
                case "toString" -> "fake-source";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> defaultValue(method.getReturnType());
            };
        }

        private static Object defaultValue(Class<?> type) {
            if (!type.isPrimitive()) {
                return null;
            }
            if (type == boolean.class) {
                return false;
            }
            if (type == void.class) {
                return null;
            }
            return 0;
        }
    }
}
