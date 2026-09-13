package io.github.railgun19457.serveroute.model;

import io.github.railgun19457.serveroute.service.HostMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public record RuntimeConfig(
        int configVersion,
        boolean replaceServer,
        List<String> lineAliases,
        int minProtocol,
        int reconnectTimeoutSeconds,
        List<ServerEntry> servers,
        List<LineEntry> lines
) {
    public RuntimeConfig {
        lineAliases = List.copyOf(lineAliases == null ? List.of() : lineAliases);
        servers = List.copyOf(servers == null ? List.of() : servers);
        lines = List.copyOf(lines == null ? List.of() : lines);
    }

    public Optional<ServerEntry> findServer(String input) {
        return findNamed(servers, input, ServerEntry::id, ServerEntry::display);
    }

    public Optional<LineEntry> findLine(String input) {
        return findNamed(lines, input, LineEntry::id, LineEntry::display);
    }

    public Optional<ServerEntry> findInternalByTarget(String target) {
        if (target == null || target.isBlank()) {
            return Optional.empty();
        }
        for (ServerEntry entry : servers) {
            if (entry.type() == ServerType.INTERNAL && target.equals(entry.target())) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    public Optional<ServerEntry> findInternalByHost(String host) {
        String normalizedHost = HostMatcher.normalize(host);
        if (normalizedHost.isEmpty()) {
            return Optional.empty();
        }
        for (ServerEntry entry : servers) {
            if (entry.type() != ServerType.INTERNAL) {
                continue;
            }
            if (HostMatcher.matches(normalizedHost, entry.host())) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    public Optional<LineEntry> findLineByHost(String host) {
        String normalizedHost = HostMatcher.normalize(host);
        if (normalizedHost.isEmpty()) {
            return Optional.empty();
        }
        for (LineEntry entry : lines) {
            if (HostMatcher.matches(normalizedHost, entry.host())) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    public List<String> suggestServers(String prefix, Predicate<ServerEntry> allowed) {
        String normalizedPrefix = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        Set<String> suggestions = new LinkedHashSet<>();
        for (ServerEntry entry : servers) {
            if (entry.hidden() || !allowed.test(entry)) {
                continue;
            }
            if (startsWith(entry.id(), normalizedPrefix) || startsWith(entry.display(), normalizedPrefix)) {
                suggestions.add(entry.display());
            }
        }
        return new ArrayList<>(suggestions);
    }

    public List<String> suggestLines(String prefix, Predicate<LineEntry> allowed) {
        String normalizedPrefix = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        Set<String> suggestions = new LinkedHashSet<>();
        for (LineEntry entry : lines) {
            if (!allowed.test(entry)) {
                continue;
            }
            if (startsWith(entry.id(), normalizedPrefix) || startsWith(entry.display(), normalizedPrefix)) {
                suggestions.add(entry.display());
            }
        }
        return new ArrayList<>(suggestions);
    }

    public int protocolFor(ServerEntry entry) {
        return entry.minProtocol() == null ? minProtocol : entry.minProtocol();
    }

    public int protocolFor(LineEntry entry) {
        return entry.minProtocol() == null ? minProtocol : entry.minProtocol();
    }

    private static boolean startsWith(String value, String prefix) {
        return value.toLowerCase(Locale.ROOT).startsWith(prefix);
    }

    private static <T> Optional<T> findNamed(
            List<T> entries,
            String input,
            java.util.function.Function<T, String> idGetter,
            java.util.function.Function<T, String> displayGetter
    ) {
        if (input == null) {
            return Optional.empty();
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        for (T entry : entries) {
            if (idGetter.apply(entry).equals(trimmed)) {
                return Optional.of(entry);
            }
        }
        for (T entry : entries) {
            if (idGetter.apply(entry).equalsIgnoreCase(trimmed)) {
                return Optional.of(entry);
            }
        }
        for (T entry : entries) {
            if (displayGetter.apply(entry).equals(trimmed)) {
                return Optional.of(entry);
            }
        }
        for (T entry : entries) {
            if (displayGetter.apply(entry).equalsIgnoreCase(trimmed)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    public List<ServerEntry> visibleServers(Predicate<ServerEntry> allowed) {
        List<ServerEntry> visible = new ArrayList<>();
        for (ServerEntry entry : servers) {
            if (!entry.hidden() && allowed.test(entry)) {
                visible.add(entry);
            }
        }
        return Collections.unmodifiableList(visible);
    }

    public List<LineEntry> visibleLines(Predicate<LineEntry> allowed) {
        List<LineEntry> visible = new ArrayList<>();
        for (LineEntry entry : lines) {
            if (allowed.test(entry)) {
                visible.add(entry);
            }
        }
        return Collections.unmodifiableList(visible);
    }
}
