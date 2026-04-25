package gg.glacier.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import gg.glacier.Glacier;
import gg.glacier.check.Check;
import gg.glacier.data.PlayerData;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PacketListener {

    private final Glacier plugin;
    private final ProtocolManager manager;
    private PacketAdapter receiveAdapter;
    private PacketAdapter sendAdapter;

    // player -> (keepAliveId -> sentTimeMs)
    private final Map<UUID, Map<Integer, Long>> keepAlives = new ConcurrentHashMap<>();

    // player -> (transactionActionId -> sentTimeNanos)
    private final Map<UUID, Map<Short, Long>> transactions = new ConcurrentHashMap<>();

    private volatile boolean transactionsAvailable = true;
    private short txCounter = 1;

    public PacketListener(Glacier plugin) {
        this.plugin = plugin;
        this.manager = ProtocolLibrary.getProtocolManager();
    }

    public void register() {
        receiveAdapter = new PacketAdapter(plugin, ListenerPriority.NORMAL,
                PacketType.Play.Client.FLYING,
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.LOOK,
                PacketType.Play.Client.POSITION_LOOK,
                PacketType.Play.Client.KEEP_ALIVE,
                PacketType.Play.Client.TRANSACTION) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                PlayerData d = PacketListener.this.plugin.dataManager().get(event.getPlayer());
                if (d == null) return;

                PacketType type = event.getPacketType();

                if (type == PacketType.Play.Client.KEEP_ALIVE) {
                    int id = event.getPacket().getIntegers().read(0);
                    Map<Integer, Long> map = keepAlives.get(d.uuid());
                    if (map != null) {
                        Long sent = map.remove(id);
                        if (sent != null) {
                            long rtt = System.currentTimeMillis() - sent;
                            d.pingMs = (int) rtt;
                            for (Check c : d.checks()) if (c.enabled()) c.onKeepAliveResponse(rtt);
                        }
                    }
                    return;
                }

                if (type == PacketType.Play.Client.TRANSACTION) {
                    short action = event.getPacket().getShorts().read(0);
                    Map<Short, Long> map = transactions.get(d.uuid());
                    if (map != null) {
                        Long sentNs = map.remove(action);
                        if (sentNs != null) {
                            long rttMs = (System.nanoTime() - sentNs) / 1_000_000L;
                            d.transactionLatencyMs = (int) rttMs;
                        }
                    }
                    return;
                }

                if (type == PacketType.Play.Client.LOOK
                 || type == PacketType.Play.Client.POSITION_LOOK) {
                    try {
                        float yaw   = event.getPacket().getFloat().read(0);
                        float pitch = event.getPacket().getFloat().read(1);
                        d.updateRotation(yaw, pitch);
                        for (Check c : d.checks()) if (c.enabled()) c.onRotation();
                    } catch (Throwable ignored) {}
                }

                d.lastFlyingPacketMs = System.currentTimeMillis();
                d.flyingSinceLastAttack = true;
                for (Check c : d.checks()) if (c.enabled()) c.onFlyingPacket();
            }
        };

        sendAdapter = new PacketAdapter(plugin, ListenerPriority.NORMAL,
                PacketType.Play.Server.KEEP_ALIVE) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PlayerData d = PacketListener.this.plugin.dataManager().get(event.getPlayer());
                if (d == null) return;
                int id = event.getPacket().getIntegers().read(0);
                keepAlives.computeIfAbsent(d.uuid(), k -> new HashMap<>())
                          .put(id, System.currentTimeMillis());
            }
        };

        manager.addPacketListener(receiveAdapter);
        manager.addPacketListener(sendAdapter);
    }

    public void unregister() {
        if (receiveAdapter != null) manager.removePacketListener(receiveAdapter);
        if (sendAdapter    != null) manager.removePacketListener(sendAdapter);
    }

    /** Releases per-player tracking maps so unmatched probes don't leak. */
    public void clearForPlayer(UUID uuid) {
        keepAlives.remove(uuid);
        transactions.remove(uuid);
    }

    /** Sends a window-0 transaction used purely as a latency probe. */
    public void sendTransaction(Player p) {
        if (!transactionsAvailable) return;
        PlayerData d = plugin.dataManager().get(p);
        if (d == null) return;

        short id;
        synchronized (this) {
            if (txCounter >= Short.MAX_VALUE) txCounter = 1;
            id = txCounter++;
        }

        try {
            PacketContainer pkt = manager.createPacket(PacketType.Play.Server.TRANSACTION);
            pkt.getIntegers().write(0, 0);              // window id 0 (player inventory - always open)
            pkt.getShorts().write(0, id);               // action number
            pkt.getBooleans().write(0, false);          // not accepted - prevents client-side side effects
            manager.sendServerPacket(p, pkt);
            transactions.computeIfAbsent(d.uuid(), k -> new ConcurrentHashMap<>())
                        .put(id, System.nanoTime());
        } catch (Throwable t) {
            transactionsAvailable = false;
            plugin.getLogger().warning("Transaction packet unavailable on this server - falling back to keep-alive latency.");
        }
    }

    public boolean transactionsAvailable() { return transactionsAvailable; }
}
