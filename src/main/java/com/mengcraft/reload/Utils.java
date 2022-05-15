package com.mengcraft.reload;

import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.lang.reflect.Field;
import java.util.List;

public class Utils {

    public static final ScriptEngine SCRIPT_ENGINE;
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

    @SneakyThrows
    public static <T> T asInterface(Class<T> cls, String contents) {
        Bindings bindings = SCRIPT_ENGINE.createBindings();
        SCRIPT_ENGINE.eval(contents, bindings);
        return ((Invocable) SCRIPT_ENGINE).getInterface(bindings, cls);
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
}
