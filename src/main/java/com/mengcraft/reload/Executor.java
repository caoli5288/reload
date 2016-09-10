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

/**
 * Created on 16-8-7.
 */
public class Executor extends Messenger implements Listener, Runnable {

    private final TickPerSecond ticker;
    private final ScriptEngine engine;
    private final int time;

    private String kickTo;

    private boolean processWait;
    private boolean shutdown;

    private int wait;
    private int flow;

    public Executor(Main main, ScriptEngine engine, TickPerSecond ticker) {
        super(main);
        this.engine = engine;
        this.ticker = ticker;
        time = Main.unixTime();
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
        if (shutdown) {
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
        engine.put("runtime", Main.unixTime() - time);
        engine.put("online", getMain().getServer().getOnlinePlayers().size());
        engine.put("flow", flow);
        engine.put("load", calcLoad());
        engine.put("tps", ticker.get());

        try {
            if ((boolean) Invocable.class.cast(engine).invokeFunction("check")) {
                getMain().getLogger().info("Scheduled shutdown!");
                processWait = true;
                wait = getMain().getConfig().getInt("wait");
            }
        } catch (ScriptException | NoSuchMethodException ignore) {
        }

    }

    private float calcLoad() {
        Runtime runtime = Runtime.getRuntime();
        return new BigDecimal(runtime.totalMemory() - runtime.freeMemory()
        ).divide(new BigDecimal(runtime.maxMemory()), 2, RoundingMode.HALF_UP
        ).floatValue();
    }

    private void processTimeWait() {
        if (!shutdown && (wait = wait - 5) < 0) {
            processEnd();
        }
        processNotify();
    }

    private void processNotify() {
        for (Player p : getMain().getServer().getOnlinePlayers()) {
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
        getMain().getServer().getScheduler().runTaskLater(getMain(), () -> {
            if (getMain().getConfig().getBoolean("force")) {
                System.exit(0);
            } else {
                getMain().getServer().shutdown();
            }
        }, 20);
        shutdown = true;
    }

    private void processKick() {
        ByteArrayDataOutput buf = ByteStreams.newDataOutput();
        buf.writeUTF("Connect");
        buf.writeUTF(kickTo);
        byte[] data = buf.toByteArray();

        for (Player p : getMain().getServer().getOnlinePlayers()) {
            p.sendPluginMessage(getMain(), "BungeeCord", data);
        }
    }

    public void setKickTo(String kickTo) {
        this.kickTo = kickTo;
    }

}
