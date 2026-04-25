package gg.glacier.check.impl.combat;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import gg.glacier.util.EntityUtil;
import gg.glacier.util.MathUtil;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Distance from the attacker's eye to the closest point of the target's bounding box.
 * Vanilla melee reach is 3.0; we add latency slack (default 3.1) before flagging.
 *
 * Height resolution: uses Entity#getHeight() via reflection when running on
 * 1.9+ (method added there). Falls back to a hard-coded table on 1.8 where the
 * method does not exist.
 */
public class ReachA extends Check {

    public ReachA(Glacier plugin, PlayerData data) {
        super(plugin, data, "reach", "combat");
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event) {
        Player p = player();
        if (p == null) return;
        Entity target = event.getEntity();
        if (target.getWorld() != p.getWorld()) return;

        double baseDist = cfgD("base-distance", 3.0);
        double maxSlack = cfgD("max-latency-slack", 0.8);
        int latMs = data.transactionLatencyMs >= 0 ? data.transactionLatencyMs : data.pingMs;
        // A target moves up to ~0.28 blocks/tick (sprinting). latency/50 ticks * 0.28 blocks = slack.
        double latencySlack = Math.min(maxSlack, Math.max(0, latMs) * 0.28 / 50.0);
        double maxDist = baseDist + latencySlack;

        Location eye = p.getEyeLocation();
        Location to  = target.getLocation();
        double hw = 0.3;
        double maxY = to.getY() + EntityUtil.height(target);

        double cx = MathUtil.clamp(eye.getX(), to.getX() - hw, to.getX() + hw);
        double cy = MathUtil.clamp(eye.getY(), to.getY(), maxY);
        double cz = MathUtil.clamp(eye.getZ(), to.getZ() - hw, to.getZ() + hw);

        double dx = cx - eye.getX(), dy = cy - eye.getY(), dz = cz - eye.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > maxDist) {
            flag(String.format("dist=%.3f max=%.2f", distance, maxDist));
        } else {
            reward();
        }
    }
}
