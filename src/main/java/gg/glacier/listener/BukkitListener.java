package gg.glacier.listener;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import gg.glacier.util.EntityUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

public class BukkitListener implements Listener {

    private final Glacier plugin;

    public BukkitListener(Glacier plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.dataManager().create(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.dataManager().remove(e.getPlayer());
        if (plugin.packetListener() != null) plugin.packetListener().clearForPlayer(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        PlayerData d = plugin.dataManager().get(e.getPlayer());
        if (d != null) d.lastRespawnMs = System.currentTimeMillis();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();
        if (EntityUtil.isNpc(p)) return;
        PlayerData d = plugin.dataManager().get(p);
        if (d == null) return;

        long now = System.currentTimeMillis();
        d.recordClick(now);
        if (d.lastTarget != e.getEntity()) {
            d.lastTargetSwitchMs = now;
            d.lastTarget = e.getEntity();
        }

        // Skip combat checks against NPCs (Citizens / ZNPCs / MythicMobs).
        // Click tracking still records, so resuming on real targets is unaffected.
        if (!EntityUtil.isNpc(e.getEntity())) {
            for (Check c : d.checks()) if (c.enabled()) c.onAttack(e);
        }

        // Must be after onAttack so KillAuraB can see the previous state
        d.flyingSinceLastAttack = false;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        PlayerData d = plugin.dataManager().get(e.getPlayer());
        if (d == null) return;

        d.lastX = e.getFrom().getX(); d.lastY = e.getFrom().getY(); d.lastZ = e.getFrom().getZ();
        d.deltaX = e.getTo().getX() - d.lastX;
        d.deltaY = e.getTo().getY() - d.lastY;
        d.deltaZ = e.getTo().getZ() - d.lastZ;
        d.prevDeltaXZ = d.deltaXZ;
        d.deltaXZ = Math.sqrt(d.deltaX * d.deltaX + d.deltaZ * d.deltaZ);

        if (d.velocityTicksLeft > 0) {
            d.velocityAbsorbedXZ += d.deltaXZ;
            d.velocityTicksLeft--;
        }

        boolean onGround = e.getPlayer().isOnGround();
        d.clientOnGround = onGround;
        if (onGround) { d.groundTicks++; d.airTicks = 0; }
        else          { d.airTicks++;    d.groundTicks = 0; }

        // The packet listener is the canonical source for rotation when ProtocolLib
        // is loaded - it sees pure-rotation packets that don't fire PlayerMoveEvent.
        // Skip the move-event rotation update to avoid double-counting into history.
        if (plugin.packetListener() == null) {
            d.updateRotation(e.getTo().getYaw(), e.getTo().getPitch());
        }

        d.flyingSinceLastAttack = true;

        for (Check c : d.checks()) {
            if (!c.enabled()) continue;
            c.onMove(e);
            if (d.deltaYaw > 0 || d.deltaPitch > 0) c.onRotation();
        }
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(e.getEntity() instanceof Player)) return;
        PlayerData d = plugin.dataManager().get((Player) e.getEntity());
        if (d != null) d.lastFallDamageMs = System.currentTimeMillis();
    }

    @EventHandler
    public void onVelocity(PlayerVelocityEvent e) {
        PlayerData d = plugin.dataManager().get(e.getPlayer());
        if (d == null) return;
        d.lastVelocityMs = System.currentTimeMillis();
        d.pendingVelocityXZ = Math.hypot(e.getVelocity().getX(), e.getVelocity().getZ());
        d.velocityTicksLeft = 3;
        d.velocityAbsorbedXZ = 0.0;
        for (Check c : d.checks()) if (c.enabled()) c.onVelocityApplied();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        PlayerData d = plugin.dataManager().get(e.getPlayer());
        if (d == null) return;
        for (Check c : d.checks()) if (c.enabled()) c.onBlockPlace(e);
        d.lastBlockPlaceMs = System.currentTimeMillis();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        PlayerData d = plugin.dataManager().get(e.getPlayer());
        if (d == null) return;
        for (Check c : d.checks()) if (c.enabled()) c.onBlockBreak(e);
        d.lastBlockBreakMs = System.currentTimeMillis();
    }

    @EventHandler
    public void onInvOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        PlayerData d = plugin.dataManager().get((Player) e.getPlayer());
        if (d == null) return;
        d.inventoryOpen = true;
        d.inventoryOpenMs = System.currentTimeMillis();
    }

    @EventHandler
    public void onInvClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        PlayerData d = plugin.dataManager().get((Player) e.getPlayer());
        if (d == null) return;
        d.inventoryOpen = false;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack is = e.getItem();
        if (is == null) return;
        PlayerData d = plugin.dataManager().get(e.getPlayer());
        if (d == null) return;

        String name = is.getType().name();
        String kind = null;
        long dur = 0L;
        if (name.equals("BOW")) { kind = "bow"; dur = 5000L; }
        else if (name.equals("SHIELD")) { kind = "shield"; dur = 10000L; }
        else if (isEdible(name)) { kind = "food"; dur = 1700L; }
        else if (name.equals("POTION") || name.equals("MILK_BUCKET")) { kind = "potion"; dur = 1700L; }

        if (kind != null) {
            d.usingItemUntilMs = System.currentTimeMillis() + dur;
            d.usingItemKind = kind;
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        PlayerData d = plugin.dataManager().get(e.getPlayer());
        if (d != null) d.usingItemUntilMs = 0L;
    }

    @EventHandler
    public void onShootBow(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        PlayerData d = plugin.dataManager().get((Player) e.getEntity());
        if (d != null) d.usingItemUntilMs = 0L;
    }

    private static boolean isEdible(String n) {
        // Broad keyword match - works across versions (1.8 uses COOKED_BEEF etc.,
        // 1.13+ uses flattened names). Covers vanilla edibles without
        // referencing Material constants that may not exist.
        return n.equals("APPLE") || n.equals("BREAD") || n.equals("COOKIE") ||
               n.equals("MELON") || n.equals("SPIDER_EYE") || n.contains("CARROT") ||
               n.contains("POTATO") || n.contains("PORKCHOP") || n.contains("BEEF") ||
               n.contains("CHICKEN") || n.contains("MUTTON") || n.contains("RABBIT") ||
               n.contains("FISH") || n.contains("COD") || n.contains("SALMON") ||
               n.contains("SOUP") || n.contains("STEW") || n.contains("BEETROOT") ||
               n.contains("ROTTEN") || n.contains("PUMPKIN_PIE") ||
               n.equals("GOLDEN_APPLE") || n.equals("ENCHANTED_GOLDEN_APPLE") ||
               n.equals("GOLDEN_CARROT") || n.equals("CHORUS_FRUIT") ||
               n.contains("BERRIES") || n.contains("KELP") || n.equals("PUFFERFISH") ||
               n.equals("TROPICAL_FISH") || n.contains("HONEY_BOTTLE");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInvClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        PlayerData d = plugin.dataManager().get((Player) e.getWhoClicked());
        if (d == null) return;
        for (Check c : d.checks()) if (c.enabled()) c.onInventoryClick(e);
    }
}
