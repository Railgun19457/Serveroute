package io.github.railgun19457.serveroute.model;

public record MessageConfig(
        String prefix,
        String noPermission,
        String playersOnly,
        String serverListHeader,
        String serverListEntry,
        String serverListEntryCurrent,
        String serverListEmpty,
        String serverNotFound,
        String serverAlreadyConnected,
        String serverConnecting,
        String serverInternalMissing,
        String serverTransferOldClient,
        String serverConnectFail,
        String lineListHeader,
        String lineListEntry,
        String lineListEntryCurrent,
        String lineListEmpty,
        String lineUnknownCurrent,
        String lineNotFound,
        String lineAlreadyOn,
        String lineSwitching,
        String lineOldClient,
        String adminReloaded,
        String adminReloadFailed
) {
    public MessageConfig {
        prefix = normalize(prefix);
        noPermission = normalize(noPermission);
        playersOnly = normalize(playersOnly);
        serverListHeader = normalize(serverListHeader);
        serverListEntry = normalize(serverListEntry);
        serverListEntryCurrent = normalize(serverListEntryCurrent);
        serverListEmpty = normalize(serverListEmpty);
        serverNotFound = normalize(serverNotFound);
        serverAlreadyConnected = normalize(serverAlreadyConnected);
        serverConnecting = normalize(serverConnecting);
        serverInternalMissing = normalize(serverInternalMissing);
        serverTransferOldClient = normalize(serverTransferOldClient);
        serverConnectFail = normalize(serverConnectFail);
        lineListHeader = normalize(lineListHeader);
        lineListEntry = normalize(lineListEntry);
        lineListEntryCurrent = normalize(lineListEntryCurrent);
        lineListEmpty = normalize(lineListEmpty);
        lineUnknownCurrent = normalize(lineUnknownCurrent);
        lineNotFound = normalize(lineNotFound);
        lineAlreadyOn = normalize(lineAlreadyOn);
        lineSwitching = normalize(lineSwitching);
        lineOldClient = normalize(lineOldClient);
        adminReloaded = normalize(adminReloaded);
        adminReloadFailed = normalize(adminReloadFailed);
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }
}
