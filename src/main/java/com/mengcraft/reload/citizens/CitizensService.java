package com.mengcraft.reload.citizens;

import com.google.common.collect.Maps;
import net.citizensnpcs.api.event.NPCDespawnEvent;
import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;

public class CitizensService implements Listener {

    private static final CitizensService SERVICE = new CitizensService();

    private final Map<UUID, ITrait> map = Maps.newHashMap();

    @EventHandler
    public void onReload(NPCDespawnEvent event) {
        if (event.getReason().name().equals("RELOAD")) {
            ITrait trait = map.get(event.getNPC().getUniqueId());
            if (trait != null) {
                trait.onReload();
            }
        }
    }

    @EventHandler
    public void onClick(NPCLeftClickEvent event) {
        ITrait trait = map.get(event.getNPC().getUniqueId());
        if (trait != null) {
            trait.onClick(event.getClicker());
        }
    }

    @EventHandler
    public void onClick(NPCRightClickEvent event) {
        ITrait trait = map.get(event.getNPC().getUniqueId());
        if (trait != null) {
            trait.onClick(event.getClicker());
        }
    }

    public static CitizensService getService() {
        return SERVICE;
    }

    public static void register(UUID uuid, ITrait trait) {
        getService().map.put(uuid, trait);
    }

    public static void unregister(UUID uuid) {
        getService().map.remove(uuid);
    }
}
