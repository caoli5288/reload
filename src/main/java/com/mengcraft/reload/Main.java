package com.mengcraft.reload;

import org.bukkit.plugin.java.JavaPlugin;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import static org.bukkit.util.NumberConversions.toLong;

/**
 * Created on 16-8-7.
 */
public class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Executor executor = new Executor(this);

        double load = getConfig().getDouble("control.load");
        if (load > 0 && load < 1) {
            executor.setLoadLimit(toLong(Runtime.getRuntime().maxMemory() * load));
        }

        int flow = getConfig().getInt("control.flow");
        if (flow > 0) {
            executor.setFlowLimit(flow);
        }

        int time = getConfig().getInt("time");
        if (time > 0) {
            executor.setTime(System.currentTimeMillis() + time * 3600000L);
        }

        if (executor.hasFunction()) {
            String to = getConfig().getString("kick.to", "");
            if (!to.isEmpty()) {
                getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
                executor.setKickTo(to);
            }
            executor.setDebug(getConfig().getBoolean("debug"));
            getServer().getPluginManager().registerEvents(executor, this);
            getServer().getScheduler().runTaskTimer(this, executor, 100, 100);
        } else {
            getLogger().warning("No controller enabled!");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

}
