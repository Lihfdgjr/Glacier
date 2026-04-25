package gg.glacier.check.impl.movement;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;

/**
 * Horizontal ground speed cap.
 * Uses a recent-maximum filter that ignores one-frame spikes from velocity knockback
 * or block bounces. Only flags if the player sustains impossible speed for N ticks.
 */
public class SpeedA extends Check {

    private int buffer = 0;

    public SpeedA(Glacier plugin, PlayerData data) {
        super(plugin, data, "speed-a", "movement");
    }

    @Override
    public void onMove(PlayerMoveEvent event) {
        Player p = player();
        if (p == null) return;
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        if (p.isFlying() || p.isInsideVehicle()) return;
        if (p.getAllowFlight()) return;
        long now = System.currentTimeMillis();
        if (now - data.lastVelocityMs < 1500) return; // knockback grace

        double base = section() != null ? section().getDouble("max-xz-per-tick", 0.36) : 0.36;
        // Speed potion allowance
        if (p.hasPotionEffect(PotionEffectType.SPEED)) {
            int amp = 0;
            for (org.bukkit.potion.PotionEffect e : p.getActivePotionEffects()) {
                if (e.getType().equals(PotionEffectType.SPEED)) { amp = e.getAmplifier() + 1; break; }
            }
            base += 0.06 * amp;
        }
        if (data.airTicks > 0) base += 0.10; // sprint-jump headroom

        if (data.deltaXZ > base) {
            buffer = Math.min(buffer + 1, 20);
            if (buffer > 4) {
                flag(String.format("dxz=%.3f max=%.3f buffer=%d", data.deltaXZ, base, buffer));
            }
        } else {
            buffer = Math.max(0, buffer - 1);
            reward();
        }
    }
}
