package io.github.railgun19457.serveroute.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconnectTrackerTest {
    @Test
    void consumeReturnsBackendOnce() {
        ReconnectTracker tracker = new ReconnectTracker();
        UUID uuid = UUID.randomUUID();
        tracker.remember(uuid, "survival", 5_000);

        assertTrue(tracker.isPending(uuid));
        assertEquals("survival", tracker.consume(uuid).backendName());
        assertNull(tracker.consume(uuid));
        assertFalse(tracker.isPending(uuid));
    }

    @Test
    void expiredEntryIsDropped() {
        ReconnectTracker tracker = new ReconnectTracker();
        UUID uuid = UUID.randomUUID();
        tracker.remember(uuid, "survival", 1);
        sleepQuietly(5);
        assertNull(tracker.consume(uuid));
        assertFalse(tracker.isPending(uuid));
    }

    @Test
    void pendingDisconnectDoesNotForget() {
        ReconnectTracker tracker = new ReconnectTracker();
        UUID uuid = UUID.randomUUID();
        tracker.remember(uuid, "survival", 5_000);
        if (!tracker.isPending(uuid)) {
            throw new AssertionError("expected pending reconnect");
        }
        ReconnectTracker.PendingReconnect pending = tracker.consume(uuid);
        assertNotNull(pending);
        assertEquals("survival", pending.backendName());
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
