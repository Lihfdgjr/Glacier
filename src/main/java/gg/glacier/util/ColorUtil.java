package gg.glacier.util;

import org.bukkit.ChatColor;

public final class ColorUtil {
    private ColorUtil() {}
    public static String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }
    /** Removes both &-coded and §-coded color sequences. */
    public static String strip(String s) {
        return s == null ? "" : ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', s));
    }
}
