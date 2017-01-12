package com.mengcraft.reload;

import org.bukkit.plugin.java.JavaPlugin;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Created on 16-8-7.
 */
public class Main extends JavaPlugin {

    private Timer daemon;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ScriptEngine engine = new ScriptEngineManager().getEngineByName("js");
        try {
            engine.eval("function check() {\n" +
                    "    return " + getConfig().getString("control.expr") + ";\n" +
                    "}");
        } catch (ScriptException ignore) {
        }

        Ticker ticker = new Ticker();
        Executor executor = new Executor(this, engine, ticker);

        List<String> to = getConfig().getStringList("kick.to");
        if (!to.isEmpty()) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            executor.setKickTo(to);
        }

        getServer().getPluginManager().registerEvents(executor, this);

        daemon = new Timer("reload daemon", true);

        getServer().getScheduler().runTask(this, () -> daemon.schedule(new TimerTask() {
            public void run() {
                ticker.update();
                if (ticker.get() < 1) {
                    getLogger().log(Level.SEVERE, "TPS < 1, killing...");
                    shutdown(true);
                }
            }
        }, 0, TimeUnit.MINUTES.toMillis(1)));

        getServer().getScheduler().runTaskTimer(this, ticker, 10, 10);
        getServer().getScheduler().runTaskTimer(this, executor, 200, 200);
    }

    public void shutdown(boolean force) {
        if (force || getConfig().getBoolean("force")) {
            if (System.getProperty("os.name").equals("Linux")) {
                String bean = ManagementFactory.getRuntimeMXBean().getName();
                String pid = bean.substring(0, bean.indexOf('@'));
                ProcessBuilder b = new ProcessBuilder("kill", "-9", pid);
                try {
                    b.start();
                } catch (IOException e) {
                }
            } else {
                System.exit(1);
            }
        } else {
            getServer().shutdown();
        }
    }

    public void shutdown() {
        shutdown(false);
    }

    @Override
    public void onDisable() {
        if (daemon != null) {
            daemon.cancel();
        }
    }

    public static int unixTime() {
        return (int) System.currentTimeMillis() / 1000;
    }

}
