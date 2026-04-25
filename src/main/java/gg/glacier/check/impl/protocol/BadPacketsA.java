package gg.glacier.check.impl.protocol;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;

/**
 * Invalid pitch value.
 * Valid pitch is strictly in [-90, 90]. Some cheat clients send values outside
 * that range to exploit broken anti-cheats or to glitch rendering.
 */
public class BadPacketsA extends Check {

    public BadPacketsA(Glacier plugin, PlayerData data) {
        super(plugin, data, "bad-packets-a", "protocol");
    }

    @Override
    public void onRotation() {
        if (data.pitch < -90f || data.pitch > 90f) {
            flag("pitch=" + data.pitch);
        }
    }
}
