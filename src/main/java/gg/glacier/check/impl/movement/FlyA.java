package gg.glacier.check.impl.movement;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Sustained airtime without a valid reason.
 * A vanilla jump peaks around 12 airticks; anything beyond indicates flight,
 * unless the player is in creative, spectator, riding, flying, or has allow-flight.
 */
public class FlyA extends Check {

    public FlyA(Glacier plugin, PlayerData data) {
        super(plugin, data, "fly-a", "movement");
    }

    @Override
    public void onMove(PlayerMoveEvent event) {
        Player p = player();
        if (p == null) return;
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        if (p.isFlying() || p.getAllowFlight() || p.isInsideVehicle()) return;
        long now = System.currentTimeMillis();
        if (now - data.lastVelocityMs < 2000) return;

        int max = section() != null ? section().getInt("max-air-ticks", 30) : 30;

        // If the player has been in the air way too long AND is not descending, flag.
        if (data.airTicks > max && data.deltaY >= -0.05) {
            flag("airTicks=" + data.airTicks + " dy=" + String.format("%.3f", data.deltaY));
        } else if (data.airTicks <= max) {
            reward();
        }
    }
}
