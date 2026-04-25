package gg.glacier.check.impl.combat;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;

/**
 * Rotation GCD.
 * Live mouse input plus driver sensitivity scaling produces yaw deltas that
 * essentially never share a common divisor. Cheats that pre-compute yaw from a
 * target position generate deltas quantized to a fixed step - the greatest
 * common divisor of a batch of deltas reveals that quantization.
 *
 * Deltas are scaled by 65536 (Minecraft's internal 1/65536-degree rotation
 * resolution) so they can be treated as integers. Flags when the GCD exceeds
 * a conservative bound equivalent to ~1 degree of shared step.
 */
public class KillAuraC extends Check {

    public KillAuraC(Glacier plugin, PlayerData data) {
        super(plugin, data, "killaura-c", "combat");
    }

    @Override
    public void onRotation() {
        int minSamples  = section() != null ? section().getInt("min-samples", 15)      : 15;
        long threshold  = section() != null ? section().getLong("gcd-threshold", 65536): 65536L;

        if (data.yawDeltaScaled.size() < minSamples) return;

        long g = 0;
        long lastAbs = -1;
        boolean allEqual = true;
        for (long v : data.yawDeltaScaled) {
            long a = Math.abs(v);
            if (a == 0) continue;
            if (lastAbs >= 0 && a != lastAbs) allEqual = false;
            lastAbs = a;
            g = g == 0 ? a : gcd(g, a);
        }
        // All-equal deltas are common for truly still / rubberbanding clients - not a cheat signal
        if (allEqual) return;

        if (g > threshold) {
            flag("gcd=" + g + " (" + String.format("%.4f", g / 65536.0) + " deg)");
        }
    }

    private static long gcd(long a, long b) {
        while (b != 0) { long t = b; b = a % b; a = t; }
        return a;
    }
}
