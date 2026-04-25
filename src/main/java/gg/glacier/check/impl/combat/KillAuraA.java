package gg.glacier.check.impl.combat;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import gg.glacier.util.MathUtil;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Angle-to-target on hit.
 * KillAura attacks entities outside the player's field of view.
 * We compute the yaw delta between where the player is looking and where the
 * target actually is, and flag when that delta is impossibly large.
 */
public class KillAuraA extends Check {

    public KillAuraA(Glacier plugin, PlayerData data) {
        super(plugin, data, "killaura-a", "combat");
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event) {
        Player p = player();
        if (p == null) return;
        Entity target = event.getEntity();

        double maxYaw = section() != null ? section().getDouble("max-yaw-to-target", 60.0) : 60.0;

        Location eye = p.getEyeLocation();
        double dx = target.getLocation().getX() - eye.getX();
        double dz = target.getLocation().getZ() - eye.getZ();
        double expectedYaw = Math.toDegrees(Math.atan2(-dx, dz));
        float diff = Math.abs(MathUtil.wrapDegrees((float) (eye.getYaw() - expectedYaw)));

        if (diff > maxYaw) {
            flag(String.format("yawDiff=%.1f max=%.1f", diff, maxYaw));
        } else {
            reward();
        }
    }
}
