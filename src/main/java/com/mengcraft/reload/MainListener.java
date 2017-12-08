package com.mengcraft.reload;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerListPingEvent;

/**
 * Created on 16-8-7.
 */
public class MainListener extends Messenger implements Listener, Runnable {

    private final Main main;
    private final Machine machine;

    public MainListener(Main main, Machine machine) {
        super(main);
        this.main = main;
        this.machine = machine;
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        machine.incFlow();
    }

    @EventHandler
    public void handle(PlayerJoinEvent event) {
        machine.incJoin();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void handle(PlayerLoginEvent event) {
        if (main.shutdown) {
            event.setResult(Result.KICK_FULL);
        } else {
            machine.incLogin();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void handle(ServerListPingEvent event) {
        if (main.shutdown) {
            event.setMaxPlayers(event.getNumPlayers());
            event.setMotd(ChatColor.DARK_RED + "重启中");
        }
    }

    @Override
    public void run() {
        if (!main.shutdown) {
            process();
        }
    }

    private void process() {
        if (machine.process()) {
            main.getLogger().info("Express " + machine.getExpr() + " matched, " +
                    "scheduling shutdown...");
            main.shutdown = true;
            int i = main.getConfig().getInt("wait") * 20;
            main.run(this::note, 0, 55);
            main.run(main::kickAll, i - 5);
            main.run(main::shutdown, i);
        }
    }

    private void note() {
        for (Player p : main.getServer().getOnlinePlayers()) {
            send(p, "notify");
        }
    }

}
