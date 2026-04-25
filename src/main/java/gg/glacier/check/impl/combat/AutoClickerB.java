package gg.glacier.check.impl.combat;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import gg.glacier.util.MathUtil;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.List;

/**
 * Robotic click regularity.
 * Coefficient of variation of inter-click times. Humans sit around 0.15-0.35.
 * Consistent values below the configured threshold (default 0.04) indicate a macro.
 */
public class AutoClickerB extends Check {

    public AutoClickerB(Glacier plugin, PlayerData data) {
        super(plugin, data, "autoclicker-b", "combat");
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event) {
        int minSamples   = cfgI("min-samples", 12);
        double cvTrigger = cfgD("cv-threshold", 0.04);

        if (data.clickTimes.size() < minSamples) return;
        List<Long> deltas = data.clickIntervals();

        double cv = MathUtil.cv(deltas);
        if (cv < cvTrigger) {
            flag(String.format("cv=%.4f samples=%d", cv, deltas.size()));
        } else {
            reward();
        }
    }
}
