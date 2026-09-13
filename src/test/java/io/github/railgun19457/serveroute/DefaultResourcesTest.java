package io.github.railgun19457.serveroute;

import org.junit.jupiter.api.Test;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultResourcesTest {
    @Test
    void bundledTomlIsValid() throws Exception {
        TomlParseResult config = Toml.parse(readResource("config.toml"));
        TomlParseResult messages = Toml.parse(readResource("message.toml"));
        assertFalse(config.hasErrors(), () -> config.errors().toString());
        assertFalse(messages.hasErrors(), () -> messages.errors().toString());
        assertTrue(config.contains("servers.lobby"));
        assertTrue(messages.contains("prefix"));
    }

    private static String readResource(String name) throws Exception {
        try (InputStream input = DefaultResourcesTest.class.getClassLoader().getResourceAsStream(name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
