package io.github.railgun19457.serveroute.service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ReconnectTracker {
    public record PendingReconnect(String backendName, long expireAtEpochMs) {
        public boolean expired(long now) {
            return now >= expireAtEpochMs;
        }
    }

    private final ConcurrentHashMap<UUID, PendingReconnect> pending = new ConcurrentHashMap<>();

    public void remember(UUID uuid, String backendName, long timeoutMillis) {
        if (uuid == null || backendName == null || backendName.isBlank()) {
            return;
        }
        long expireAt = System.currentTimeMillis() + Math.max(1L, timeoutMillis);
        pending.put(uuid, new PendingReconnect(backendName, expireAt));
    }

    public PendingReconnect consume(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        PendingReconnect current = pending.remove(uuid);
        if (current == null) {
            return null;
        }
        if (current.expired(System.currentTimeMillis())) {
            return null;
        }
        return current;
    }

    public boolean isPending(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        PendingReconnect current = pending.get(uuid);
        if (current == null) {
            return false;
        }
        if (current.expired(System.currentTimeMillis())) {
            pending.remove(uuid, current);
            return false;
        }
        return true;
    }

    public void forget(UUID uuid) {
        if (uuid != null) {
            pending.remove(uuid);
        }
    }
}
