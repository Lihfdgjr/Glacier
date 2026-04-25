package gg.glacier.check.impl.movement;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Predictive horizontal speed.
 * Expected delta per tick = previous-delta * friction(0.91) + sprint input(0.0573).
 * Checks actual delta against that bound with slack for knockback, potions, ice,
 * and sprint-jumping. Flags only when the player sustains impossible delta for
 * multiple ticks (buffered).
 */
public class SpeedB extends Check {

    private int buffer = 0;

    public SpeedB(Glacier plugin, PlayerData data) {
        super(plugin, data, "speed-b", "movement");
    }

    @Override
    public void onMove(PlayerMoveEvent event) {
        Player p = player();
        if (p == null) return;
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        if (p.isFlying() || p.getAllowFlight() || p.isInsideVehicle()) return;

        // Knockback grace
        if (System.currentTimeMillis() - data.lastVelocityMs < 1500) return;

        double expected = data.prevDeltaXZ * 0.91 + 0.0573;

        // Speed potion
        if (p.hasPotionEffect(PotionEffectType.SPEED)) {
            int amp = 0;
            for (PotionEffect e : p.getActivePotionEffects()) {
                if (e.getType().equals(PotionEffectType.SPEED)) { amp = e.getAmplifier() + 1; break; }
            }
            expected *= 1.0 + 0.20 * amp;
        }

        // Jump headroom
        if (data.airTicks > 0 && data.airTicks < 10) expected += 0.08;

        double slack = section() != null ? section().getDouble("slack", 0.03) : 0.03;
        if (data.deltaXZ > expected + slack) {
            buffer = Math.min(buffer + 1, 20);
            if (buffer > 3) {
                flag(String.format("dxz=%.3f expected=%.3f buffer=%d",
                        data.deltaXZ, expected + slack, buffer));
            }
        } else {
            buffer = Math.max(0, buffer - 1);
            reward();
        }
    }
}
