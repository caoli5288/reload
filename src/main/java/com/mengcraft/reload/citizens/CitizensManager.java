package com.mengcraft.reload.citizens;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.reflect.ClassPath;
import lombok.Getter;
import lombok.SneakyThrows;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCDespawnEvent;
import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.persistence.PersistenceLoader;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitFactory;
import net.citizensnpcs.api.trait.TraitInfo;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;
import java.util.UUID;

public class CitizensManager implements Listener {

    private static final CitizensManager INSTANCE = new CitizensManager();
    private final Multimap<Class<?>, Class<Trait>> types = ArrayListMultimap.create();
    @Getter
    private final Map<UUID, NPC> hides = Maps.newHashMap();

    static {
        PersistenceLoader.registerPersistDelegate(Rule.class, Rule.class);
    }

    @SneakyThrows
    public CitizensManager() {
        ImmutableSet<ClassPath.ClassInfo> classes = ClassPath.from(CitizensManager.class.getClassLoader())
                .getTopLevelClassesRecursive("com.mengcraft.reload.citizens");
        for (ClassPath.ClassInfo info : classes) {
            Class<?> cls = info.load();
            if (Trait.class.isAssignableFrom(cls)) {
                Class<Trait> tCls = (Class<Trait>) cls;
                if (IClickable.class.isAssignableFrom(cls)) {
                    types.put(IClickable.class, tCls);
                }
                if (IReloadable.class.isAssignableFrom(cls)) {
                    types.put(IReloadable.class, tCls);
                }
            }
        }
    }

    @EventHandler
    public void onReload(NPCDespawnEvent event) {
        String reason = event.getReason().name();
        if (reason.equals("RELOAD")) {
            NPC npc = event.getNPC();
            for (Class<Trait> cls : types.get(IReloadable.class)) {
                if (npc.hasTrait(cls)) {
                    ((IReloadable) npc.getTrait(cls)).onReload();
                }
            }
        }
    }

    @EventHandler
    public void onClick(NPCLeftClickEvent event) {
        NPC npc = event.getNPC();
        for (Class<Trait> cls : types.get(IClickable.class)) {
            if (npc.hasTrait(cls)) {
                ((IClickable) npc.getTrait(cls)).onClick(event.getClicker());
            }
        }
    }

    @EventHandler
    public void onClick(NPCRightClickEvent event) {
        NPC npc = event.getNPC();
        for (Class<Trait> cls : types.get(IClickable.class)) {
            if (npc.hasTrait(cls)) {
                ((IClickable) npc.getTrait(cls)).onClick(event.getClicker());
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (hides.isEmpty()) {
            return;
        }
        Player p = event.getPlayer();
        for (NPC npc : hides.values()) {
            HideTrait trait = npc.getTrait(HideTrait.class);
            trait.onJoinWorld(p);
        }
    }

    @EventHandler
    public void onChangeWorld(PlayerChangedWorldEvent event) {
        if (hides.isEmpty()) {
            return;
        }
        Player p = event.getPlayer();
        for (NPC npc : hides.values()) {
            HideTrait trait = npc.getTrait(HideTrait.class);
            trait.onJoinWorld(p);
        }
    }

    public static void addTrait(TraitInfo info) {
        TraitFactory tf = CitizensAPI.getTraitFactory();
        tf.deregisterTrait(info);
        tf.registerTrait(info);
    }

    public static CitizensManager getInstance() {
        return INSTANCE;
    }
}
