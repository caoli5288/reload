package com.mengcraft.reload;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.lang.reflect.Field;
import java.util.List;

public class Utils {

    private static final ScriptEngine SCRIPT_ENGINE;
    @Getter
    private static CommandMap commandMap;

    static {
        SCRIPT_ENGINE = new ScriptEngineManager(Utils.class.getClassLoader())
                .getEngineByName("nashorn");
        try {
            // To compatible with some ecloud placeholders
            SCRIPT_ENGINE.eval("yes = true; no = false;");
        } catch (ScriptException e) {
            e.printStackTrace();
        }
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
        try {
            Object result = Utils.eval(s);
            if (result instanceof Boolean) {
                return (boolean) result;
            } else if (result instanceof Number) {
                return ((Number) result).doubleValue() != 0;
            }
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Object eval(String js) throws ScriptException {
        return SCRIPT_ENGINE.eval(js);
    }
}
