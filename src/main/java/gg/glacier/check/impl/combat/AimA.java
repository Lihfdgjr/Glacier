package gg.glacier.check.impl.combat;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Instantaneous aim snap on attack.
 * On a hit, the player's rotation within the last packet should fit a human
 * turning speed. Snap-aim cheats produce huge yaw+pitch deltas in a single tick
 * exactly when the attack is registered.
 */
public class AimA extends Check {

    public AimA(Glacier plugin, PlayerData data) {
        super(plugin, data, "aim-a", "combat");
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event) {
        double max = section() != null ? section().getDouble("max-rotation-speed", 40.0) : 40.0;
        double speed = data.deltaYaw + data.deltaPitch;
        if (speed > max) {
            flag(String.format("rotSpeed=%.1f max=%.1f", speed, max));
        } else {
            reward();
        }
    }
}
