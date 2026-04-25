package gg.glacier.check.impl.combat;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import gg.glacier.util.MathUtil;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Collections;
import java.util.List;

/**
 * Bimodal click-pattern detector.
 * Some macros alternate between a burst of rapid clicks and a brief pause,
 * producing a two-humped distribution of inter-click deltas rather than a
 * single smooth hump. A median-split 2-means clustering exposes the gap:
 * when the two cluster centroids are far apart relative to the within-cluster
 * spread, the sample is bimodal.
 */
public class AutoClickerC extends Check {

    public AutoClickerC(Glacier plugin, PlayerData data) {
        super(plugin, data, "autoclicker-c", "combat");
    }

    @Override
    public void onAttack(EntityDamageByEntityEvent event) {
        int minSamples      = cfgI("min-samples", 20);
        double bimodTrigger = cfgD("bimodality-threshold", 5.0);

        if (data.clickTimes.size() < minSamples) return;
        List<Long> deltas = data.clickIntervals();
        if (deltas.size() < minSamples) return;

        Collections.sort(deltas);
        int n = deltas.size();
        int mid = n / 2;
        List<Long> lower = deltas.subList(0, mid);
        List<Long> upper = deltas.subList(mid, n);
        if (lower.isEmpty() || upper.isEmpty()) return;

        double centroidA = MathUtil.mean(lower);
        double centroidB = MathUtil.mean(upper);
        double sigmaA = MathUtil.stdDev(lower);
        double sigmaB = MathUtil.stdDev(upper);

        double gap = centroidB - centroidA;
        double bimodality = gap / (sigmaA + sigmaB + 1.0);
        boolean realGap = centroidA < centroidB / 3.0;

        if (bimodality > bimodTrigger && realGap) {
            flag(String.format("bimod=%.2f a=%.1f b=%.1f samples=%d", bimodality, centroidA, centroidB, n));
        } else {
            reward();
        }
    }
}
