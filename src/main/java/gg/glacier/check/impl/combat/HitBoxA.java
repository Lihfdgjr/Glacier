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
 * Raycast from the player's eye along the look direction and test whether
 * the ray actually pierces the target's axis-aligned bounding box. A hit
 * that does not intersect the hitbox, regardless of distance, is a hitbox
 * expansion / silent-aim tell. This complements ReachA (distance only) and
 * KillAuraA (yaw-only angle) with a true 3-D check.
 */
public class HitBoxA extends Check {

    public HitBoxA(Glacier plugin, PlayerData data) {
        super(plugin, data, "hitbox-a", "combat");
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event) {
        Player p = player();
        if (p == null) return;
        Entity target = event.getEntity();
        if (target.getWorld() != p.getWorld()) return;

        double expandSlack = cfgD("slack", 0.15);
        double maxRange    = cfgD("max-range", 4.0);

        Location eye = p.getEyeLocation();
        double yawR   = Math.toRadians(eye.getYaw());
        double pitchR = Math.toRadians(eye.getPitch());
        double dx = -Math.sin(yawR) * Math.cos(pitchR);
        double dy = -Math.sin(pitchR);
        double dz =  Math.cos(yawR) * Math.cos(pitchR);

        Location to = target.getLocation();
        double hw = 0.3 + expandSlack;
        double minY = to.getY() - expandSlack;
        double maxY = to.getY() + EntityUtil.height(target) + expandSlack;

        double t = MathUtil.rayAabbDistance(
                eye.getX(), eye.getY(), eye.getZ(),
                dx, dy, dz,
                to.getX() - hw, minY, to.getZ() - hw,
                to.getX() + hw, maxY, to.getZ() + hw);

        if (t < 0 || t > maxRange) {
            flag(t < 0 ? "ray miss" : String.format("rayDist=%.2f max=%.2f", t, maxRange));
        } else {
            reward();
        }
    }
}
