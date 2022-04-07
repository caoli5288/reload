package com.mengcraft.reload;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mengcraft.reload.citizens.CitizensListeners;
import com.mengcraft.reload.citizens.CommandsTrait;
import com.mengcraft.reload.citizens.HologramsTrait;
import com.mengcraft.reload.citizens.TermsTrait;
import com.mengcraft.reload.command.CommandConnect;
import com.mengcraft.reload.command.CommandEcho;
import com.mengcraft.reload.command.CommandExit;
import com.mengcraft.reload.command.at.CommandAt;
import com.mengcraft.reload.command.at.CommandAtq;
import com.mengcraft.reload.command.at.CommandEvery;
import com.mengcraft.reload.command.curl.CommandCurl;
import com.mengcraft.reload.util.AdvanceTilesMechanism;
import com.mengcraft.reload.util.NMS;
import com.mengcraft.reload.variable.TimeVariable;
import com.sun.management.HotSpotDiagnosticMXBean;
import lombok.Getter;
import lombok.SneakyThrows;
import me.clip.placeholderapi.PlaceholderAPI;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.TraitFactory;
import net.citizensnpcs.api.trait.TraitInfo;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Created on 16-8-7.
 */
public class Main extends JavaPlugin {

    private static ScheduledExecutorService async;
    @Getter
    private static Ticker ticker;
    @Getter
    private static Main instance;
    boolean shutdown;
    private List<String> kick;
    private Thread primary;
    private Future<?> bootstrapWatchdog;
    private boolean serverValid;
    private static boolean papi;
    @Getter
    private static NMS nms;
    @Getter
    private static boolean spigot;

    @Override
    public void onLoad() {
        instance = this;
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            spigot = true;
        } catch (ClassNotFoundException e) {
            // ignore
        }
        nms = new NMS();
        primary = Thread.currentThread();
        async = Executors.newSingleThreadScheduledExecutor();
        saveDefaultConfig();
        reloadConfig();

        int s = getConfig().getInt("bootstrap_watchdog", 600);
        if (s > 0) {
            bootstrapWatchdog = async.schedule(() -> shutdown(true), s, TimeUnit.SECONDS);
            getLogger().info("Schedule bootstrap watchdog task");
        }
    }

    @Override
    public void onEnable() {
        FileConfiguration config = getConfig();
        if (config.getBoolean("valid_sqlite")) {
            try {
                validSQLite();
            } catch (SQLException thr) {
                getLogger().log(Level.SEVERE, thr + "", thr);
                shutdown(false);
                return;
            }
        }

        if (spigot && config.getBoolean("extension.advance_tiles_mechanism")) {
            Bukkit.getScheduler().runTaskTimer(this, new AdvanceTilesMechanism(), 1200, 1200);// Update per min
        }

        if (bootstrapWatchdog != null) {
            Bukkit.getScheduler().runTask(this, () -> {
                bootstrapWatchdog.cancel(false);
                getLogger().info("Cancel bootstrap watchdog task");
            });
        }

        ticker = new Ticker();

        String expr = config.getString("control.expr");

        if (config.getBoolean("control.enable", true) && !Utils.isNullOrEmpty(expr)) {
            MachineListener l = new MachineListener(this, Machine.build(expr));

            getServer().getPluginManager().registerEvents(l, this);
            getServer().getScheduler().runTaskTimer(this, l, 0, 200);
        }

        kick = config.getStringList("kick_to");
        if (!kick.isEmpty()) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        }

        serverValid = config.getBoolean("valid_server_alive", true);
        async.scheduleAtFixedRate(() -> {
            ticker.update();
            if (serverValid && ticker.getShort() < 1 && nms.mServer$isRunning()) {
                getLogger().log(Level.SEVERE, "TPS < 1, preparing kill server");
                if (config.getBoolean("extension.auto_dump")) {
                    dump();
                }
                getLogger().log(Level.SEVERE, "Kill server");
                shutdown(true);
            }
        }, 15, 15, TimeUnit.SECONDS);

        getServer().getScheduler().runTaskTimer(this, ticker, config.getInt("wait") * 20L, 20);

        PluginHelper.addExecutor(this, new Uptime());
        PluginHelper.addExecutor(this, "at", "at.use", new CommandAt());
        PluginHelper.addExecutor(this, "atq", "atq.use", new CommandAtq());
        PluginHelper.addExecutor(this, "every", "every.use", new CommandEvery());
        PluginHelper.addExecutor(this, "sudo", "sudo.use", this::sudo);
        PluginHelper.addExecutor(this, "dumpmemory", "dumpmemory.use", (sender, list) -> dump());

        PluginHelper.addExecutor(this, "halt", "halt.use", (who, input) -> shutdown(true));
        PluginHelper.addExecutor(this, "shutdown", "shutdown.use", (who, input) -> {
            who.sendMessage(ChatColor.RED + "System shutdown...");
            shutdown0();
        });
        PluginHelper.addExecutor(this, "async", "async.use", this::async);
        PluginHelper.addExecutor(this, "rconnect", "rconnect.use", new CommandConnect());
        PluginHelper.addExecutor(this, "echo", "echo.use", new CommandEcho());
        PluginHelper.addExecutor(this, "curl", "curl.use", new CommandCurl());
        PluginHelper.addExecutor(this, "exit", "exit.use", new CommandExit());

        config.getStringList("schedule").forEach(this::runCommand);

        // variables
        PluginManager pm = Bukkit.getPluginManager();
        if (pm.getPlugin("PlaceholderAPI") != null) {
            papi = true;
            new TimeVariable(this, "time").register();
        }
        if (pm.getPlugin("Citizens") != null) {
            TraitFactory tf = CitizensAPI.getTraitFactory();
            tf.registerTrait(TraitInfo.create(TermsTrait.class));
            TraitInfo commands = TraitInfo.create(CommandsTrait.class);
            tf.deregisterTrait(commands);// force resolve conflicts
            tf.registerTrait(commands);
            TraitInfo info = TraitInfo.create(HologramsTrait.class);
            tf.deregisterTrait(info);
            tf.registerTrait(info);
            try {
                Class.forName("net.citizensnpcs.trait.HologramTrait");
            } catch (ClassNotFoundException e) {
                tf.registerTrait(TraitInfo.create(HologramsTrait.class).withName("hologramtrait"));
            }
            pm.registerEvents(new CitizensListeners(), this);
        }
    }

    void shutdown0() {
        if (!shutdown) {
            shutdown = true;
            if (!Utils.isNullOrEmpty(kick)) {
                shutdown();
                return;
            }
            new AwaitHaltLoop(this).runTaskTimer(this, 20, 20);
        }
    }

    private void async(CommandSender sender, List<String> params) {
        String joins = String.join(" ", params);
        async.execute(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), joins));
    }

    private void sudo(CommandSender who, List<String> input) {
        Iterator<String> itr = input.iterator();
        if (itr.hasNext()) {
            Player p = Bukkit.getPlayerExact(itr.next());
            if (p == null) {
                who.sendMessage("player not online");
                return;
            }
            if (!itr.hasNext()) {
                who.sendMessage("command missing");
                return;
            }
            StringJoiner joiner = new StringJoiner(" ");
            while (itr.hasNext()) {
                joiner.add(itr.next());
            }
            p.chat(String.valueOf(joiner));
        } else {
            who.sendMessage("/sudo <player> <command...>");
        }
    }

    private void validSQLite() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            DriverManager.getConnection("jdbc:sqlite::memory:").close();
        } catch (Throwable thr) {
            throw new SQLException("valid sqlite fail", thr);
        }
    }

    public void runCommand(String command) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(this, () -> runCommand(command));
            return;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    @SneakyThrows
    public void shutdown(boolean force) {
        if (force) {
            String system = System.getProperty("os.name").toLowerCase();

            String n = ManagementFactory.getRuntimeMXBean().getName();
            String pid = n.substring(0, n.indexOf('@'));

            if (system.contains("windows")) {
                ProcessBuilder b = new ProcessBuilder("taskkill", "/f", "/pid", pid);
                b.start();
            } else {
                ProcessBuilder b = new ProcessBuilder("kill", "-9", pid);
                b.start();
            }
        } else {
            async.schedule(() -> shutdown(true), getConfig().getInt("force_wait", 120), TimeUnit.SECONDS);
            // Try common way first
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), getConfig().getString("stop_command", "stop"));
        }
    }

    @Override
    public void onDisable() {
        if (!(async == null)) async.shutdown();
    }

    public void shutdown() {
        shutdown(false);
    }

    public void dump() {
        String filename = "dump-" + LocalDateTime.now() + ".hprof";
        HotSpotDiagnosticMXBean diagnostic = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        try {
            diagnostic.dumpHeap(filename, false);
        } catch (IOException e) {
        }
        getLogger().log(Level.INFO, "Heap dumped to " + filename);
    }

    public void kickAll() {
        if (kick.isEmpty()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.kickPlayer(getConfig().getString("message.notify"));
            }
        } else {
            ByteArrayDataOutput buf = ByteStreams.newDataOutput();
            buf.writeUTF("Connect");
            buf.writeUTF(nextKickTo());

            byte[] data = buf.toByteArray();

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getListeningPluginChannels().contains("BungeeCord")) {
                    p.sendPluginMessage(this, "BungeeCord", data);
                } else {
                    p.kickPlayer(getConfig().getString("message.notify"));
                }
            }
        }
    }

    public String nextKickTo() {
        if (kick.isEmpty()) return null;
        int i = ThreadLocalRandom.current().nextInt(kick.size());
        return kick.get(i);
    }

    public void run(Runnable r, int delay, int i) {
        getServer().getScheduler().runTaskTimer(this, r, delay, i);
    }

    public void run(Runnable r, int delay) {
        getServer().getScheduler().runTaskLater(this, r, delay);
    }

    public static ScheduledExecutorService executor() {
        return async;
    }

    public static String format(Player p, String s) {
        if (Utils.isNullOrEmpty(s)) {
            return s;
        }
        if (p != null) {
            if (papi) {
                return PlaceholderAPI.setPlaceholders(p, s);
            } else {
                s = s.replace("%player_name%", p.getName());
            }
        }
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static class AwaitHaltLoop extends BukkitRunnable {

        private final Main plugin;
        private int cnt;

        AwaitHaltLoop(Main plugin) {
            this.plugin = plugin;
        }

        @Override
        public void run() {
            cnt++;
            if (cnt >= 60 || Bukkit.getOnlinePlayers().isEmpty()) {
                cancel();
                plugin.shutdown();
                return;
            }
            plugin.kickAll();
        }
    }

}
