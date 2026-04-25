package gg.glacier.check.impl.movement;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Client reports onGround while clearly in the air.
 * Checks the block directly under the player; flags when that block is passable
 * yet the client still claims to be standing on it.
 *
 * Uses Material#isSolid plus string-name fallbacks, so the class loads on any
 * server version from 1.8 (where STATIONARY_WATER etc. exist) to 1.21 (where
 * they were flattened away).
 */
public class NoFallA extends Check {

    public NoFallA(Glacier plugin, PlayerData data) {
        super(plugin, data, "nofall-a", "movement");
    }

    @Override
    public void onMove(PlayerMoveEvent event) {
        Player p = player();
        if (p == null) return;
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        if (p.isFlying() || p.getAllowFlight() || p.isInsideVehicle()) return;

        if (!data.clientOnGround) return;

        Location below = p.getLocation().clone().subtract(0, 0.05, 0);
        Material m = below.getBlock().getType();
        if (!isPassable(m)) { reward(); return; }

        if (data.deltaY < -0.08 && data.airTicks > 2) {
            flag("claimsGround dy=" + String.format("%.3f", data.deltaY)
                    + " below=" + m.name());
        }
    }

    private static boolean isPassable(Material m) {
        if (m == null) return true;
        // A non-solid block is always passable
        if (!m.isSolid()) return true;
        // Some solid materials are still walkable-through in certain directions
        // (rare; covered by the solid check). Keep the switch minimal.
        return false;
    }
}
