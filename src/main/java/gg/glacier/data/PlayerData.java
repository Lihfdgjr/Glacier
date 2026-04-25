package gg.glacier.data;

import gg.glacier.Glacier;
import gg.glacier.check.Check;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private final Glacier plugin;
    private final List<Check> checks = new ArrayList<>();

    public final long joinTime = System.currentTimeMillis();
    public long lastRespawnMs = System.currentTimeMillis();

    // ─── Combat ────────────────────────────────────────────
    public final Deque<Long> clickTimes = new ArrayDeque<>();
    public long lastAttackMs = 0L;
    public Entity lastTarget = null;
    public long lastTargetSwitchMs = 0L;
    public boolean flyingSinceLastAttack = true;

    // ─── Rotation ──────────────────────────────────────────
    public float yaw, pitch;
    public float lastYaw, lastPitch;
    public float deltaYaw, deltaPitch;

    // ─── Movement ──────────────────────────────────────────
    public double lastX, lastY, lastZ;
    public double deltaX, deltaY, deltaZ;
    public double deltaXZ, prevDeltaXZ;
    public int airTicks = 0;
    public int groundTicks = 0;
    public boolean clientOnGround = true;
    public long lastFallDamageMs = 0L;
    public long lastVelocityMs = 0L;

    // ─── Velocity (knockback tracking) ─────────────────────
    public double pendingVelocityXZ = 0.0;
    public int velocityTicksLeft = 0;
    public double velocityAbsorbedXZ = 0.0;

    // ─── Timer ─────────────────────────────────────────────
    public long lastFlyingPacketMs = 0L;
    public double timerBalanceMs = 0.0;
    public int flyingBurstCounter = 0;

    // ─── Inventory ─────────────────────────────────────────
    public boolean inventoryOpen = false;
    public long inventoryOpenMs = 0L;

    // ─── World interaction ─────────────────────────────────
    public long lastBlockPlaceMs = 0L;
    public long lastBlockBreakMs = 0L;

    // ─── Item use (NoSlowA) ────────────────────────────────
    public long usingItemUntilMs = 0L;
    public String usingItemKind = "";

    // ─── Network ───────────────────────────────────────────
    public int pingMs = 0;
    public int transactionLatencyMs = -1;   // -1 = never measured

    // ─── Rotation history (ScaffoldB / KillAuraC) ──────────
    public final Deque<Float> rotationTotalHistory  = new ArrayDeque<>(); // last 10 (dyaw+dpitch) totals
    public final Deque<Long>  yawDeltaScaled        = new ArrayDeque<>(); // last 20 yaw deltas * 65536

    public PlayerData(UUID uuid, Glacier plugin) {
        this.uuid = uuid;
        this.plugin = plugin;
    }

    public UUID uuid() { return uuid; }
    public Player player() { return plugin.getServer().getPlayer(uuid); }
    public List<Check> checks() { return checks; }

    public boolean inGracePeriod() {
        long graceMs = plugin.glacierConfig().graceMs();
        long now = System.currentTimeMillis();
        return now - joinTime < graceMs || now - lastRespawnMs < graceMs;
    }

    public void recordClick(long now) {
        clickTimes.addLast(now);
        while (clickTimes.size() > 40) clickTimes.pollFirst();
        lastAttackMs = now;
    }

    public int clicksInLastMs(long window) {
        long now = System.currentTimeMillis();
        int n = 0;
        for (long t : clickTimes) if (now - t <= window) n++;
        return n;
    }

    public java.util.List<Long> clickIntervals() {
        if (clickTimes.size() < 2) return java.util.Collections.emptyList();
        java.util.List<Long> out = new java.util.ArrayList<>(clickTimes.size() - 1);
        java.util.Iterator<Long> it = clickTimes.iterator();
        long prev = it.next();
        while (it.hasNext()) { long cur = it.next(); out.add(cur - prev); prev = cur; }
        return out;
    }

    public void updateRotation(float newYaw, float newPitch) {
        lastYaw = yaw;
        lastPitch = pitch;
        yaw = newYaw;
        pitch = newPitch;
        deltaYaw = Math.abs(newYaw - lastYaw);
        deltaPitch = Math.abs(newPitch - lastPitch);
        if (deltaYaw > 0 || deltaPitch > 0) recordRotationDelta(deltaYaw, deltaPitch);
    }

    public void recordRotationDelta(float dYaw, float dPitch) {
        rotationTotalHistory.addLast(dYaw + dPitch);
        while (rotationTotalHistory.size() > 10) rotationTotalHistory.pollFirst();
        yawDeltaScaled.addLast((long) (dYaw * 65536f));
        while (yawDeltaScaled.size() > 20) yawDeltaScaled.pollFirst();
    }

    // ─── Cached aggregates (PlaceholderHook) ───────────────
    private double cachedTotalVl = 0.0;
    private double cachedWorstVl = 0.0;
    private String cachedWorstId = "-";
    private long   cachedAtMs = 0L;
    public synchronized void refreshVlCache() {
        double total = 0, worst = 0;
        String worstId = "-";
        for (Check c : checks) {
            total += c.vl();
            if (c.vl() > worst) { worst = c.vl(); worstId = c.id(); }
        }
        cachedTotalVl = total;
        cachedWorstVl = worst;
        cachedWorstId = worst > 0 ? worstId : "-";
        cachedAtMs = System.currentTimeMillis();
    }
    private void ensureVlCache() {
        if (System.currentTimeMillis() - cachedAtMs > 1000L) refreshVlCache();
    }
    public double  totalVl()    { ensureVlCache(); return cachedTotalVl; }
    public double  worstVl()    { ensureVlCache(); return cachedWorstVl; }
    public String  worstVlId()  { ensureVlCache(); return cachedWorstId; }
}
