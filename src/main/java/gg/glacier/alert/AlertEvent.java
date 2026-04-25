package gg.glacier.alert;

import gg.glacier.check.Check;
import org.bukkit.entity.Player;

/** Fired once per accepted alert (after cooldown, after bypass checks). */
public final class AlertEvent {
    public final Player player;
    public final Check check;
    public final String info;
    public final double vl;
    public final long timestampMs = System.currentTimeMillis();

    public AlertEvent(Player player, Check check, String info, double vl) {
        this.player = player;
        this.check = check;
        this.info = info;
        this.vl = vl;
    }
}
