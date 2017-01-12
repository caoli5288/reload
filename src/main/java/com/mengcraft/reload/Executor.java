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
    private final ScriptEngine engine;
    private final int startedTime;

    private List<String> kickTo;
    private boolean processWait;
    private boolean shutdown;

    private int wait;
    private int flow;

    public Executor(Main main, ScriptEngine engine, Ticker ticker) {
        super(main);
        this.engine = engine;
        this.ticker = ticker;
        startedTime = Main.unixTime();
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
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
        if (processWait || shutdown) {
            event.setMaxPlayers(event.getNumPlayers());
            event.setMotd(ChatColor.DARK_RED + "重启中");
        }
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
        engine.put("time", Main.unixTime() - startedTime);
        engine.put("online", main.getServer().getOnlinePlayers().size());
        engine.put("flow", flow);
        engine.put("tps", ticker.get());
        engine.put("memory", calcMemory());

        try {
            if ((boolean) Invocable.class.cast(engine).invokeFunction("check")) {
                main.getLogger().info("Scheduled shutdown");
                processWait = true;
                wait = main.getConfig().getInt("wait");
            }
        } catch (ScriptException | NoSuchMethodException ignore) {
        }

    }

    public static float calcMemory() {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long free = runtime.freeMemory();
        long allocated = runtime.totalMemory();
        return new BigDecimal(allocated - free).divide(new BigDecimal(max), 2, RoundingMode.HALF_UP).floatValue();
    }

    private void processTimeWait() {
        if (!shutdown && (wait = wait - 5) < 0) {
            processEnd();
        }
        processNotify();
    }

    private void processNotify() {
        for (Player p : main.getServer().getOnlinePlayers()) {
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
        main.getServer().getScheduler().runTaskLater(main, main::shutdown, 20);
        shutdown = true;
    }

    private void processKick() {
        ByteArrayDataOutput buf = ByteStreams.newDataOutput();
        buf.writeUTF("Connect");
        buf.writeUTF(nextKickTo());
        byte[] data = buf.toByteArray();

        for (Player p : main.getServer().getOnlinePlayers()) {
            p.sendPluginMessage(main, "BungeeCord", data);
        }
    }

    private String nextKickTo() {
        int i = ThreadLocalRandom.current().nextInt(kickTo.size());
        return kickTo.get(i);
    }

    public void setKickTo(List<String> kickTo) {
        this.kickTo = kickTo;
    }

}
