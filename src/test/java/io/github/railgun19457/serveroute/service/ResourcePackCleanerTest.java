package io.github.railgun19457.serveroute.service;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcePackCleanerTest {
    private static final ResourcePackInfo PACK = (ResourcePackInfo) Proxy.newProxyInstance(
            ResourcePackCleanerTest.class.getClassLoader(),
            new Class<?>[]{ResourcePackInfo.class},
            (proxy, method, args) -> null
    );

    @Test
    void doesNothingWhenDisabled() {
        FakePlayer player = new FakePlayer(ProtocolVersion.MINECRAFT_1_21, List.of(PACK), List.of());
        assertFalse(ResourcePackCleaner.clearBeforeTransfer(player.player(), false, LoggerFactory.getLogger("test")));
        assertFalse(player.cleared);
    }

    @Test
    void clearsAppliedPacksWhenEnabled() {
        FakePlayer player = new FakePlayer(ProtocolVersion.MINECRAFT_1_21, List.of(PACK), List.of());
        assertTrue(ResourcePackCleaner.clearBeforeTransfer(player.player(), true, LoggerFactory.getLogger("test")));
        assertTrue(player.cleared);
    }

    @Test
    void clearsPendingPacksWhenEnabled() {
        FakePlayer player = new FakePlayer(ProtocolVersion.MINECRAFT_1_20_5, List.of(), List.of(PACK));
        assertTrue(ResourcePackCleaner.clearBeforeTransfer(player.player(), true, LoggerFactory.getLogger("test")));
        assertTrue(player.cleared);
    }

    @Test
    void skipsWhenNoPacksAreTracked() {
        FakePlayer player = new FakePlayer(ProtocolVersion.MINECRAFT_1_21, List.of(), List.of());
        assertFalse(ResourcePackCleaner.clearBeforeTransfer(player.player(), true, LoggerFactory.getLogger("test")));
        assertFalse(player.cleared);
    }

    @Test
    void clearsOnTheOneTwentyThreeBoundary() {
        // 1.20.3 起才有「移除资源包」包，无此包时移除请求不会生效。
        FakePlayer player = new FakePlayer(ProtocolVersion.MINECRAFT_1_20_3, List.of(PACK), List.of());
        assertTrue(ResourcePackCleaner.clearBeforeTransfer(player.player(), true, LoggerFactory.getLogger("test")));
        assertTrue(player.cleared);
    }

    @Test
    void skipsClientsOlderThanOneTwentyThree() {
        FakePlayer player = new FakePlayer(ProtocolVersion.MINECRAFT_1_20_2, List.of(PACK), List.of(PACK));
        assertFalse(ResourcePackCleaner.clearBeforeTransfer(player.player(), true, LoggerFactory.getLogger("test")));
        assertFalse(player.cleared);
    }

    private static final class FakePlayer implements InvocationHandler {
        private final ProtocolVersion version;
        private final List<ResourcePackInfo> applied;
        private final List<ResourcePackInfo> pending;
        private final List<String> calls = new ArrayList<>();
        private boolean cleared;

        FakePlayer(ProtocolVersion version, List<ResourcePackInfo> applied, List<ResourcePackInfo> pending) {
            this.version = version;
            this.applied = applied;
            this.pending = pending;
        }

        Player player() {
            return (Player) Proxy.newProxyInstance(
                    ResourcePackCleanerTest.class.getClassLoader(),
                    new Class<?>[]{Player.class},
                    this
            );
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            calls.add(method.getName());
            return switch (method.getName()) {
                case "getProtocolVersion" -> version;
                case "getAppliedResourcePacks" -> applied;
                case "getPendingResourcePacks" -> pending;
                case "clearResourcePacks" -> {
                    cleared = true;
                    yield null;
                }
                case "getUniqueId" -> java.util.UUID.randomUUID();
                case "toString" -> "fake-player";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> null;
            };
        }
    }
}
