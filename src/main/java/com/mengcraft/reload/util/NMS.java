package com.mengcraft.reload.util;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Server;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class NMS {

    private Object mServer;
    private Method mServer$isRunning;
    @Getter
    private boolean enable;

    public NMS() {
        try {
            Server server = Bukkit.getServer();// CraftServer
            Field f = server.getClass().getDeclaredField("console");
            f.setAccessible(true);
            mServer = f.get(server);// MinecraftServer
            mServer$isRunning = mServer.getClass().getDeclaredMethod("isRunning");
            mServer$isRunning.setAccessible(true);
            enable = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean mServer$isRunning() {
        if (enable) {
            try {
                return (boolean) mServer$isRunning.invoke(mServer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}
