package io.github.railgun19457.serveroute.model;

public record LineEntry(
        String id,
        String display,
        String host,
        String domain,
        int port,
        String permission,
        Integer minProtocol
) {
    public String emitHost() {
        if (domain != null && !domain.isBlank()) {
            return domain;
        }
        return host;
    }
}
