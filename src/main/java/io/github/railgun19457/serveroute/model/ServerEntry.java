package io.github.railgun19457.serveroute.model;

public record ServerEntry(
        String id,
        String display,
        ServerType type,
        String target,
        String host,
        String domain,
        int port,
        String permission,
        Integer minProtocol,
        boolean hidden
) {
    public String emitHost() {
        if (domain != null && !domain.isBlank()) {
            return domain;
        }
        return host;
    }
}
