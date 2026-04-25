package gg.glacier.check.impl.player;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;

/**
 * Transaction-latency-compensated timer.
 * Complements TimerA: instead of treating the 50 ms flying-packet expectation as fixed,
 * we absorb a fraction of the measured transaction RTT as legitimate jitter. This keeps
 * false positives down on laggy networks while still catching genuine tick-rate drift.
 */
public class TimerB extends Check {

    private long lastAckMs = 0;
    private double balance = 0;

    public TimerB(Glacier plugin, PlayerData data) {
        super(plugin, data, "timer-b", "player");
    }

    @Override
    public void onFlyingPacket() {
        long now = System.currentTimeMillis();
        if (lastAckMs == 0) { lastAckMs = now; return; }

        long delta = now - lastAckMs;
        lastAckMs = now;

        if (delta > 300) { balance = 0; return; }
        if (data.transactionLatencyMs < 0) return;

        double jitterAllowance = section() != null ? section().getDouble("jitter-allowance", 0.5) : 0.5;
        double balanceLimit = section() != null ? section().getDouble("balance-limit-ms", 80.0) : 80.0;

        double expected = 50.0 + data.transactionLatencyMs * jitterAllowance;
        balance += (expected - delta);
        balance = Math.max(-200, Math.min(500, balance));

        if (balance > balanceLimit) {
            flag(String.format("balance=%.1fms limit=%.0fms rtt=%dms", balance, balanceLimit, data.transactionLatencyMs));
            balance = 0;
            return;
        }

        if (delta >= expected + 15) {
            reward();
        }
    }
}
