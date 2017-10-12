package com.mengcraft.reload;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created on 16-8-7.
 */
public class Executor extends Messenger implements Listener, Runnable {

    private final Main main;
    private final Machine machine;

    private List<String> kick;
    private boolean shutdown;

    public Executor(Main main, Machine machine) {
        super(main);
        this.main = main;
        this.machine = machine;
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        machine.incFlow();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void handle(PlayerLoginEvent event) {
        if (shutdown) {
            event.setResult(Result.KICK_FULL);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void handle(ServerListPingEvent event) {
        if (shutdown) {
            event.setMaxPlayers(event.getNumPlayers());
            event.setMotd(ChatColor.DARK_RED + "重启中");
        }
    }

    @Override
    public void run() {
        if (!shutdown) {
            process();
        }
    }

    private void process() {
        if (machine.process()) {
            main.getLogger().info("Express " + machine.getExpr() + " matched, " +
                    "scheduling shutdown...");
            shutdown = true;
            int i = main.getConfig().getInt("wait") * 20;
            main.run(this::note, 0, 100);
            main.run(this::kick, i - 5);
            main.run(main::shutdown, i);
        }

    }

    private void note() {
        for (Player p : main.getServer().getOnlinePlayers()) {
            send(p, "notify");
        }
    }

    private void kick() {
        ByteArrayDataOutput buf = ByteStreams.newDataOutput();
        buf.writeUTF("Connect");
        buf.writeUTF(nextKickTo());
        byte[] data = buf.toByteArray();

        for (Player p : main.getServer().getOnlinePlayers()) {
            p.sendPluginMessage(main, "BungeeCord", data);
        }
    }

    private String nextKickTo() {
        int i = ThreadLocalRandom.current().nextInt(kick.size());
        return kick.get(i);
    }

    public void setKick(List<String> kick) {
        this.kick = kick;
    }

}
