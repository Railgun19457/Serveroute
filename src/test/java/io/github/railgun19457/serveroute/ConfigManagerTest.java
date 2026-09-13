package io.github.railgun19457.serveroute;

import io.github.railgun19457.serveroute.model.RuntimeConfig;
import io.github.railgun19457.serveroute.model.ServerType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerTest {
    @TempDir
    Path dataDirectory;

    @Test
    void loadsValidConfigAndSkipsConflicts() throws Exception {
        Files.writeString(dataDirectory.resolve("config.toml"), """
                [meta]
                config-version = 1

                [command]
                replace-server = true
                line-aliases = ["线路", "node"]

                [transfer]
                min-protocol = 766
                reconnect-timeout-seconds = 30

                [servers.lobby]
                display = "大厅"
                type = "internal"
                target = "lobby"

                [servers.bad]
                display = "坏"
                type = "nope"

                [servers.skyblock]
                display = "空岛"
                type = "transfer"
                host = "skyblock.example.com"
                port = 25565
                domain = "skyblock.example.com"

                [servers.dup]
                display = "空岛"
                type = "transfer"
                host = "dup.example.com"
                port = 25565

                [lines.nodes.jp]
                display = "日本"
                host = "jp.example.com"
                port = 25565
                domain = "jp.example.com"

                [lines.nodes.clash]
                display = "冲突"
                host = "jp.example.com"
                port = 25565
                """, StandardCharsets.UTF_8);
        Files.writeString(dataDirectory.resolve("message.toml"), """
                prefix = "<gray>[Serveroute]</gray> "
                no-permission = "<red>no</red>"
                """, StandardCharsets.UTF_8);

        ConfigManager manager = new ConfigManager(
                LoggerFactory.getLogger("test"),
                dataDirectory,
                Set.of("lobby")::contains
        );
        manager.initialize();

        RuntimeConfig runtime = manager.runtime();
        assertEquals(1, runtime.configVersion());
        assertEquals(2, runtime.servers().size());
        assertEquals(ServerType.INTERNAL, runtime.findServer("大厅").orElseThrow().type());
        assertEquals("skyblock.example.com", runtime.findServer("skyblock").orElseThrow().emitHost());
        assertEquals(1, runtime.lines().size());
        assertTrue(runtime.findLine("日本").isPresent());
        assertTrue(runtime.findLine("clash").isEmpty());
    }

    @Test
    void reloadKeepsOldConfigOnParseFailure() throws Exception {
        Files.writeString(dataDirectory.resolve("config.toml"), """
                [meta]
                config-version = 1
                [servers.lobby]
                display = "大厅"
                type = "internal"
                target = "lobby"
                """, StandardCharsets.UTF_8);
        Files.writeString(dataDirectory.resolve("message.toml"), "prefix = \"ok\"\n", StandardCharsets.UTF_8);

        ConfigManager manager = new ConfigManager(
                LoggerFactory.getLogger("test"),
                dataDirectory,
                name -> true
        );
        manager.initialize();
        assertEquals("大厅", manager.runtime().findServer("lobby").orElseThrow().display());

        Files.writeString(dataDirectory.resolve("config.toml"), "this is not toml [[[", StandardCharsets.UTF_8);
        ConfigManager.ReloadResult result = manager.reload();
        assertTrue(!result.success());
        assertEquals("大厅", manager.runtime().findServer("lobby").orElseThrow().display());
    }
}
