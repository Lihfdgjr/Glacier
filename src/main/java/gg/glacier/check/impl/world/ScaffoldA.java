package gg.glacier.check.impl.world;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import gg.glacier.util.MathUtil;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Scaffold / bridging.
 * When a player places a block below themselves against a face that points in
 * their direction of travel, they are placing blocks forward while looking
 * forward - impossible without aim assist or scaffold.
 * Normal forward-placement of a wall has the face pointing TOWARD the player
 * (180 degrees from yaw); scaffold has it pointing away (0 degrees from yaw).
 */
public class ScaffoldA extends Check {

    public ScaffoldA(Glacier plugin, PlayerData data) {
        super(plugin, data, "scaffold-a", "world");
    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        Player p = player();
        if (p == null) return;
        // Only inspect below-feet placements (bridging / towering)
        if (event.getBlock().getY() >= p.getLocation().getY()) { reward(); return; }

        BlockFace face = event.getBlockAgainst().getFace(event.getBlockPlaced());
        if (face == null) { reward(); return; }

        double faceYaw = faceToYaw(face);
        float diff = Math.abs(MathUtil.wrapDegrees((float) (p.getLocation().getYaw() - faceYaw)));

        // Small diff = face points same way player is facing = bridging forward
        double maxDiff = section() != null ? section().getDouble("max-yaw-diff", 60.0) : 60.0;
        if (diff < maxDiff) {
            flag(String.format("yawDiff=%.1f face=%s", diff, face));
        } else {
            reward();
        }
    }

    private static double faceToYaw(BlockFace face) {
        switch (face) {
            case NORTH: return 180;
            case SOUTH: return 0;
            case EAST:  return -90;
            case WEST:  return 90;
            default:    return 0;
        }
    }
}
