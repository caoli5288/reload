package com.mengcraft.reload;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.Plugin;

/**
 * Created on 16-8-7.
 */
public class Executor extends Messenger implements Listener, Runnable {

    private String kickTo;

    private boolean processWait;
    private boolean shutdown;
    private int wait;

    private long load;
    private long loadLimit;

    private int flow;
    private int flowLimit;

    private boolean debug;

    public Executor(Plugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void handle(PlayerJoinEvent event) {
        flow++;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void handle(PlayerLoginEvent event) {
        if (processWait) {
            event.setResult(Result.KICK_FULL);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void handle(ServerListPingEvent event) {
        event.setMaxPlayers(event.getNumPlayers());
        event.setMotd(ChatColor.DARK_RED + "重启中");
    }

    @Override
    public void run() {
        if (processWait) {
            processTimeWait();
        } else {
            process();
        }
    }

    private void process() {
        load = Math.max(load, Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());

        if ((load > loadLimit) && (flow > flowLimit)) {
            getPlugin().getLogger().info("Scheduled shutdown! (" + (load / 1048576) + "M, " + flow + ')');
            processWait = true;
            wait = getPlugin().getConfig().getInt("wait");
        }

        if (debug) {
            getPlugin().getLogger().info("DEBUG! (" + (load / 1048576) + "M, " + flow + ')');
        }
    }

    private void processTimeWait() {
        if (!shutdown && (wait = wait - 5) < 0) {
            processEnd();
        }
        processNotify();
    }

    private void processNotify() {
        for (Player p : getPlugin().getServer().getOnlinePlayers()) {
            send(p, "notify");
        }
    }

    private void processEnd() {
        if (kickTo != null) {
            processKick();
        }
        processShutdown();
    }

    private void processShutdown() {
        getPlugin().getServer().getScheduler().runTaskLater(getPlugin(), () -> {
            if (getPlugin().getConfig().getBoolean("force")) {
                System.exit(0);
            } else {
                getPlugin().getServer().shutdown();
            }
        }, 20);
        shutdown = true;
    }

    private void processKick() {
        ByteArrayDataOutput buf = ByteStreams.newDataOutput();
        buf.writeUTF("Connect");
        buf.writeUTF(kickTo);
        byte[] data = buf.toByteArray();

        for (Player p : getPlugin().getServer().getOnlinePlayers()) {
            p.sendPluginMessage(getPlugin(), "BungeeCord", data);
        }
    }

    public boolean hasFunction() {
        return loadLimit != 0 || flowLimit != 0;
    }

    public void setLoadLimit(long loadLimit) {
        this.loadLimit = loadLimit;
    }

    public void setFlowLimit(int flowLimit) {
        this.flowLimit = flowLimit;
    }

    public void setKickTo(String kickTo) {
        this.kickTo = kickTo;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

}
