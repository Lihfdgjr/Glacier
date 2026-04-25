package gg.glacier;

import gg.glacier.alert.AlertManager;
import gg.glacier.alert.DiscordWebhook;
import gg.glacier.check.Check;
import gg.glacier.check.CheckManager;
import gg.glacier.command.GlacierCommand;
import gg.glacier.config.GlacierConfig;
import gg.glacier.data.PlayerData;
import gg.glacier.data.PlayerDataManager;
import gg.glacier.hook.PlaceholderHook;
import gg.glacier.listener.BukkitListener;
import gg.glacier.listener.PacketListener;
import gg.glacier.storage.VlStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;

public final class Glacier extends JavaPlugin {

    private static Glacier instance;

    private GlacierConfig glacierConfig;
    private PlayerDataManager dataManager;
    private CheckManager checkManager;
    private AlertManager alertManager;
    private PacketListener packetListener;
    private BukkitTask transactionTask;
    private BukkitTask persistenceTask;
    private VlStorage vlStorage;
    private PlaceholderHook placeholderHook;
    private DiscordWebhook discordWebhook;

    public PacketListener packetListener() { return packetListener; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.glacierConfig = new GlacierConfig(this);
        this.alertManager = new AlertManager(this);
        this.checkManager = new CheckManager(this);
        this.dataManager = new PlayerDataManager(this);

        Bukkit.getPluginManager().registerEvents(new BukkitListener(this), this);

        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            this.packetListener = new PacketListener(this);
            this.packetListener.register();
            // Every 5 ticks, probe each player with a transaction packet for sub-tick latency.
            this.transactionTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                if (!packetListener.transactionsAvailable()) return;
                for (Player p : Bukkit.getOnlinePlayers()) packetListener.sendTransaction(p);
            }, 40L, 5L);
            getLogger().info("Hooked into ProtocolLib - packet checks + transaction lag-comp active.");
        } else {
            getLogger().warning("ProtocolLib not found - packet-level checks disabled.");
        }

        GlacierCommand cmd = new GlacierCommand(this);
        getCommand("glacier").setExecutor(cmd);
        getCommand("glacier").setTabCompleter(cmd);

        // ─── Persistence ─────────────────────────────────────────────
        if (getConfig().getBoolean("integrations.persistence.enabled", true)) {
            this.vlStorage = new VlStorage(this);
            Bukkit.getPluginManager().registerEvents(new PersistenceListener(), this);
            long periodicSec = getConfig().getLong("integrations.persistence.periodic-save-seconds", 300);
            this.persistenceTask = Bukkit.getScheduler().runTaskTimer(this, this::snapshotAllOnline,
                    periodicSec * 20L, periodicSec * 20L);
        }

        // ─── Discord webhook ─────────────────────────────────────────
        this.discordWebhook = new DiscordWebhook(this);
        alertManager.registerSink(discordWebhook);

        // ─── PlaceholderAPI ──────────────────────────────────────────
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                this.placeholderHook = new PlaceholderHook(this);
                placeholderHook.register();
                getLogger().info("Hooked into PlaceholderAPI.");
            } catch (Throwable t) {
                getLogger().warning("PlaceholderAPI hook failed: " + t.getMessage());
            }
        }

        getLogger().info("Glacier enabled. " + checkManager.count() + " checks registered.");
    }

    private void snapshotAllOnline() {
        if (vlStorage == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData d = dataManager.get(p);
            if (d == null) continue;
            for (Check c : d.checks()) if (c.vl() > 0) vlStorage.recordVl(p.getUniqueId(), c.id(), c.vl());
            vlStorage.purgeZero(p.getUniqueId());
        }
    }

    private final class PersistenceListener implements Listener {
        @EventHandler
        public void onJoin(PlayerJoinEvent e) {
            PlayerData d = dataManager.get(e.getPlayer());
            if (d == null || vlStorage == null) return;
            Map<String, Double> saved = vlStorage.getSavedVls(e.getPlayer().getUniqueId());
            if (saved.isEmpty()) return;
            for (Check c : d.checks()) {
                Double v = saved.get(c.id());
                if (v != null) c.setVl(v);
            }
        }
        @EventHandler
        public void onQuit(PlayerQuitEvent e) {
            PlayerData d = dataManager.get(e.getPlayer());
            if (d == null || vlStorage == null) return;
            for (Check c : d.checks())
                if (c.vl() > 0) vlStorage.recordVl(e.getPlayer().getUniqueId(), c.id(), c.vl());
            vlStorage.purgeZero(e.getPlayer().getUniqueId());
        }
    }

    @Override
    public void onDisable() {
        if (transactionTask != null) transactionTask.cancel();
        if (persistenceTask != null) persistenceTask.cancel();
        if (packetListener != null) packetListener.unregister();
        if (placeholderHook != null) {
            try { placeholderHook.unregister(); } catch (Throwable ignored) {}
        }
        if (vlStorage != null) { snapshotAllOnline(); vlStorage.shutdown(); }
        if (dataManager != null) dataManager.clear();
    }

    public void reload() {
        reloadConfig();
        glacierConfig.reload();
        checkManager.reloadAll();
        if (discordWebhook != null) discordWebhook.reload();
    }

    public static Glacier get() { return instance; }
    public GlacierConfig glacierConfig() { return glacierConfig; }
    public PlayerDataManager dataManager() { return dataManager; }
    public CheckManager checkManager() { return checkManager; }
    public AlertManager alertManager() { return alertManager; }
}
