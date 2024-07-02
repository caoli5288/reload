package com.mengcraft.reload;

import com.google.common.base.Preconditions;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.MetadataStoreBase;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.Map;

public class MetadataCleaner implements Listener {

    private static Map<String, ?> metadataMap;
    private static MetadataCleaner instance;

    static {
        load0();
    }

    private final boolean log;

    public MetadataCleaner(boolean log) {
        this.log = log;
    }

    @SneakyThrows
    private static void load0() {
        Field f = Bukkit.getServer().getClass().getDeclaredField("playerMetadata");
        f.setAccessible(true);
        Object playerMetadata = f.get(Bukkit.getServer());
        f = MetadataStoreBase.class.getDeclaredField("metadataMap");
        f.setAccessible(true);
        metadataMap = (Map<String, ?>) f.get(playerMetadata);
    }

    public static void load(Plugin plugin, boolean log) {
        Preconditions.checkState(instance == null, "Already loaded");
        instance = new MetadataCleaner(log);
        Bukkit.getPluginManager().registerEvents(instance, plugin);
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        Player who = event.getPlayer();
        String key = who.getUniqueId() + ":";
        int old = metadataMap.size();
        metadataMap.keySet().removeIf(it -> it.startsWith(key));
        int delta = old - metadataMap.size();
        if (delta > 0 && log) {
            // log
            Bukkit.getLogger().warning("[MetadataCleaner] Clean " + who.getName() + "'s metadata: " + delta);
        }
    }
}
