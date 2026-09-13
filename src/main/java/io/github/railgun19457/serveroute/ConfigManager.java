package io.github.railgun19457.serveroute;

import io.github.railgun19457.serveroute.model.LineEntry;
import io.github.railgun19457.serveroute.model.MessageConfig;
import io.github.railgun19457.serveroute.model.RuntimeConfig;
import io.github.railgun19457.serveroute.model.ServerEntry;
import io.github.railgun19457.serveroute.model.ServerType;
import io.github.railgun19457.serveroute.service.HostMatcher;
import org.slf4j.Logger;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseError;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class ConfigManager {
    private static final String CONFIG_FILE_NAME = "config.toml";
    private static final String MESSAGE_FILE_NAME = "message.toml";
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_-]+");

    private final Logger logger;
    private final Path dataDirectory;
    private final Predicate<String> registeredServer;

    private volatile RuntimeConfig runtimeConfig;
    private volatile MessageConfig messageConfig;

    public ConfigManager(Logger logger, Path dataDirectory, Predicate<String> registeredServer) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.registeredServer = registeredServer;
    }

    public synchronized void initialize() throws IOException {
        Files.createDirectories(dataDirectory);
        ensureDefaultFile(CONFIG_FILE_NAME);
        ensureDefaultFile(MESSAGE_FILE_NAME);
        ReloadResult result = reloadInternal();
        if (!result.success()) {
            throw new IOException(result.error());
        }
    }

    public synchronized ReloadResult reload() {
        ReloadResult result = reloadInternal();
        if (result.success()) {
            logger.info("[Serveroute][config] Reloaded. config-version={} servers={} lines={}",
                    runtimeConfig.configVersion(),
                    runtimeConfig.servers().size(),
                    runtimeConfig.lines().size());
        } else {
            logger.error("[Serveroute][config] Reload failed, keeping previous snapshot: {}", result.error());
        }
        return result;
    }

    public RuntimeConfig runtime() {
        return Objects.requireNonNull(runtimeConfig, "Runtime config has not been initialized");
    }

    public MessageConfig messages() {
        return Objects.requireNonNull(messageConfig, "Message config has not been initialized");
    }

    private ReloadResult reloadInternal() {
        try {
            ParsedBundle parsed = parseFiles();
            this.runtimeConfig = parsed.runtimeConfig();
            this.messageConfig = parsed.messageConfig();
            return ReloadResult.ok();
        } catch (Exception ex) {
            return ReloadResult.failed(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
        }
    }

    private ParsedBundle parseFiles() throws IOException {
        TomlParseResult configToml = parseToml(dataDirectory.resolve(CONFIG_FILE_NAME));
        TomlParseResult messageToml = parseToml(dataDirectory.resolve(MESSAGE_FILE_NAME));
        return new ParsedBundle(parseRuntimeConfig(configToml), parseMessageConfig(messageToml));
    }

    private RuntimeConfig parseRuntimeConfig(TomlParseResult toml) {
        int configVersion = intValue(toml, "meta.config-version", 1);
        boolean replaceServer = boolValue(toml, "command.replace-server", true);
        List<String> aliases = stringList(toml.getArray("command.line-aliases"), List.of("线路", "node"));
        int minProtocol = intValue(toml, "transfer.min-protocol", 766);
        int reconnectTimeout = Math.max(1, intValue(toml, "transfer.reconnect-timeout-seconds", 30));

        Set<String> claimedHosts = new HashSet<>();
        List<ServerEntry> servers = parseServers(toml.getTable("servers"), claimedHosts);
        List<LineEntry> lines = parseLines(toml.getTable("lines"), claimedHosts);

        return new RuntimeConfig(
                configVersion,
                replaceServer,
                aliases,
                minProtocol,
                reconnectTimeout,
                servers,
                lines
        );
    }

    private List<ServerEntry> parseServers(TomlTable serversTable, Set<String> claimedHosts) {
        List<ServerEntry> servers = new ArrayList<>();
        if (serversTable == null) {
            return servers;
        }

        Set<String> displays = new HashSet<>();
        for (String rawId : childTableNames(serversTable)) {
            TomlTable table = serversTable.getTable(rawId);
            if (table == null) {
                logger.error("[Serveroute][config] Skipping server '{}': expected a table", rawId);
                continue;
            }
            String id = rawId.toLowerCase(Locale.ROOT);
            if (!ID_PATTERN.matcher(id).matches()) {
                logger.error("[Serveroute][config] Skipping server '{}': id must match [a-z0-9_-]+", rawId);
                continue;
            }

            String display = stringValue(table, "display", null);
            if (display == null || display.isBlank()) {
                logger.error("[Serveroute][config] Skipping server '{}': missing display", id);
                continue;
            }
            String displayKey = display.toLowerCase(Locale.ROOT);
            if (!displays.add(displayKey)) {
                logger.warn("[Serveroute][config] Skipping server '{}': display '{}' already used", id, display);
                continue;
            }

            String typeRaw = stringValue(table, "type", null);
            ServerType type = parseServerType(typeRaw);
            if (type == null) {
                logger.error("[Serveroute][config] Skipping server '{}': type must be internal or transfer", id);
                continue;
            }

            String target = blankToNull(stringValue(table, "target", null));
            String host = blankToNull(stringValue(table, "host", null));
            warnLegacyDomain(table, "server", id);
            Integer port = optionalPort(table, "port");
            String permission = blankToNull(stringValue(table, "permission", null));
            Integer minProtocol = optionalInt(table, "min-protocol");
            boolean hidden = boolValue(table, "hidden", false);

            if (type == ServerType.INTERNAL) {
                if (target == null) {
                    logger.error("[Serveroute][config] Skipping server '{}': internal entries require target", id);
                    continue;
                }
                if (!registeredServer.test(target)) {
                    logger.error("[Serveroute][config] Skipping server '{}': backend '{}' is not registered", id, target);
                    continue;
                }
            } else {
                if (host == null) {
                    logger.error("[Serveroute][config] Skipping server '{}': transfer entries require host", id);
                    continue;
                }
                if (port == null) {
                    logger.error("[Serveroute][config] Skipping server '{}': transfer entries require port", id);
                    continue;
                }
            }

            if (!claimHost(claimedHosts, id, "server", host)) {
                continue;
            }

            servers.add(new ServerEntry(id, display, type, target, host, port == null ? 0 : port, permission, minProtocol, hidden));
        }
        return servers;
    }

    private List<LineEntry> parseLines(TomlTable linesTable, Set<String> claimedHosts) {
        List<LineEntry> lines = new ArrayList<>();
        if (linesTable == null) {
            return lines;
        }
        TomlTable nodes = linesTable.getTable("nodes");
        if (nodes == null) {
            return lines;
        }

        Set<String> displays = new HashSet<>();
        for (String rawId : childTableNames(nodes)) {
            TomlTable table = nodes.getTable(rawId);
            if (table == null) {
                logger.error("[Serveroute][config] Skipping line '{}': expected a table", rawId);
                continue;
            }
            String id = rawId.toLowerCase(Locale.ROOT);
            if (!ID_PATTERN.matcher(id).matches()) {
                logger.error("[Serveroute][config] Skipping line '{}': id must match [a-z0-9_-]+", rawId);
                continue;
            }

            String display = stringValue(table, "display", null);
            if (display == null || display.isBlank()) {
                logger.error("[Serveroute][config] Skipping line '{}': missing display", id);
                continue;
            }
            String displayKey = display.toLowerCase(Locale.ROOT);
            if (!displays.add(displayKey)) {
                logger.warn("[Serveroute][config] Skipping line '{}': display '{}' already used", id, display);
                continue;
            }

            String host = blankToNull(stringValue(table, "host", null));
            warnLegacyDomain(table, "line", id);
            Integer port = optionalPort(table, "port");
            String permission = blankToNull(stringValue(table, "permission", null));
            Integer minProtocol = optionalInt(table, "min-protocol");

            if (host == null) {
                logger.error("[Serveroute][config] Skipping line '{}': require host", id);
                continue;
            }
            if (port == null) {
                logger.error("[Serveroute][config] Skipping line '{}': require port 1-65535", id);
                continue;
            }
            if (!claimHost(claimedHosts, id, "line", host)) {
                continue;
            }
            lines.add(new LineEntry(id, display, host, port, permission, minProtocol));
        }
        return lines;
    }

    private boolean claimHost(Set<String> claimedHosts, String id, String kind, String host) {
        String normalized = HostMatcher.normalize(host);
        if (normalized.isEmpty()) {
            return true;
        }
        if (!claimedHosts.add(normalized)) {
            logger.warn("[Serveroute][config] Skipping {} '{}': host '{}' already claimed", kind, id, normalized);
            return false;
        }
        return true;
    }

    private void warnLegacyDomain(TomlTable table, String kind, String id) {
        if (blankToNull(stringValue(table, "domain", null)) != null) {
            logger.warn("[Serveroute][config] {} '{}' still sets 'domain'; it was merged into 'host' and is ignored", kind, id);
        }
    }

    private MessageConfig parseMessageConfig(TomlParseResult toml) {
        return new MessageConfig(
                stringValue(toml, "prefix", "<gray>[Serveroute]</gray> "),
                stringValue(toml, "no-permission", "<red>你没有权限。</red>"),
                stringValue(toml, "players-only", "<red>该命令只能由玩家执行。</red>"),
                stringValue(toml, "server.list-header", "<white>可用服务器：</white>"),
                stringValue(toml, "server.list-entry", "<gray>- <white>{display}</white> <dark_gray>({id})</dark_gray></gray>"),
                stringValue(toml, "server.list-entry-current", "<gray>- <green>{display}</green> <dark_gray>({id})</dark_gray> <aqua>[当前]</aqua></gray>"),
                stringValue(toml, "server.list-empty", "<yellow>没有可加入的服务器。</yellow>"),
                stringValue(toml, "server.not-found", "<red>找不到服务器 <white>{input}</white>。</red>"),
                stringValue(toml, "server.already-connected", "<yellow>你已经在 <white>{display}</white>。</yellow>"),
                stringValue(toml, "server.connecting", "<gray>正在前往 <white>{display}</white>…</gray>"),
                stringValue(toml, "server.internal-missing", "<red>后端 <white>{target}</white> 未在代理中注册。</red>"),
                stringValue(toml, "server.transfer-old-client", "<red>前往 <white>{display}</white> 需要 Minecraft 1.20.5 或更高版本。</red>"),
                stringValue(toml, "server.connect-fail", "<red>无法连接到 <white>{display}</white>：{reason}</red>"),
                stringValue(toml, "line.list-header", "<white>可用线路：</white>"),
                stringValue(toml, "line.list-entry", "<gray>- <white>{display}</white> <dark_gray>({id})</dark_gray></gray>"),
                stringValue(toml, "line.list-entry-current", "<gray>- <green>{display}</green> <dark_gray>({id})</dark_gray> <aqua>[当前]</aqua></gray>"),
                stringValue(toml, "line.list-empty", "<yellow>没有可切换的线路。</yellow>"),
                stringValue(toml, "line.unknown-current", "<gray>无法识别当前线路（请使用域名连接）。</gray>"),
                stringValue(toml, "line.not-found", "<red>找不到线路 <white>{input}</white>。</red>"),
                stringValue(toml, "line.already-on", "<yellow>你已经在 <white>{display}</white> 线路。</yellow>"),
                stringValue(toml, "line.switching", "<gray>正在切换到 <white>{display}</white> 线路…</gray>"),
                stringValue(toml, "line.old-client", "<red>切换线路需要 Minecraft 1.20.5 或更高版本。</red>"),
                stringValue(toml, "admin.reloaded", "<green>配置已重载。</green>"),
                stringValue(toml, "admin.reload-failed", "<red>重载失败，已保留当前配置：{error}</red>")
        );
    }

    private void ensureDefaultFile(String resourceName) throws IOException {
        Path outputPath = dataDirectory.resolve(resourceName);
        if (Files.exists(outputPath)) {
            return;
        }
        try (InputStream input = ConfigManager.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IOException("Missing default resource: " + resourceName);
            }
            Files.copy(input, outputPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private TomlParseResult parseToml(Path path) throws IOException {
        TomlParseResult parseResult = Toml.parse(Files.readString(path, StandardCharsets.UTF_8));
        if (!parseResult.hasErrors()) {
            return parseResult;
        }
        StringBuilder errorBuilder = new StringBuilder();
        for (TomlParseError error : parseResult.errors()) {
            errorBuilder.append("[")
                    .append(error.position())
                    .append("] ")
                    .append(error.getMessage())
                    .append(System.lineSeparator());
        }
        throw new IOException("Invalid TOML in " + path.getFileName() + ": " + errorBuilder);
    }

    private static ServerType parseServerType(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "internal" -> ServerType.INTERNAL;
            case "transfer" -> ServerType.TRANSFER;
            default -> null;
        };
    }

    private static List<String> stringList(TomlArray array, List<String> fallback) {
        if (array == null) {
            return fallback;
        }
        List<String> values = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            Object value = array.get(i);
            if (value instanceof String str && !str.isBlank()) {
                values.add(str);
            }
        }
        return values.isEmpty() ? fallback : values;
    }

    private static boolean boolValue(TomlParseResult toml, String key, boolean defaultValue) {
        Boolean value = toml.getBoolean(key);
        return value == null ? defaultValue : value;
    }

    private static boolean boolValue(TomlTable table, String key, boolean defaultValue) {
        Boolean value = table.getBoolean(key);
        return value == null ? defaultValue : value;
    }

    private static int intValue(TomlParseResult toml, String key, int defaultValue) {
        Long value = toml.getLong(key);
        return value == null ? defaultValue : value.intValue();
    }

    private static Integer optionalInt(TomlTable table, String key) {
        Long value = table.getLong(key);
        return value == null ? null : value.intValue();
    }

    private static Integer optionalPort(TomlTable table, String key) {
        Integer port = optionalInt(table, key);
        if (port == null || port < 1 || port > 65535) {
            return null;
        }
        return port;
    }

    private static String stringValue(TomlParseResult toml, String key, String defaultValue) {
        String value = toml.getString(key);
        return value == null ? defaultValue : value;
    }

    private static String stringValue(TomlTable table, String key, String defaultValue) {
        String value = table.getString(key);
        return value == null ? defaultValue : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Set<String> childTableNames(TomlTable table) {
        Set<String> names = new LinkedHashSet<>();
        for (String key : table.keySet()) {
            String name = key.contains(".") ? key.substring(0, key.indexOf('.')) : key;
            if (table.isTable(name)) {
                names.add(name);
            }
        }
        return names;
    }

    public record ReloadResult(boolean success, String error) {
        public static ReloadResult ok() {
            return new ReloadResult(true, null);
        }

        public static ReloadResult failed(String error) {
            return new ReloadResult(false, error == null ? "unknown error" : error);
        }
    }

    private record ParsedBundle(RuntimeConfig runtimeConfig, MessageConfig messageConfig) {
    }
}
