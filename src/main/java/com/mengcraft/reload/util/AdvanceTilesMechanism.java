package com.mengcraft.reload.util;

import com.mengcraft.reload.Main;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.json.simple.JSONObject;
import org.spigotmc.SpigotWorldConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

public class AdvanceTilesMechanism implements Runnable {

    public static final int HOPPER_TRANSFER_MIN = 8;
    public static final int HOPPER_TRANSFER_MAX = 240;

    private static Method handleGetter;
    private static Field wcGetter;

    private final AtomicInteger taskId = new AtomicInteger();

    static {
        nms();
    }

    static void nms() {
        World first = Bukkit.getWorlds().get(0);
        for (Method method : first.getClass().getMethods()) {
            if (method.getName().equals("getHandle")) {
                handleGetter = method;
                Class<?> clsWorld = handleGetter.getReturnType().getSuperclass();// nms.World
                for (Field field : clsWorld.getFields()) {
                    if (field.getType() == SpigotWorldConfig.class) {
                        wcGetter = field;
                        Bukkit.getLogger().info("[Tiles] Inject " + field);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        int i = taskId.incrementAndGet();
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            if (i == taskId.get()) {
                execute();
            }
        });
    }

    private void execute() {
        float tps = Main.getTicker().getShort();
        if (tps < 5) {// OMG
            // force reset to minimal bound
            for (World world : Bukkit.getWorlds()) {
                SpigotWorldConfig sc = spigotConfig(world);
                // check if hopperTransfer is not minimal
                if (sc.hopperTransfer > HOPPER_TRANSFER_MIN) {
                    sc.hopperTransfer = HOPPER_TRANSFER_MAX;
                    sc.hopperCheck = HOPPER_TRANSFER_MAX / 3;
                    // log
                    JSONObject json = new JSONObject();
                    json.put("world", world.getName());
                    json.put("hopperTransfer", sc.hopperTransfer);
                    json.put("hopperCheck", sc.hopperCheck);
                    Bukkit.getLogger().info("[Tiles]," + json.toJSONString());
                }
            }
        } else if (tps < 10) {
            // Server in heavy load
            for (World world : Bukkit.getWorlds()) {
                SpigotWorldConfig sc = spigotConfig(world);
                if (sc.hopperTransfer < HOPPER_TRANSFER_MAX) {
                    // adjust it
                    sc.hopperTransfer = Math.min(HOPPER_TRANSFER_MAX, sc.hopperTransfer + HOPPER_TRANSFER_MIN);
                    sc.hopperCheck = sc.hopperTransfer / 3;
                    // log
                    JSONObject obj = new JSONObject();
                    obj.put("world", world.getName());
                    obj.put("hopperTransfer", sc.hopperTransfer);
                    obj.put("hopperCheck", sc.hopperCheck);
                    Bukkit.getLogger().info("[Tiles]," + obj.toJSONString());
                }
                // Paper
            }
        } else if (tps < 15) {
            // do nothing and wait
        } else {
            // Server in light load
            for (World world : Bukkit.getWorlds()) {
                SpigotWorldConfig sc = spigotConfig(world);
                if (sc.hopperTransfer > HOPPER_TRANSFER_MIN) {
                    // adjust it
                    sc.hopperTransfer = Math.max(HOPPER_TRANSFER_MAX, sc.hopperTransfer - HOPPER_TRANSFER_MIN);
                    sc.hopperCheck = sc.hopperTransfer / 3;
                    JSONObject obj = new JSONObject();
                    obj.put("world", world.getName());
                    obj.put("hopperTransfer", sc.hopperTransfer);
                    obj.put("hopperCheck", sc.hopperCheck);
                    Bukkit.getLogger().info("[Tiles]," + obj.toJSONString());
                }
            }
        }
    }

    @SneakyThrows
    SpigotWorldConfig spigotConfig(World world) {
        Object nmsWorld = handleGetter.invoke(world);
        return (SpigotWorldConfig) wcGetter.get(nmsWorld);
    }
}
