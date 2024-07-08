package com.mengcraft.reload.command;

import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import com.mengcraft.reload.Main;
import com.mengcraft.reload.PluginHelper;
import com.mengcraft.reload.util.DelegateTask;
import com.mengcraft.reload.util.Generator;
import com.mengcraft.reload.util.IntRef;
import com.mengcraft.reload.util.Position;
import lombok.SneakyThrows;
import lombok.Value;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.util.NumberConversions;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CommandChunk implements PluginHelper.IExec {

    private static final Map<String, Material> BY_NAME = loadMaterials();

    @SneakyThrows
    private static Map<String, Material> loadMaterials() {
        Field f = Material.class.getDeclaredField("BY_NAME");
        f.setAccessible(true);
        return (Map<String, Material>) f.get(Material.class);
    }

    private static final List<Position> NEARBY_POSITIONS;

    static {
        NEARBY_POSITIONS = Arrays.stream(BlockFace.values()).filter(it -> it.getModY() == 0).map(it -> new Position(it.getModX(), 0, it.getModZ())).collect(Collectors.toList());
    }

    private final Map<String, CleanTileOptions> taskClean = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .<String, CleanTileOptions>build()
            .asMap();

    @Override
    public void exec(CommandSender console, List<String> list) {
        if (list.isEmpty()) {
            // Display chunks
            for (World it : Bukkit.getWorlds()) {
                console.sendMessage(it.getName() + ": " + it.getChunkCount());
            }
            console.sendMessage("/chunk load <*|world> <x[:len]> <z[:len]> [gen]");
            console.sendMessage("/chunk clean-tile <*|world> <tile[,tile2...]> [0|1]");
        } else {
            String label = list.remove(0);
            if (label.equals("load")) {
                load(console, list);
            } else if (label.equals("clean-tile")) {
                cleanTile(console, list);
            }
        }
    }

    private void cleanTile(CommandSender console, List<String> list) {
        // clean 0:world_selector 1:tile_selector 2:pending
        Collection<World> levels = list.get(0).equals("*") ?
                Sets.newHashSet(Bukkit.getWorlds()) :
                Collections.singleton(Bukkit.getWorld(list.get(0)));
        Set<Material> types = Arrays.stream(list.get(1).split(","))
                .map(BY_NAME::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        // notify clean types
        console.sendMessage("Clean " + types);
        CleanTileOptions options = new CleanTileOptions(levels, types);
        if (list.size() > 2 && list.get(2).equals("1")) {
            taskClean.put(console.getName(), options);
        }
        for (World level : levels) {
            for (Chunk chunk : level.getLoadedChunks()) {
                for (BlockState tile : chunk.getTileEntities()) {
                    cleanIfMatched(console, tile, options);
                }
            }
        }
    }

    private void load(CommandSender console, List<String> list) {
        String label0 = list.get(0);
        Collection<World> levels = label0.equals("*") ?
                Bukkit.getWorlds() :
                Collections.singleton(Bukkit.getWorld(label0));
        Iterator<Position> positions;
        int x;
        int z;
        String t = list.get(1);
        if (t.contains(":")) {
            // Range mode
            int i = t.indexOf(':');
            x = NumberConversions.toInt(t.substring(0, i));
            int lx = NumberConversions.toInt(t.substring(i + 1));
            t = list.get(2);
            i = t.indexOf(':');
            z = NumberConversions.toInt(t.substring(0, i));
            int lz = NumberConversions.toInt(t.substring(i + 1));
            IntRef gx = new IntRef();
            IntRef gz = new IntRef();
            positions = new Generator<>(() -> {
                int oldX = gx.value;
                int oldZ = gz.value;
                if (oldX < lx && oldZ < lz) {
                    gx.value++;
                    if (gx.value >= lx) {
                        gx.value = 0;
                        gz.value++;
                    }
                    return new Position(oldX, 0, oldZ);
                }
                return null;
            }).iterator();
        } else {
            positions = NEARBY_POSITIONS.iterator();
            x = NumberConversions.toInt(t);
            z = NumberConversions.toInt(list.get(2));
        }
        boolean gen = list.size() > 3 && list.get(3).equalsIgnoreCase("gen");
        loadTask(console, new LoadOptions(positions, levels), x, z, gen);
    }

    private void loadTask(CommandSender console, LoadOptions options, int x, int z, boolean gen) {
        new DelegateTask(task -> {
            Iterator<Position> positions = options.positions;
            CleanTileOptions cleanOptions = taskClean.get(console.getName());
            for (int i = 0; i < 3 && !task.isCancelled(); i++) {
                if (positions.hasNext()) {
                    Position next = positions.next();
                    int chunkX = x + next.getX();
                    int chunkZ = z + next.getZ();
                    for (World level : options.levels) {
                        Chunk chunkAt = level.getChunkAt(chunkX, chunkZ);
                        boolean load = chunkAt.load(gen);
                        console.sendMessage("load chunk (" + level.getName() + "," + chunkX + "," + chunkZ + ") " + (load ? "success" : "fail"));
                        if (load && cleanOptions != null && cleanOptions.levels.contains(level)) {
                            for (BlockState tile : chunkAt.getTileEntities()) {
                                cleanIfMatched(console, tile, cleanOptions);
                            }
                        }
                    }
                } else {
                    task.cancel();
                }
            }
        }).runTaskTimer(Main.getInstance(), 0, 1);
    }

    private static void cleanIfMatched(CommandSender console, BlockState tile, CleanTileOptions options) {
        if (options.types.contains(tile.getType())) {
            tile.setType(Material.AIR);
            console.sendMessage("clean tile " + tile.getLocation());
        }
    }

    @Value
    static class CleanTileOptions {

        public Collection<World> levels;
        public Set<Material> types;
    }

    @Value
    static class LoadOptions {

        Iterator<Position> positions;
        Collection<World> levels;
    }
}
