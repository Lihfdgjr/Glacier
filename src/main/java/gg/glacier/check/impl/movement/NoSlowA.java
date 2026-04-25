package gg.glacier.check.impl.movement;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * NoSlow detection.
 * Vanilla slows the player to ~20% speed while using an item (eat/drink, bow draw, shield block).
 * NoSlow cheats preserve sprint speed during these actions. While usingItemUntilMs is in the
 * future, we cap horizontal movement and flag anything above the threshold.
 */
public class NoSlowA extends Check {

    public NoSlowA(Glacier plugin, PlayerData data) {
        super(plugin, data, "noslow-a", "movement");
    }

    @Override
    public void onMove(PlayerMoveEvent event) {
        long now = System.currentTimeMillis();

        if (now >= data.usingItemUntilMs) {
            reward();
            return;
        }
        if (now - data.lastVelocityMs < 1500) return; // knockback grace

        Player p = player();
        if (p == null) return;
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        if (p.isFlying() || p.getAllowFlight() || p.isInsideVehicle()) return;

        double max = section() != null ? section().getDouble("max-xz", 0.15) : 0.15;

        if (data.deltaXZ > max) {
            flag(String.format("dxz=%.3f max=%.3f kind=%s", data.deltaXZ, max, data.usingItemKind));
        } else {
            reward();
        }
    }
}
