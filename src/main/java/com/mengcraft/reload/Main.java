package com.mengcraft.reload;

import org.bukkit.command.CommandMap;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Created on 16-8-7.
 */
public class Main extends JavaPlugin {

    private ScheduledExecutorService watchdog; // GC safe
    private ScheduledExecutorService pool;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String expr = getConfig().getString("control.expr");

        if (!(nil(expr) || expr.isEmpty())) {
            Executor executor = new Executor(this, Machine.build(expr));

            List<String> to = getConfig().getStringList("kick.to");
            if (!to.isEmpty()) {
                getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
                executor.setKick(to);
            }

            getServer().getPluginManager().registerEvents(executor, this);
            getServer().getScheduler().runTaskTimer(this, executor, 0, 200);
        }

        pool = new ScheduledThreadPoolExecutor(1);
        pool.scheduleAtFixedRate(() -> {
            Ticker.INST.update();
            if (Ticker.INST.getShort() < 1) {
                getLogger().log(Level.SEVERE, "TPS < 1, killing...");
                shutdown(true);
            }
        }, 30, 60, TimeUnit.SECONDS);

        getServer().getScheduler().runTaskTimer(this, Ticker.INST, 0, 20);

        try {
            Field f = SimplePluginManager.class.getDeclaredField("commandMap");
            f.setAccessible(true);
            CommandMap map = (CommandMap) f.get(getServer().getPluginManager());
            map.register("uptime", new Uptime());
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public void shutdown(boolean force) {
        if (force || getConfig().getBoolean("force")) {
            if (System.getProperty("os.name").equals("Linux")) {
                String bean = ManagementFactory.getRuntimeMXBean().getName();
                String pid = bean.substring(0, bean.indexOf('@'));
                ProcessBuilder b = new ProcessBuilder("kill", "-9", pid);
                try {
                    b.start();
                } catch (IOException ignore) {
                }
            } else {
                System.exit(1);
            }
        } else {
            watchdog = new ScheduledThreadPoolExecutor(1);
            watchdog.schedule(() -> shutdown(true), 2, TimeUnit.MINUTES);
            getServer().shutdown();
        }
    }

    public void log(String line) {
        getLogger().info(line);
    }

    public void shutdown() {
        shutdown(false);
    }

    @Override
    public void onDisable() {
        if (pool != null) {
            pool.shutdownNow();
        }
    }

    public static boolean nil(Object i) {
        return i == null;
    }

    public void run(Runnable r, int delay, int i) {
        getServer().getScheduler().runTaskTimer(this, r, delay, i);
    }

    public void run(Runnable r, int delay) {
        getServer().getScheduler().runTaskLater(this, r, delay);
    }

}
