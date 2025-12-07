package com.mengcraft.reload.toolkit;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.mengcraft.reload.Main;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

public class DataFixersCleaner extends BukkitRunnable {

    private static DataFixersCleaner instance;
    private final List<Map<?, ?>> allMaps = Lists.newArrayList();
    private final boolean log;

    public DataFixersCleaner(boolean log) {
        this.log = log;
    }

    public void loadMaps() {
        try {
            loadStMap(Class.forName("com.mojang.datafixers.types.Type"));
            loadStMap(Class.forName("com.mojang.datafixers.functions.Fold"));
        } catch (ClassNotFoundException | IllegalAccessException e) {
            Main.log("[DataFixersCleaner] Failed to load maps.", e);
        }
    }

    private void loadStMap(Class<?> cls) throws IllegalAccessException {
        for (Field field : cls.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(field.getType()) && Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                allMaps.add((Map<?, ?>) field.get(null));
            }
        }
    }

    public static void load(Plugin plugin, FileConfiguration config) {
        Preconditions.checkState(instance == null);
        if (config.getBoolean("datafixers_cleaner.enable", false)) {
            instance = new DataFixersCleaner(config.getBoolean("datafixers_cleaner.log", true));
            instance.load();
            Bukkit.getLogger().info("[DataFixersCleaner] Enabled");
        }
    }

    private void load() {
        loadMaps();
        if (!allMaps.isEmpty()) {
            runTaskTimer(Main.getInstance(), 1200, 1200);
        }
    }

    @Override
    public void run() {
        int count = 0;
        for (Map<?, ?> let : allMaps) {
            count += let.size();
            let.clear();
        }
        if (count > 0 && log) {
            Bukkit.getLogger().warning("[DataFixersCleaner] Clean " + count + " cached keys.");
        }
    }
}
