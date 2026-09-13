package io.github.railgun19457.serveroute.service;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import org.slf4j.Logger;

import java.util.Collection;

/**
 * 发 Transfer 前清掉客户端身上的服务端资源包。
 *
 * <p>背景：Velocity 上游存在「从带强制资源包的后端 Transfer 会卡住」的问题
 * （PaperMC/Velocity#1139），玩家会停在 configuration 阶段直到 read timed out，
 * 表现之一是代理下发的 Commands 包收不到，{@code /server} 失去补全。
 *
 * <p>用 adventure 的 {@code Audience#clearResourcePacks()}（Velocity 已实现），
 * 它会在 1.20.3+ 客户端上发送「移除全部资源包」包。Transfer 要求 1.20.5+，
 * 所以只要玩家能收到 Transfer，就一定能收到移除包，不存在版本缺口。
 */
public final class ResourcePackCleaner {
    private ResourcePackCleaner() {
    }

    /**
     * @return 是否真的发出了移除包
     */
    public static boolean clearBeforeTransfer(Player player, boolean enabled, Logger logger) {
        if (!enabled) {
            return false;
        }
        if (!player.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
            return false;
        }

        Collection<ResourcePackInfo> applied = player.getAppliedResourcePacks();
        Collection<ResourcePackInfo> pending = player.getPendingResourcePacks();
        int appliedCount = applied == null ? 0 : applied.size();
        int pendingCount = pending == null ? 0 : pending.size();
        if (appliedCount == 0 && pendingCount == 0) {
            return false;
        }

        logger.info("[Serveroute][pack] Clearing server resource packs before transfer. uuid={} applied={} pending={}",
                player.getUniqueId(), appliedCount, pendingCount);
        player.clearResourcePacks();
        return true;
    }
}
