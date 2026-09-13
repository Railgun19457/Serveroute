package io.github.railgun19457.serveroute.service;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.net.InetSocketAddress;
import java.util.Hashtable;

public final class TransferAddresses {
    private static final int DEFAULT_MINECRAFT_PORT = 25565;

    private TransferAddresses() {
    }

    public static InetSocketAddress unresolved(String host, int port) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Transfer host must not be blank");
        }
        String emitHost = host.trim();
        return InetSocketAddress.createUnresolved(emitHost, port);
    }

    public static InetSocketAddress forTransfer(String host, int configuredPort) {
        int port = configuredPort;
        if (port == DEFAULT_MINECRAFT_PORT) {
            Integer srvPort = lookupMinecraftSrvPort(host);
            if (srvPort != null) {
                port = srvPort;
            }
        }
        return unresolved(host, port);
    }

    static Integer lookupMinecraftSrvPort(String host) {
        if (host == null || host.isBlank()) {
            return null;
        }
        String query = "_minecraft._tcp." + host.trim();
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("java.naming.provider.url", "dns:");
        try {
            DirContext context = new InitialDirContext(env);
            try {
                Attributes attributes = context.getAttributes(query, new String[]{"SRV"});
                Attribute attribute = attributes.get("SRV");
                if (attribute == null || attribute.size() == 0) {
                    return null;
                }
                int bestPriority = Integer.MAX_VALUE;
                int bestPort = -1;
                for (int i = 0; i < attribute.size(); i++) {
                    SrvRecord record = SrvRecord.parse(String.valueOf(attribute.get(i)));
                    if (record == null) {
                        continue;
                    }
                    if (record.priority < bestPriority) {
                        bestPriority = record.priority;
                        bestPort = record.port;
                    }
                }
                return bestPort > 0 ? bestPort : null;
            } finally {
                context.close();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private record SrvRecord(int priority, int port) {
        static SrvRecord parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            String[] parts = raw.trim().split("\\s+");
            if (parts.length < 4) {
                return null;
            }
            try {
                int priority = Integer.parseInt(parts[0]);
                int port = Integer.parseInt(parts[2]);
                if (port < 1 || port > 65535) {
                    return null;
                }
                return new SrvRecord(priority, port);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }
}
