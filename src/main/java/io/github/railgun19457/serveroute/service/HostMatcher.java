package io.github.railgun19457.serveroute.service;

import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.Optional;

public final class HostMatcher {
    private HostMatcher() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String host = raw.trim();
        if (host.isEmpty()) {
            return "";
        }

        if (host.startsWith("[") && host.contains("]")) {
            int closing = host.indexOf(']');
            String ipv6 = host.substring(1, closing);
            String remainder = host.substring(closing + 1);
            host = remainder.startsWith(":") ? ipv6 : ipv6 + remainder;
        }

        host = stripTrailingDots(host);
        if (looksLikeHostPort(host)) {
            host = host.substring(0, host.lastIndexOf(':'));
        }
        return stripTrailingDots(host).toLowerCase(Locale.ROOT);
    }

    public static Optional<String> virtualHostOf(Optional<String> rawVirtualHost, Optional<InetSocketAddress> virtualHost) {
        if (rawVirtualHost != null && rawVirtualHost.isPresent()) {
            String normalized = normalize(rawVirtualHost.get());
            if (!normalized.isEmpty()) {
                return Optional.of(normalized);
            }
        }
        if (virtualHost != null && virtualHost.isPresent()) {
            InetSocketAddress address = virtualHost.get();
            String hostname = address.getHostString();
            if (hostname == null || hostname.isBlank()) {
                hostname = address.getHostName();
            }
            String normalized = normalize(hostname);
            if (!normalized.isEmpty()) {
                return Optional.of(normalized);
            }
        }
        return Optional.empty();
    }

    public static boolean matches(String normalizedHost, String domain, String host) {
        if (normalizedHost == null || normalizedHost.isEmpty()) {
            return false;
        }
        String normalizedDomain = normalize(domain);
        if (!normalizedDomain.isEmpty() && normalizedDomain.equals(normalizedHost)) {
            return true;
        }
        String normalizedEntryHost = normalize(host);
        return !normalizedEntryHost.isEmpty() && normalizedEntryHost.equals(normalizedHost);
    }

    public static boolean looksLikeIpv6(String value) {
        return value != null && value.indexOf(':') >= 0 && value.indexOf('.') < 0;
    }

    private static String stripTrailingDots(String host) {
        while (!host.isEmpty() && host.charAt(host.length() - 1) == '.') {
            host = host.substring(0, host.length() - 1);
        }
        return host;
    }

    private static boolean looksLikeHostPort(String host) {
        int lastColon = host.lastIndexOf(':');
        if (lastColon <= 0 || lastColon == host.length() - 1) {
            return false;
        }
        if (looksLikeIpv6(host)) {
            return false;
        }
        String port = host.substring(lastColon + 1);
        for (int i = 0; i < port.length(); i++) {
            if (!Character.isDigit(port.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
