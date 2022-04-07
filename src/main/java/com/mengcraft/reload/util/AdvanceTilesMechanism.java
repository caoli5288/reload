package com.mengcraft.reload.util;

import com.mengcraft.reload.Main;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.json.simple.JSONObject;
import org.spigotmc.SpigotWorldConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class AdvanceTilesMechanism implements Runnable {

    public static final int HOPPER_TRANSFER_MIN = 24;
    public static final int HOPPER_TRANSFER_MAX = 480;

    private static Method handleGetter;
    private static Field wcGetter;

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
                    if (field.getName().equals("spigotConfig")) {
                        wcGetter = field;
                    }
                    // TODO paper world config
                }
                return;
            }
        }
    }

    @Override
    public void run() {
        float tps = Main.getTicker().getShort();
        if (tps < 5) {// OMG
            // force reset to minimal bound
            for (World world : Bukkit.getWorlds()) {
                SpigotWorldConfig sc = (SpigotWorldConfig) spigot(world);
                sc.hopperTransfer = HOPPER_TRANSFER_MAX;
                sc.hopperCheck = HOPPER_TRANSFER_MAX / 3;
            }
        } else if (tps < 10) {
            // Server in heavy load
            for (World world : Bukkit.getWorlds()) {
                SpigotWorldConfig sc = (SpigotWorldConfig) spigot(world);
                if (sc.hopperTransfer < HOPPER_TRANSFER_MAX) {
                    // adjust it
                    sc.hopperTransfer = Math.min(HOPPER_TRANSFER_MAX, sc.hopperTransfer + (sc.hopperTransfer / 10));
                    sc.hopperCheck = sc.hopperTransfer / 3;
                    JSONObject obj = new JSONObject();
                    obj.put("world", world.getName());
                    obj.put("hopperTransfer", sc.hopperTransfer);
                    obj.put("hopperCheck", sc.hopperCheck);
                    Bukkit.getLogger().info("[]," + obj.toJSONString());
                }
            }
        } else if (tps < 15) {
            // do nothing and wait
        } else {
            // Server in light load
            for (World world : Bukkit.getWorlds()) {
                SpigotWorldConfig sc = (SpigotWorldConfig) spigot(world);
                if (sc.hopperTransfer > HOPPER_TRANSFER_MIN) {
                    // adjust it
                    sc.hopperTransfer = Math.max(HOPPER_TRANSFER_MAX, sc.hopperTransfer - (sc.hopperTransfer / 10));
                    sc.hopperCheck = sc.hopperTransfer / 3;
                    JSONObject obj = new JSONObject();
                    obj.put("worldName", world.getName());
                    obj.put("hopperTransfer", sc.hopperTransfer);
                    obj.put("hopperCheck", sc.hopperCheck);
                    Bukkit.getLogger().info("[]," + obj.toJSONString());
                }
            }
        }
    }

    @SneakyThrows
    Object spigot(World world) {
        Object nmsWorld = handleGetter.invoke(world);
        return wcGetter.get(nmsWorld);
    }
}
