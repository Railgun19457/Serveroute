package io.github.railgun19457.serveroute.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeConfigTest {
    @Test
    void resolvesIdAndDisplay() {
        RuntimeConfig runtime = new RuntimeConfig(
                1,
                true,
                List.of("线路"),
                766,
                30,
                List.of(new ServerEntry("lobby", "大厅", ServerType.INTERNAL, "lobby", null, 0, null, null, false)),
                List.of(new LineEntry("jp", "日本", "jp.example.com", 25565, null, null))
        );

        assertEquals("lobby", runtime.findServer("大厅").orElseThrow().id());
        assertEquals("jp", runtime.findLine("日本").orElseThrow().id());
        assertEquals("jp", runtime.findLineByHost("JP.example.com:25565").orElseThrow().id());
        assertTrue(runtime.suggestServers("大", entry -> true).contains("大厅"));
        assertTrue(runtime.suggestLines("jp", entry -> true).contains("日本"));
        assertTrue(runtime.suggestServers("lob", entry -> true).contains("大厅"));
        assertFalse(runtime.suggestServers("lob", entry -> true).contains("lobby"));
    }

    @Test
    void hidesHiddenServersFromSuggestions() {
        RuntimeConfig runtime = new RuntimeConfig(
                1,
                true,
                List.of(),
                766,
                30,
                List.of(new ServerEntry("hidden", "隐藏", ServerType.INTERNAL, "hidden", null, 0, null, null, true)),
                List.of()
        );
        assertTrue(runtime.suggestServers("h", entry -> true).isEmpty());
        assertTrue(runtime.visibleServers(entry -> true).isEmpty());
        assertEquals("hidden", runtime.findServer("隐藏").orElseThrow().id());
    }
}
