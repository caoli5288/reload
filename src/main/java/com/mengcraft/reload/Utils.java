package com.mengcraft.reload;

import com.google.common.base.Preconditions;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;

public class Utils {

    @Getter
    private static CommandMap commandMap;

    static {
        Server server = Bukkit.getServer();
        try {
            Field field = server.getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            commandMap = (CommandMap) field.get(server);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static boolean isNullOrEmpty(List<?> l) {
        return l == null || l.isEmpty();
    }

    public static <T> T take(List<T> list, int index) {
        if (list.size() > index) {
            return list.get(index);
        }
        return null;
    }

    public static Player castPlayer(Entity entity) {
        if (entity.getType() == EntityType.PLAYER) {
            return (Player) entity;
        }
        return null;
    }

    public static boolean asBoolean(String s) {
        // check simple contents
        switch (s) {
            case "true":
            case "yes":
            case "1":
                return true;
            case "false":
            case "no":
            case "0":
                return false;
        }
        Object result = JsContext.getInstance().eval(s);
        if (result instanceof Boolean) {
            return (boolean) result;
        } else if (result instanceof Number) {
            return ((Number) result).doubleValue() != 0;
        }
        return false;
    }

    public static String getPathQuery(URI uri) {
        String path = uri.getPath();
        String query = uri.getQuery();
        if (Utils.isNullOrEmpty(query)) {
            return path;
        }
        return path + "?" + query;
    }

    @NotNull
    public static String pid() {
        String rmxName = ManagementFactory.getRuntimeMXBean().getName();
        return rmxName.substring(0, rmxName.indexOf('@'));
    }

    public static void ensureLinux() {
        Preconditions.checkState(System.getProperty("os.name").toLowerCase().contains("linux"), "Only support Linux");
    }
}
