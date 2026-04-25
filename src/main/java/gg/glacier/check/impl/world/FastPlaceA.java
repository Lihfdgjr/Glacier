package gg.glacier.check.impl.world;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Places a block sooner than vanilla allows.
 * Vanilla right-click cooldown is 4 ticks = 200 ms. We default to 100 ms to
 * allow small network jitter and 1.9 combat ticks.
 */
public class FastPlaceA extends Check {

    public FastPlaceA(Glacier plugin, PlayerData data) {
        super(plugin, data, "fastplace-a", "world");
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        long now = System.currentTimeMillis();
        long min = section() != null ? section().getLong("min-interval-ms", 100) : 100L;
        if (data.lastBlockPlaceMs == 0) return;
        long delta = now - data.lastBlockPlaceMs;
        if (delta < min) {
            flag("interval=" + delta + "ms min=" + min + "ms");
        } else {
            reward();
        }
    }
}
