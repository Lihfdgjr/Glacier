package gg.glacier.check.impl.player;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;

/**
 * Client-tick rate drift.
 * Server expects one flying packet every ~50 ms. When the client sends them faster,
 * we accumulate a "balance" of surplus ms. Flag if balance crosses balance-limit-ms.
 * Balance is clamped and bleeds back down when packets arrive on time, so transient
 * lag spikes do not cause false positives.
 */
public class TimerA extends Check {

    private long lastPacketMs = 0;

    public TimerA(Glacier plugin, PlayerData data) {
        super(plugin, data, "timer", "player");
    }

    @Override
    public void onFlyingPacket() {
        long now = System.currentTimeMillis();
        if (lastPacketMs == 0) { lastPacketMs = now; return; }

        long delta = now - lastPacketMs;
        lastPacketMs = now;

        // Ignore transient lag (server hitches send bursts on resume)
        if (delta > 300) { data.timerBalanceMs = 0; return; }

        double expected = 50.0;
        data.timerBalanceMs += (expected - delta);
        // Clamp to avoid runaway accumulation
        data.timerBalanceMs = Math.max(-200, Math.min(500, data.timerBalanceMs));

        double limit = section() != null ? section().getDouble("balance-limit-ms", 120) : 120;
        if (data.timerBalanceMs > limit) {
            flag(String.format("balance=%.1fms limit=%.0fms", data.timerBalanceMs, limit));
            data.timerBalanceMs = 0;
        }
    }
}
