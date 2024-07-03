package com.mengcraft.reload.command;

import com.mengcraft.reload.Main;
import com.mengcraft.reload.PluginHelper;
import com.mengcraft.reload.util.DelegateTask;
import com.mengcraft.reload.util.Generator;
import com.mengcraft.reload.util.IntRef;
import com.mengcraft.reload.util.Position;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.util.NumberConversions;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class CommandChunk implements PluginHelper.IExec {

    private static final List<Position> NEARBY_POSITIONS;

    static {
        NEARBY_POSITIONS = Arrays.stream(BlockFace.values()).filter(it -> it.getModY() == 0).map(it -> new Position(it.getModX(), 0, it.getModZ())).collect(Collectors.toList());
    }

    @Override
    public void exec(CommandSender console, List<String> list) {
        if (list.isEmpty()) {
            // Display chunks
            for (World it : Bukkit.getWorlds()) {
                console.sendMessage(it.getName() + ": " + it.getChunkCount());
            }
            console.sendMessage("Use /chunk <world> <x[:len]> <z[:len]> [gen] to load chunks");
        } else {
            loadChunk(console, list);
        }
    }

    private static void loadChunk(CommandSender console, List<String> list) {
        World level = Bukkit.getWorld(list.get(0));
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
        new DelegateTask(task -> {
            for (int i = 0; i < 3 && !task.isCancelled(); i++) {
                if (positions.hasNext()) {
                    Position next = positions.next();
                    int chunkX = x + next.getX();
                    int chunkZ = z + next.getZ();
                    Chunk chunkAt = level.getChunkAt(chunkX, chunkZ);
                    boolean load = chunkAt.load(gen);
                    console.sendMessage("load chunk (" + chunkX + "," + chunkZ + ") " + (load ? "success" : "fail"));
                } else {
                    task.cancel();
                }
            }
        }).runTaskTimer(Main.getInstance(), 0, 1);
    }
}
