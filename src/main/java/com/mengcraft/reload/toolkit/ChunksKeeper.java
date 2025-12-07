package com.mengcraft.reload.toolkit;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;

@RequiredArgsConstructor
public class ChunksKeeper implements Listener {

    private static ChunksKeeper instance;
    private final int limit;
    private final boolean log;

    public static void load(Plugin plugin, FileConfiguration config) {
        Preconditions.checkState(instance == null);
        if (config.getBoolean("chunks_keeper.enable", false)) {
            Bukkit.getPluginManager().registerEvents(instance = new ChunksKeeper(config.getInt("chunks_keeper.limit", 1000), config.getBoolean("chunks_keeper.log", false)), plugin);
            Bukkit.getLogger().info("[ChunksKeeper] Enabled");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void handle(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        World level = chunk.getWorld();
        if (level.getChunkCount() < limit) {
            event.setCancelled(true);
            if (log) {
                // log
                Bukkit.getLogger().warning("[ChunksKeeper] Keep chunk " + level.getName() + " " + chunk.getX() + "," + chunk.getZ());
            }
        }
    }
}
