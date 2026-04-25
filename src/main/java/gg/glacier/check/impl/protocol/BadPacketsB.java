package gg.glacier.check.impl.protocol;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;

/**
 * Flying-packet burst.
 * The vanilla client sends exactly one flying packet per tick (50 ms).
 * Two or three arriving in rapid succession can happen during network bursts,
 * but more than that in a row with sub-15 ms gaps means the client is
 * smuggling extra packets - classic blink / packet-delay exploits.
 */
public class BadPacketsB extends Check {

    private long prevFlyingMs = 0L;

    public BadPacketsB(Glacier plugin, PlayerData data) {
        super(plugin, data, "bad-packets-b", "protocol");
    }

    @Override
    public void onFlyingPacket() {
        long now = System.currentTimeMillis();
        int burstLimit = section() != null ? section().getInt("burst-limit", 3) : 3;
        long gapMs     = section() != null ? section().getLong("min-gap-ms", 15) : 15L;

        if (prevFlyingMs > 0 && now - prevFlyingMs < gapMs) {
            data.flyingBurstCounter++;
            if (data.flyingBurstCounter > burstLimit) {
                flag("burst=" + data.flyingBurstCounter + " gap=" + (now - prevFlyingMs) + "ms");
            }
        } else {
            data.flyingBurstCounter = 0;
        }
        prevFlyingMs = now;
    }
}
