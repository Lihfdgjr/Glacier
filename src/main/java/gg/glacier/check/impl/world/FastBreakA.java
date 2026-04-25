package gg.glacier.check.impl.world;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Breaking a block faster than vanilla permits.
 * Creative mode bypassed. We do not compute exact tool tables; we simply flag
 * sub-threshold break intervals on non-instant-break blocks.
 */
public class FastBreakA extends Check {

    public FastBreakA(Glacier plugin, PlayerData data) {
        super(plugin, data, "fastbreak-a", "world");
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = player();
        if (p == null || p.getGameMode() == GameMode.CREATIVE) return;

        long now = System.currentTimeMillis();
        long min = section() != null ? section().getLong("min-interval-ms", 80) : 80L;
        if (data.lastBlockBreakMs == 0) return;
        long delta = now - data.lastBlockBreakMs;
        if (delta < min) {
            flag("interval=" + delta + "ms min=" + min + "ms block=" + event.getBlock().getType());
        } else {
            reward();
        }
    }
}
