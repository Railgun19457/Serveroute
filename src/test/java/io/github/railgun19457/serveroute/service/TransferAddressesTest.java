package io.github.railgun19457.serveroute.service;

import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferAddressesTest {
    @Test
    void doesNotResolveDns() {
        InetSocketAddress address = TransferAddresses.unresolved("jp.example.com", 25565);
        assertTrue(address.isUnresolved());
        assertEquals("jp.example.com", address.getHostString());
        assertEquals(25565, address.getPort());
    }

    @Test
    void keepsConfiguredPortUntouched() {
        InetSocketAddress address = TransferAddresses.unresolved("example.invalid", 27193);
        assertTrue(address.isUnresolved());
        assertEquals("example.invalid", address.getHostString());
        assertEquals(27193, address.getPort());
    }

    @Test
    void rejectsBlankHost() {
        assertThrows(IllegalArgumentException.class, () -> TransferAddresses.unresolved("   ", 25565));
    }
}
