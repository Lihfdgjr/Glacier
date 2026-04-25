package gg.glacier.check.impl.world;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Rotation snap immediately before a block place.
 * Several scaffold modules realign yaw/pitch onto the support face the tick
 * before the place packet. Humans either already face the spot or sweep
 * smoothly. We flag when the largest rotation delta within the last 3 ticks
 * exceeds a configurable threshold AND the place is below feet.
 */
public class ScaffoldB extends Check {

    public ScaffoldB(Glacier plugin, PlayerData data) {
        super(plugin, data, "scaffold-b", "world");
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        Player p = player();
        if (p == null) return;
        if (event.getBlock().getY() >= p.getLocation().getY()) { reward(); return; }
        if (data.rotationTotalHistory.isEmpty()) return;

        double threshold = section() != null ? section().getDouble("max-snap", 25.0) : 25.0;

        float maxRecent = 0f;
        int i = 0, window = 3;
        for (java.util.Iterator<Float> it = data.rotationTotalHistory.descendingIterator();
                it.hasNext() && i < window; i++) {
            float v = it.next();
            if (v > maxRecent) maxRecent = v;
        }

        if (maxRecent > threshold) {
            flag(String.format("snap=%.1f threshold=%.1f", maxRecent, threshold));
        } else {
            reward();
        }
    }
}
