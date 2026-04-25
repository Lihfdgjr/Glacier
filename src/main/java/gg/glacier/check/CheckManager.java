package gg.glacier.check;

import gg.glacier.Glacier;
import gg.glacier.check.impl.combat.AimA;
import gg.glacier.check.impl.combat.AutoClickerA;
import gg.glacier.check.impl.combat.AutoClickerB;
import gg.glacier.check.impl.combat.AutoClickerC;
import gg.glacier.check.impl.combat.CriticalsA;
import gg.glacier.check.impl.combat.HitBoxA;
import gg.glacier.check.impl.combat.KillAuraA;
import gg.glacier.check.impl.combat.KillAuraB;
import gg.glacier.check.impl.combat.KillAuraC;
import gg.glacier.check.impl.combat.ReachA;
import gg.glacier.check.impl.inventory.InventoryMoveA;
import gg.glacier.check.impl.movement.FlyA;
import gg.glacier.check.impl.movement.NoFallA;
import gg.glacier.check.impl.movement.NoSlowA;
import gg.glacier.check.impl.movement.NoWebA;
import gg.glacier.check.impl.movement.PhaseA;
import gg.glacier.check.impl.movement.SpeedA;
import gg.glacier.check.impl.movement.SpeedB;
import gg.glacier.check.impl.movement.VelocityA;
import gg.glacier.check.impl.player.TimerA;
import gg.glacier.check.impl.player.TimerB;
import gg.glacier.check.impl.protocol.BadPacketsA;
import gg.glacier.check.impl.protocol.BadPacketsB;
import gg.glacier.check.impl.world.FastBreakA;
import gg.glacier.check.impl.world.FastPlaceA;
import gg.glacier.check.impl.world.ScaffoldA;
import gg.glacier.check.impl.world.ScaffoldB;
import gg.glacier.check.impl.world.ScaffoldC;
import gg.glacier.data.PlayerData;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CheckManager {

    private final Glacier plugin;
    private final List<Class<? extends Check>> registry = new ArrayList<>();

    public CheckManager(Glacier plugin) {
        this.plugin = plugin;
        // Combat
        register(AutoClickerA.class);
        register(AutoClickerB.class);
        register(AutoClickerC.class);
        register(ReachA.class);
        register(KillAuraA.class);
        register(KillAuraB.class);
        register(KillAuraC.class);
        register(AimA.class);
        register(HitBoxA.class);
        register(CriticalsA.class);
        // Movement
        register(SpeedA.class);
        register(SpeedB.class);
        register(FlyA.class);
        register(NoFallA.class);
        register(PhaseA.class);
        register(VelocityA.class);
        register(NoWebA.class);
        register(NoSlowA.class);
        // World interaction
        register(ScaffoldA.class);
        register(ScaffoldB.class);
        register(ScaffoldC.class);
        register(FastPlaceA.class);
        register(FastBreakA.class);
        // Inventory
        register(InventoryMoveA.class);
        // Invalid protocol
        register(BadPacketsA.class);
        register(BadPacketsB.class);
        // Player / tick speed
        register(TimerA.class);
        register(TimerB.class);
    }

    private void register(Class<? extends Check> clazz) {
        registry.add(clazz);
    }

    public List<Check> instantiate(PlayerData data) {
        List<Check> list = new ArrayList<>(registry.size());
        for (Class<? extends Check> c : registry) {
            try {
                Constructor<? extends Check> ctor = c.getConstructor(Glacier.class, PlayerData.class);
                list.add(ctor.newInstance(plugin, data));
            } catch (ReflectiveOperationException e) {
                plugin.getLogger().severe("Failed to create " + c.getSimpleName() + ": " + e);
            }
        }
        return list;
    }

    public void reloadAll() {
        for (PlayerData d : plugin.dataManager().all()) {
            for (Check c : d.checks()) c.reload();
        }
    }

    public int count() { return registry.size(); }
    public List<Class<? extends Check>> registry() { return Collections.unmodifiableList(registry); }
}
