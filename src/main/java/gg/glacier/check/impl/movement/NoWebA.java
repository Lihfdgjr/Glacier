package gg.glacier.check.impl.movement;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Cobweb speed bypass.
 * A player standing in cobweb moves at about 0.0125 blocks per tick. NoWeb
 * cheats ignore the block drag entirely. Flags any horizontal delta above a
 * configurable threshold while the block at the player's feet is a cobweb.
 *
 * Name-matches WEB (1.8) or COBWEB (1.13+) so it works across all versions.
 */
public class NoWebA extends Check {

    public NoWebA(Glacier plugin, PlayerData data) {
        super(plugin, data, "noweb-a", "movement");
    }

    @Override
    public void onMove(PlayerMoveEvent event) {
        Player p = player();
        if (p == null) return;
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        if (p.isFlying() || p.isInsideVehicle()) return;

        Material m = p.getLocation().getBlock().getType();
        String n = m.name();
        if (!n.equals("WEB") && !n.equals("COBWEB")) { reward(); return; }

        double max = section() != null ? section().getDouble("max-xz", 0.03) : 0.03;
        if (data.deltaXZ > max) {
            flag(String.format("dxz=%.3f max=%.3f", data.deltaXZ, max));
        }
    }
}
