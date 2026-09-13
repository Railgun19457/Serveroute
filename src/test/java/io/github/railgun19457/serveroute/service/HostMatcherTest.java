package io.github.railgun19457.serveroute.service;

import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HostMatcherTest {
    @Test
    void stripsPortAndTrailingDot() {
        assertEquals("jp.example.com", HostMatcher.normalize("JP.Example.com:25565."));
        assertEquals("jp.example.com", HostMatcher.normalize("jp.example.com:25565"));
        assertEquals("jp.example.com", HostMatcher.normalize("jp.example.com."));
        assertEquals("jp.example.com", HostMatcher.normalize("  jp.example.com:25565  "));
    }

    @Test
    void keepsIpv6() {
        assertEquals("2001:db8::1", HostMatcher.normalize("[2001:db8::1]:25565"));
        assertEquals("2001:db8::1", HostMatcher.normalize("2001:db8::1"));
    }

    @Test
    void matchesDomainBeforeHost() {
        assertTrue(HostMatcher.matches("sg.example.com", "sg.example.com", "ignored.example.com"));
        assertTrue(HostMatcher.matches("sg.example.com", null, "sg.example.com:25565"));
        assertFalse(HostMatcher.matches("us.example.com", "sg.example.com", "sg.example.com"));
    }

    @Test
    void prefersRawVirtualHost() {
        Optional<String> host = HostMatcher.virtualHostOf(
                Optional.of("JP.example.com:25565"),
                Optional.of(InetSocketAddress.createUnresolved("ignored.example.com", 25565))
        );
        assertEquals(Optional.of("jp.example.com"), host);
    }
}
