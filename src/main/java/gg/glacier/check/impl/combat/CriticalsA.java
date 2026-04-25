package gg.glacier.check.impl.combat;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Forced criticals.
 * A vanilla critical hit requires the attacker to be falling (fallDistance > 0)
 * AND not on ground AND not in water / on a ladder / in a vehicle.
 * "Criticals" cheats flag every hit as a crit without meeting any of those.
 * We flag when the server-tracked fallDistance is > 0 yet the player has been
 * stably on the ground for several ticks - the classic tell-tale.
 */
public class CriticalsA extends Check {

    public CriticalsA(Glacier plugin, PlayerData data) {
        super(plugin, data, "criticals-a", "combat");
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event) {
        Player p = player();
        if (p == null) return;
        if (p.getVehicle() != null) return;
        if (p.isInsideVehicle()) return;

        float fall = p.getFallDistance();
        if (fall <= 0) { reward(); return; }

        // Player is on the ground, not in a liquid, not on a ladder, yet fallDistance > 0
        if (data.groundTicks > 2 && p.isOnGround() && !isInClimbOrLiquid(p)) {
            flag(String.format("fall=%.2f groundTicks=%d", fall, data.groundTicks));
        } else {
            reward();
        }
    }

    private static boolean isInClimbOrLiquid(Player p) {
        String below = p.getLocation().getBlock().getType().name();
        return below.contains("WATER") || below.contains("LAVA")
            || below.contains("LADDER") || below.contains("VINE")
            || below.contains("SCAFFOLDING");
    }
}
