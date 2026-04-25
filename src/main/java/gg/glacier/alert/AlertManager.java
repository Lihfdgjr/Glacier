package gg.glacier.alert;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.util.ColorUtil;
import gg.glacier.util.EntityUtil;
import gg.glacier.util.MathUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class AlertManager {

    private final Glacier plugin;
    private final Set<UUID> listeners = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> cooldown = new ConcurrentHashMap<>();
    private final List<AlertSink> sinks = new CopyOnWriteArrayList<>();
    private static final ThreadLocal<SimpleDateFormat> LOG_FILE_FMT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
    private static final ThreadLocal<SimpleDateFormat> LOG_LINE_FMT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("HH:mm:ss"));

    public AlertManager(Glacier plugin) {
        this.plugin = plugin;
    }

    public void registerSink(AlertSink s) { sinks.add(s); }

    public boolean toggle(Player p) {
        UUID u = p.getUniqueId();
        if (listeners.contains(u)) { listeners.remove(u); return false; }
        listeners.add(u); return true;
    }

    public boolean isListening(Player p) {
        return listeners.contains(p.getUniqueId());
    }

    public void alert(Check check, String info) {
        Player p = check.player();
        if (p == null) return;

        String key = p.getUniqueId() + ":" + check.id();
        long now = System.currentTimeMillis();
        Long last = cooldown.get(key);
        long cd = plugin.glacierConfig().alertCooldownMs();
        if (last != null && now - last < cd) return;
        cooldown.put(key, now);

        String msg = plugin.glacierConfig().alertPrefix()
                + plugin.glacierConfig().alertFormat()
                .replace("{player}", p.getName())
                .replace("{check}",  check.id())
                .replace("{type}",   check.category())
                .replace("{vl}",     fmt(check.vl()))
                .replace("{max-vl}", String.valueOf(check.maxVl()))
                .replace("{ping}",   String.valueOf(getPing(p)))
                .replace("{tps}",    "20.0")
                .replace("{info}",   info == null ? "" : info);

        String coloured = ColorUtil.color(msg);

        for (UUID u : listeners) {
            Player staff = Bukkit.getPlayer(u);
            if (staff != null && staff.hasPermission("glacier.alerts")) staff.sendMessage(coloured);
        }
        Bukkit.getConsoleSender().sendMessage(coloured);

        if (plugin.glacierConfig().logToFile()) writeLog(p, check, info);

        if (!sinks.isEmpty()) {
            AlertEvent ev = new AlertEvent(p, check, info, check.vl());
            for (AlertSink sink : sinks) {
                try { sink.onAlert(ev); }
                catch (Throwable t) { plugin.getLogger().warning("Alert sink failed: " + t.getMessage()); }
            }
        }
    }

    private static String fmt(double vl) { return MathUtil.formatVl(vl); }
    private static int getPing(Player p) { return EntityUtil.ping(p); }

    private void writeLog(Player p, Check check, String info) {
        File dir = new File(plugin.getDataFolder(), "logs");
        if (!dir.exists() && !dir.mkdirs()) return;
        File file = new File(dir, LOG_FILE_FMT.get().format(new Date()) + ".log");
        try (BufferedWriter w = new BufferedWriter(new FileWriter(file, true))) {
            w.write("[" + LOG_LINE_FMT.get().format(new Date()) + "] "
                    + p.getName() + " " + check.id()
                    + " vl=" + fmt(check.vl())
                    + (info == null ? "" : " " + info));
            w.newLine();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write alert log: " + e.getMessage());
        }
    }
}
