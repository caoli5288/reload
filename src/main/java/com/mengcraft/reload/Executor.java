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

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created on 16-8-7.
 */
public class Executor extends Messenger implements Listener, Runnable {

    private final Ticker ticker;
    private final ScriptEngine script;
    private final int s;
    private List<String> kick;
    private int flow;
    private boolean shutdown;

    public Executor(Main main, ScriptEngine script, Ticker ticker) {
        super(main);
        this.script = script;
        this.ticker = ticker;
        s = Main.unixTime();
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        flow++;
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
        script.put("time", Main.unixTime() - s);
        script.put("online", main.getServer().getOnlinePlayers().size());
        script.put("flow", flow);
        script.put("tps", ticker.get1());
        script.put("memory", calc());

        try {
            if ((boolean) Invocable.class.cast(script).invokeFunction("check")) {
                main.getLogger().info("Scheduled shutdown");
                shutdown = true;
                int i = main.getConfig().getInt("wait") * 20;
                main.process(this::note, 0, 100);
                main.process(this::kick, i - 5);
                main.process(main::shutdown, i);
            }
        } catch (ScriptException | NoSuchMethodException ignore) {
        }

    }

    public static float calc() {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long free = runtime.freeMemory();
        long allocated = runtime.totalMemory();
        return new BigDecimal(allocated - free).divide(new BigDecimal(max), 2, RoundingMode.HALF_UP).floatValue();
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
