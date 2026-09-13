package io.github.railgun19457.serveroute.model;

public record LineEntry(
        String id,
        String display,
        String host,
        int port,
        String permission,
        Integer minProtocol
) {
}
