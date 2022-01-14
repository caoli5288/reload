package com.mengcraft.reload.citizens;

import com.google.common.collect.Maps;
import net.citizensnpcs.api.event.DespawnReason;
import net.citizensnpcs.api.event.NPCDespawnEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;

public class CitizensService implements Listener {

    private static final CitizensService SERVICE = new CitizensService();

    private final Map<UUID, ITrait> map = Maps.newHashMap();

    @EventHandler
    public void onReload(NPCDespawnEvent event) {
        if (event.getReason() == DespawnReason.RELOAD) {
            ITrait trait = map.get(event.getNPC().getUniqueId());
            if (trait != null) {
                trait.onReload();
            }
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
