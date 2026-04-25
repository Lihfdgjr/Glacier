package gg.glacier.config;

import gg.glacier.Glacier;
import org.bukkit.configuration.file.FileConfiguration;

public class GlacierConfig {

    private final Glacier plugin;

    private String alertPrefix;
    private String alertFormat;
    private long alertCooldownMs;
    private boolean logToFile;

    private String bypassPermission;
    private long graceMs;

    public GlacierConfig(Glacier plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration c = plugin.getConfig();
        alertPrefix      = c.getString("alerts.prefix", "&8[&bGlacier&8]&r ");
        alertFormat      = c.getString("alerts.format", "&b{player}&7 failed &b{check}");
        alertCooldownMs  = c.getLong("alerts.cooldown-ms", 250);
        logToFile        = c.getBoolean("alerts.log-to-file", true);
        bypassPermission = c.getString("global.bypass-permission", "glacier.bypass");
        graceMs          = c.getLong("global.grace-period-ms", 4000);
    }

    public String alertPrefix()     { return alertPrefix; }
    public String alertFormat()     { return alertFormat; }
    public long alertCooldownMs()   { return alertCooldownMs; }
    public boolean logToFile()      { return logToFile; }
    public String bypassPermission(){ return bypassPermission; }
    public long graceMs()           { return graceMs; }
}
