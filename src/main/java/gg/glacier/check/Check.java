package gg.glacier.check;

import gg.glacier.Glacier;
import gg.glacier.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public abstract class Check {

    protected final Glacier plugin;
    protected final PlayerData data;

    private final String id;
    private final String category;

    private boolean enabled;
    private int maxVl;
    private String punishment;

    private double vl;

    protected Check(Glacier plugin, PlayerData data, String id, String category) {
        this.plugin = plugin;
        this.data = data;
        this.id = id;
        this.category = category;
        reload();
    }

    public void reload() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("checks." + id);
        if (sec == null) {
            this.enabled = false;
            this.maxVl = 100;
            this.punishment = "";
            return;
        }
        this.enabled    = sec.getBoolean("enabled", true);
        this.maxVl      = sec.getInt("max-vl", 10);
        this.punishment = sec.getString("punishment", "");
    }

    protected final ConfigurationSection section() {
        return plugin.getConfig().getConfigurationSection("checks." + id);
    }

    protected final double cfgD(String key, double def) {
        ConfigurationSection s = section();
        return s == null ? def : s.getDouble(key, def);
    }
    protected final int cfgI(String key, int def) {
        ConfigurationSection s = section();
        return s == null ? def : s.getInt(key, def);
    }
    protected final long cfgL(String key, long def) {
        ConfigurationSection s = section();
        return s == null ? def : s.getLong(key, def);
    }

    protected final void flag(String info) {
        if (data.inGracePeriod()) return;
        Player p = data.player();
        if (p == null) return;
        if (p.hasPermission(plugin.glacierConfig().bypassPermission())) return;

        vl += 1.0;
        plugin.alertManager().alert(this, info);

        if (vl >= maxVl && punishment != null && !punishment.isEmpty()) {
            final String cmd = punishment.replace("{player}", p.getName());
            Bukkit.getScheduler().runTask(plugin, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
            vl = 0;
        }
    }

    protected final void reward() {
        vl = Math.max(0, vl - 0.05);
    }

    /** Package-managed setter used by VlStorage to restore persisted VLs on join. */
    public final void setVl(double v) {
        this.vl = Math.max(0, v);
    }

    // ─── Hooks (called from listeners) ──────────────────────────
    public void onAttack(EntityDamageByEntityEvent event)   {}
    public void onMove(PlayerMoveEvent event)                {}
    public void onRotation()                                 {}
    public void onFlyingPacket()                             {}
    public void onKeepAliveResponse(long rttMs)              {}
    public void onBlockPlace(BlockPlaceEvent event)          {}
    public void onBlockBreak(BlockBreakEvent event)          {}
    public void onInventoryClick(InventoryClickEvent event)  {}
    public void onVelocityApplied()                          {}

    // ─── Accessors ──────────────────────────────────────────────
    public String id()           { return id; }
    public String category()     { return category; }
    public boolean enabled()     { return enabled; }
    public int maxVl()           { return maxVl; }
    public double vl()           { return vl; }
    public PlayerData data()     { return data; }
    public Player player()       { return data.player(); }
}
