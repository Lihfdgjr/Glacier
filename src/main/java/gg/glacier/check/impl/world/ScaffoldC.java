package gg.glacier.check.impl.world;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Below-feet placement without looking down.
 * Placing a block below the player's feet requires the crosshair to intersect
 * that block's top face. The pitch must be sufficiently downward - modules
 * that fake pitch (displaying forward but placing as if down) fail this.
 */
public class ScaffoldC extends Check {

    public ScaffoldC(Glacier plugin, PlayerData data) {
        super(plugin, data, "scaffold-c", "world");
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        Player p = player();
        if (p == null) return;
        if (event.getBlock().getY() >= p.getLocation().getY() - 0.1) { reward(); return; }

        double minPitch = section() != null ? section().getDouble("min-pitch", 30.0) : 30.0;
        if (p.getLocation().getPitch() < minPitch) {
            flag(String.format("pitch=%.1f min=%.1f", p.getLocation().getPitch(), minPitch));
        } else {
            reward();
        }
    }
}
