package com.mengcraft.reload;

import org.bukkit.command.CommandMap;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Created on 16-8-7.
 */
public class Main extends JavaPlugin {

    private ScheduledThreadPoolExecutor pool;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String path = "control.expr";
        String expr;

        if (getConfig().isBoolean(path)) {
            if (getConfig().getBoolean(path)) {
                expr = "(time > 36000 && online < 1) || tps < 5";
            } else {
                expr = null;
            }
        } else {
            expr = getConfig().getString(path);
        }

        Ticker ticker = new Ticker(this);

        if (!nil(expr)) {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("js");
            try {
                engine.eval("function check() {\n" +
                        "    return " + expr + ";\n" +
                        "}");
            } catch (ScriptException ignore) {
            }

            Executor executor = new Executor(this, engine, ticker);
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
            ticker.update();
            if (ticker.get1() < 1) {
                getLogger().log(Level.SEVERE, "TPS < 1, killing...");
                shutdown(true);
            }
        }, 30, 60, TimeUnit.SECONDS);

        getServer().getScheduler().runTaskTimer(this, ticker, 0, 20);

        try {
            Field f = SimplePluginManager.class.getDeclaredField("commandMap");
            f.setAccessible(true);
            CommandMap map = (CommandMap) f.get(getServer().getPluginManager());
            map.register("uptime", new Uptime(ticker));
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
            ScheduledThreadPoolExecutor i = new ScheduledThreadPoolExecutor(1);
            i.schedule(() -> shutdown(true), 2, TimeUnit.MINUTES);
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

    public static int unixTime() {
        return Math.toIntExact(System.currentTimeMillis() / 1000);
    }

    public void process(Runnable r, int delay, int i) {
        getServer().getScheduler().runTaskTimer(this, r, delay, i);
    }

    public void process(Runnable r, int delay) {
        getServer().getScheduler().runTaskLater(this, r, delay);
    }

}
