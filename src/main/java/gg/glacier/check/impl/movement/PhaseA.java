package gg.glacier.check.impl.movement;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Player's body is inside a solid block.
 * NoClip cheats let a player enter blocks they should collide with. We sample
 * feet + head and only flag if both are inside solid, non-passable blocks for
 * two consecutive ticks.
 *
 * Uses string-name matching rather than enum-constant references so the class
 * loads on any server version from 1.8 to 1.21 (Material enum was flattened
 * in 1.13 - many old constants no longer exist).
 */
public class PhaseA extends Check {

    private int buffer = 0;

    public PhaseA(Glacier plugin, PlayerData data) {
        super(plugin, data, "phase-a", "movement");
    }

    @Override
    public void onMove(PlayerMoveEvent event) {
        Player p = player();
        if (p == null) return;
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;

        Block feet = p.getLocation().getBlock();
        Block head = p.getEyeLocation().getBlock();
        if (isInside(feet) && isInside(head)) {
            buffer++;
            if (buffer >= 2) {
                flag("feet=" + feet.getType() + " head=" + head.getType());
            }
        } else {
            buffer = Math.max(0, buffer - 1);
            reward();
        }
    }

    private static boolean isInside(Block b) {
        if (b == null) return false;
        Material m = b.getType();
        if (!m.isSolid()) return false;

        String n = m.name();
        // Partial-collision blocks across all versions
        if (n.contains("SLAB")   || n.contains("STAIRS") ||
            n.contains("FENCE")  || n.contains("WALL")   ||
            n.contains("CARPET") || n.contains("TRAPDOOR")) return false;

        // Legacy 1.8 names and edge cases
        switch (n) {
            case "STEP":
            case "WOOD_STEP":
            case "STONE_SLAB2":
            case "COBBLE_WALL":
            case "SNOW":
            case "SNOW_LAYER":
                return false;
            default:
                return true;
        }
    }
}
