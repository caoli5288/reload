package com.mengcraft.reload.util;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Server;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

public class NMS {

    private MethodHandle mServer$isRunning;
    private Object mServer;
    @Getter
    private boolean enable;

    public NMS() {
        try {
            Server server = Bukkit.getServer();// CraftServer
            Field f = server.getClass().getDeclaredField("console");
            f.setAccessible(true);
            mServer = f.get(server);// MinecraftServer
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            mServer$isRunning = lookup.findVirtual(mServer.getClass(), "isRunning", MethodType.methodType(boolean.class));
            enable = true;
            // tests
            mServer$isRunning();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean mServer$isRunning() {
        if (enable) {
            try {
                return (boolean) mServer$isRunning.invoke(mServer);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}
