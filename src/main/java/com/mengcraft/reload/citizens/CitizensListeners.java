package com.mengcraft.reload.citizens;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.reflect.ClassPath;
import lombok.SneakyThrows;
import net.citizensnpcs.api.event.NPCDespawnEvent;
import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CitizensListeners implements Listener {

    private final Multimap<Class<?>, Class<Trait>> types = ArrayListMultimap.create();

    @SneakyThrows
    public CitizensListeners() {
        ImmutableSet<ClassPath.ClassInfo> classes = ClassPath.from(CitizensListeners.class.getClassLoader())
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
}
