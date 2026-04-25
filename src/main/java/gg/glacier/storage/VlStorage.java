package gg.glacier.storage;

import gg.glacier.Glacier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Persistent storage for per-player, per-check violation levels.
 *
 * <p>Backed by {@code plugins/Glacier/vl.yml} with the layout:
 * <pre>
 * players:
 *   &lt;uuid&gt;:
 *     &lt;check-id&gt;: &lt;double&gt;
 * </pre>
 *
 * <p>All mutating operations are cheap and thread-safe; writes to disk are
 * debounced to at most once per 10 seconds and performed asynchronously.
 */
public final class VlStorage {

    private static final long SAVE_DEBOUNCE_MS = 10_000L;
    private static final String PLAYERS_KEY = "players";

    private final Glacier plugin;
    private final File file;
    private final Map<UUID, Map<String, Double>> vls = new ConcurrentHashMap<>();
    private final Object saveLock = new Object();
    private final AtomicBoolean saveScheduled = new AtomicBoolean(false);

    private volatile long nextSaveAllowed = 0L;

    public VlStorage(Glacier plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "vl.yml");
        load();
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = config.getConfigurationSection(PLAYERS_KEY);
        if (players == null) {
            return;
        }
        for (String uuidKey : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidKey);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            ConfigurationSection checks = players.getConfigurationSection(uuidKey);
            if (checks == null) {
                continue;
            }
            Map<String, Double> perCheck = new ConcurrentHashMap<>();
            for (String checkId : checks.getKeys(false)) {
                perCheck.put(checkId, checks.getDouble(checkId));
            }
            if (!perCheck.isEmpty()) {
                vls.put(uuid, perCheck);
            }
        }
    }

    /**
     * Record a VL observation for a player and check. Triggers a debounced
     * asynchronous save.
     */
    public void recordVl(UUID uuid, String checkId, double vl) {
        vls.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(checkId, vl);
        scheduleSave();
    }

    /**
     * Returns an unmodifiable snapshot of the saved VLs for a player, or an
     * empty map if no data is stored.
     */
    public Map<String, Double> getSavedVls(UUID uuid) {
        Map<String, Double> perCheck = vls.get(uuid);
        if (perCheck == null || perCheck.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new HashMap<>(perCheck));
    }

    /** Remove any entries for the player whose VL is zero or negative. */
    public void purgeZero(UUID uuid) {
        Map<String, Double> perCheck = vls.get(uuid);
        if (perCheck == null) {
            return;
        }
        perCheck.values().removeIf(v -> v == null || v <= 0.0);
        if (perCheck.isEmpty()) {
            vls.remove(uuid);
        }
        scheduleSave();
    }

    /**
     * Schedule an asynchronous save. If a save is already pending or the
     * debounce window hasn't elapsed, this is a no-op (aside from marking a
     * pending save).
     */
    private void scheduleSave() {
        long now = System.currentTimeMillis();
        if (now < nextSaveAllowed) {
            return;
        }
        if (!saveScheduled.compareAndSet(false, true)) {
            return;
        }
        nextSaveAllowed = now + SAVE_DEBOUNCE_MS;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                save();
            } finally {
                saveScheduled.set(false);
            }
        });
    }

    /** Write the current in-memory map to disk. Safe to call from any thread. */
    public void save() {
        synchronized (saveLock) {
            YamlConfiguration config = new YamlConfiguration();
            for (Map.Entry<UUID, Map<String, Double>> playerEntry : vls.entrySet()) {
                Map<String, Double> perCheck = playerEntry.getValue();
                if (perCheck == null || perCheck.isEmpty()) {
                    continue;
                }
                String base = PLAYERS_KEY + "." + playerEntry.getKey();
                for (Map.Entry<String, Double> checkEntry : perCheck.entrySet()) {
                    config.set(base + "." + checkEntry.getKey(), checkEntry.getValue());
                }
            }
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try {
                config.save(file);
            } catch (IOException ex) {
                plugin.getLogger().warning("Failed to save vl.yml: " + ex.getMessage());
            }
        }
    }

    /** Force an immediate synchronous save. Call from onDisable. */
    public void shutdown() {
        save();
    }
}
