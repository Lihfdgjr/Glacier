package gg.glacier.util;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Method;

public final class EntityUtil {

    private static final Method GET_HEIGHT;
    private static final Method GET_PING;

    static {
        Method h = null;
        try { h = Entity.class.getMethod("getHeight"); } catch (NoSuchMethodException ignored) {}
        GET_HEIGHT = h;
        Method g = null;
        try { g = Player.class.getMethod("getPing"); } catch (NoSuchMethodException ignored) {}
        GET_PING = g;
    }

    private EntityUtil() {}

    public static double height(Entity e) {
        if (GET_HEIGHT != null) {
            try {
                Object r = GET_HEIGHT.invoke(e);
                if (r instanceof Number) return ((Number) r).doubleValue();
            } catch (ReflectiveOperationException ignored) {}
        }
        switch (e.getType().name()) {
            case "PLAYER":     return 1.8;
            case "ENDERMAN":   return 2.9;
            case "IRON_GOLEM": return 2.7;
            case "SKELETON":
            case "ZOMBIE":     return 1.95;
            case "CREEPER":    return 1.7;
            case "BLAZE":      return 1.8;
            case "VILLAGER":   return 1.95;
            case "WOLF":
            case "OCELOT":
            case "CAT":        return 0.8;
            case "BAT":
            case "CHICKEN":    return 0.7;
            case "SPIDER":     return 0.9;
            case "SLIME":
            case "MAGMA_CUBE": return 2.04;
            default:           return 1.8;
        }
    }

    /** Return ping in ms. Uses Bukkit Player#getPing() on 1.17+, NMS reflection on older. */
    public static int ping(Player p) {
        if (GET_PING != null) {
            try {
                Object r = GET_PING.invoke(p);
                if (r instanceof Number) return ((Number) r).intValue();
            } catch (ReflectiveOperationException ignored) {}
        }
        try {
            Object handle = p.getClass().getMethod("getHandle").invoke(p);
            return handle.getClass().getField("ping").getInt(handle);
        } catch (ReflectiveOperationException ignored) { return 0; }
    }

    /**
     * True for entities flagged as Citizens / similar fake-player NPCs.
     * Citizens sets a "NPC" metadata key on every NPC entity; the same convention
     * is used by ZNPCs, MythicMobs Mob NPCs, and most other NPC plugins.
     */
    public static boolean isNpc(org.bukkit.entity.Entity e) {
        return e != null && e.hasMetadata("NPC");
    }

    public static int speedAmplifier(Player p) {
        if (!p.hasPotionEffect(PotionEffectType.SPEED)) return 0;
        for (PotionEffect e : p.getActivePotionEffects()) {
            if (e.getType().equals(PotionEffectType.SPEED)) return e.getAmplifier() + 1;
        }
        return 0;
    }
}
