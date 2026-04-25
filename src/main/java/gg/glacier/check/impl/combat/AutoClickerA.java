package gg.glacier.check.impl.combat;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Sustained high CPS.
 * Humans peak around 13-15 CPS; anything consistently above the configured cap is a flag.
 */
public class AutoClickerA extends Check {

    public AutoClickerA(Glacier plugin, PlayerData data) {
        super(plugin, data, "autoclicker-a", "combat");
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event) {
        int limit = section() != null ? section().getInt("cps-limit", 20) : 20;
        int cps = data.clicksInLastMs(1000);
        if (cps > limit) {
            flag("cps=" + cps + " limit=" + limit);
        } else {
            reward();
        }
    }
}
