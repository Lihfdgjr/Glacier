package gg.glacier.data;

import gg.glacier.Glacier;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final Glacier plugin;
    private final Map<UUID, PlayerData> data = new ConcurrentHashMap<>();

    public PlayerDataManager(Glacier plugin) {
        this.plugin = plugin;
        // Handle reload-while-players-online
        for (Player p : plugin.getServer().getOnlinePlayers()) create(p);
    }

    public PlayerData create(Player player) {
        PlayerData d = new PlayerData(player.getUniqueId(), plugin);
        d.checks().addAll(plugin.checkManager().instantiate(d));
        data.put(player.getUniqueId(), d);
        return d;
    }

    public PlayerData get(Player player)      { return data.get(player.getUniqueId()); }
    public PlayerData get(UUID uuid)          { return data.get(uuid); }
    public PlayerData remove(Player player)   { return data.remove(player.getUniqueId()); }

    public void clear() { data.clear(); }

    public Iterable<PlayerData> all() { return data.values(); }
}
