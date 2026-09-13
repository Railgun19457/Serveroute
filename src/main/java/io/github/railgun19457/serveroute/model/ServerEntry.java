package io.github.railgun19457.serveroute.model;

public record ServerEntry(
        String id,
        String display,
        ServerType type,
        String target,
        String host,
        int port,
        String permission,
        Integer minProtocol,
        boolean hidden
) {
}
