package com.mengcraft.reload;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.List;

public class Utils {

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
