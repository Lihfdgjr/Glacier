package gg.glacier.alert;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.util.ColorUtil;
import gg.glacier.util.MathUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public final class DiscordWebhook implements AlertSink {

    private final Glacier plugin;

    private boolean enabled;
    private String webhookUrl;
    private double minVl;
    private String username;
    private String format;

    public DiscordWebhook(Glacier plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("integrations.discord");
        if (s == null) {
            this.enabled = false;
            this.webhookUrl = "";
            this.minVl = 0.0;
            this.username = "Glacier";
            this.format = "`{player}` failed `{check}` (vl {vl}/{max-vl}) - {info}";
            return;
        }
        this.enabled    = s.getBoolean("enabled", false);
        this.webhookUrl = s.getString("webhook-url", "");
        this.minVl      = s.getDouble("min-vl", 0.0);
        this.username   = s.getString("username", "Glacier");
        this.format     = s.getString("format", "`{player}` failed `{check}` (vl {vl}/{max-vl}) - {info}");
    }

    @Override
    public void onAlert(AlertEvent e) {
        if (!enabled || webhookUrl == null || webhookUrl.isEmpty()) return;
        if (e.vl < minVl) return;

        Player player = e.player;
        Check check = e.check;
        String name = player != null ? player.getName() : "unknown";
        String checkId = check != null ? check.id() : "unknown";
        String category = check != null ? check.category() : "unknown";
        int maxVl = check != null ? check.maxVl() : 0;
        String info = e.info != null ? e.info : "";

        String message = ColorUtil.strip(format
                .replace("{player}", name)
                .replace("{check}", checkId)
                .replace("{vl}", MathUtil.formatVl(e.vl))
                .replace("{max-vl}", Integer.toString(maxVl))
                .replace("{info}", info)
                .replace("{type}", category));

        final String body = "{\"username\":\"" + jsonEscape(username) + "\",\"content\":\"" + jsonEscape(message) + "\"}";
        final String url = webhookUrl;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> send(url, body));
    }

    private void send(String urlStr, String body) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "Glacier-Anticheat");

            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(payload.length);
            try (OutputStream os = conn.getOutputStream()) { os.write(payload); }

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                plugin.getLogger().warning("Discord webhook returned non-2xx response: " + code);
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to post Discord webhook: " + ex.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"':  sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }
}
