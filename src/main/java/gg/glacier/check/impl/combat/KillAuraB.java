package gg.glacier.check.impl.combat;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Two attacks in the same tick window without a flying packet between them.
 * Vanilla clients send a flying packet every tick; swinging twice without one
 * means the client is bundling attacks, typical of KillAura modules.
 */
public class KillAuraB extends Check {

    public KillAuraB(Glacier plugin, PlayerData data) {
        super(plugin, data, "killaura-b", "combat");
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!data.flyingSinceLastAttack && data.lastAttackMs != 0) {
            flag("noFlyingBetweenAttacks");
        } else {
            reward();
        }
    }
}
