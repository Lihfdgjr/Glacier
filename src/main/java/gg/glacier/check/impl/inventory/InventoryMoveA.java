package gg.glacier.check.impl.inventory;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Rotating / moving while an inventory is open.
 * Vanilla clients lock the camera when a container GUI is up; any large rotation
 * or movement delta during that period usually comes from inventory-move modules.
 */
public class InventoryMoveA extends Check {

    public InventoryMoveA(Glacier plugin, PlayerData data) {
        super(plugin, data, "inventory-move-a", "inventory");
    }

    @Override
    public void onMove(PlayerMoveEvent event) {
        if (!guard()) return;
        double minMove = section() != null ? section().getDouble("min-move-xz", 0.10) : 0.10;
        if (data.deltaXZ > minMove) {
            flag(String.format("dxz=%.3f", data.deltaXZ));
        }
    }

    @Override
    public void onRotation() {
        if (!guard()) return;
        double minRot = section() != null ? section().getDouble("min-rotation", 5.0) : 5.0;
        if (data.deltaYaw > minRot || data.deltaPitch > minRot) {
            flag(String.format("dyaw=%.1f dpitch=%.1f", data.deltaYaw, data.deltaPitch));
        }
    }

    private boolean guard() {
        if (!data.inventoryOpen) { reward(); return false; }
        // Tiny jitter from the open packet itself - ignore
        return System.currentTimeMillis() - data.inventoryOpenMs >= 500;
    }
}
