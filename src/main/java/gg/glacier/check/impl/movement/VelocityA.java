package gg.glacier.check.impl.movement;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Ignored knockback.
 * After a velocity packet is sent, the client is expected to absorb at least
 * min-ratio of the sent horizontal magnitude over the next three ticks.
 * Anti-knockback / velocity cheats reduce that ratio to near zero.
 */
public class VelocityA extends Check {

    public VelocityA(Glacier plugin, PlayerData data) {
        super(plugin, data, "velocity-a", "movement");
    }

    @Override
    public void onMove(PlayerMoveEvent event) {
        Player p = player();
        if (p == null) return;
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;

        // Fire only on the tick the window closes
        if (data.velocityTicksLeft != 0) return;
        if (data.pendingVelocityXZ <= 0.05) return;

        double minRatio = section() != null ? section().getDouble("min-ratio", 0.30) : 0.30;
        double actual   = data.velocityAbsorbedXZ;
        double expected = data.pendingVelocityXZ;
        double ratio    = actual / Math.max(1e-6, expected);

        if (ratio < minRatio) {
            flag(String.format("absorbed=%.3f expected=%.3f ratio=%.2f",
                    actual, expected, ratio));
        } else {
            reward();
        }

        // reset so we don't re-evaluate until next velocity event
        data.pendingVelocityXZ = 0.0;
        data.velocityAbsorbedXZ = 0.0;
    }
}
