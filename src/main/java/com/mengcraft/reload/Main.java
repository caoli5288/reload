package com.mengcraft.reload;

import org.bukkit.plugin.java.JavaPlugin;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Created on 16-8-7.
 */
public class Main extends JavaPlugin {

    private final Timer timer = new Timer("reload daemon", true);

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

        TickPerSecond ticker = new TickPerSecond();

        Executor executor = new Executor(this, engine, ticker);

        String to = getConfig().getString("kick.to", "");
        if (!to.isEmpty()) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            executor.setKickTo(to);
        }

        getServer().getPluginManager().registerEvents(executor, this);

        getServer().getScheduler().runTask(this, () -> timer.schedule(new TimerTask() {
            public void run() {
                ticker.update();
                if (ticker.get() < 1) {
                    getLogger().warning("Server frozen! Force shutdown!");
                    System.exit(1);
                }
            }
        }, 0, TimeUnit.MINUTES.toMillis(1)));

        getServer().getScheduler().runTaskTimer(this, ticker, 10, 10);
        getServer().getScheduler().runTaskTimer(this, executor, 100, 100);
    }

    @Override
    public void onDisable() {
        timer.cancel();
    }

    public static int unixTime() {
        return (int) System.currentTimeMillis() / 1000;
    }

}
