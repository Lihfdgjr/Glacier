package gg.glacier.hook;

import gg.glacier.Glacier;
import gg.glacier.data.PlayerData;
import gg.glacier.util.MathUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class PlaceholderHook extends PlaceholderExpansion {

    private final Glacier plugin;

    public PlaceholderHook(Glacier plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "glacier";
    }

    @Override
    public String getAuthor() {
        return "Glacier";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer p, String params) {
        if (params == null) return null;

        if (params.equals("checks_count")) {
            PlayerData anyData = null;
            if (p != null && p.isOnline()) {
                anyData = plugin.dataManager().get((Player) p);
            }
            if (anyData != null) {
                return String.valueOf(anyData.checks().size());
            }
            return "0";
        }

        if (p == null || !p.isOnline()) {
            return defaultFor(params);
        }

        Player player = (Player) p;
        PlayerData data = plugin.dataManager().get(player);
        if (data == null) {
            return defaultFor(params);
        }

        switch (params) {
            case "vl_total":      return formatVl(data.totalVl());
            case "vl_worst":      return formatVl(data.worstVl());
            case "vl_worst_name": return data.worstVlId();
            case "ping_tx":       return data.transactionLatencyMs < 0 ? "-" : String.valueOf(data.transactionLatencyMs);
            case "ping_ka":       return String.valueOf(data.pingMs);
        }

        return null;
    }

    private String defaultFor(String params) {
        switch (params) {
            case "vl_total":
            case "vl_worst":
            case "ping_ka":
                return "0";
            case "vl_worst_name":
            case "ping_tx":
                return "-";
            default:
                return null;
        }
    }

    private static String formatVl(double v) { return MathUtil.formatVl(v); }
}
