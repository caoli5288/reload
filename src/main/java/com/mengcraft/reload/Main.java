package com.mengcraft.reload;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mengcraft.reload.citizens.CitizensManager;
import com.mengcraft.reload.citizens.CommandsTrait;
import com.mengcraft.reload.citizens.HideTrait;
import com.mengcraft.reload.citizens.HologramsTrait;
import com.mengcraft.reload.citizens.TermsTrait;
import com.mengcraft.reload.command.CommandAlias;
import com.mengcraft.reload.command.CommandCmd;
import com.mengcraft.reload.command.CommandCmdAll;
import com.mengcraft.reload.command.CommandConnect;
import com.mengcraft.reload.command.CommandCpus;
import com.mengcraft.reload.command.CommandEcho;
import com.mengcraft.reload.command.CommandExit;
import com.mengcraft.reload.command.CommandLag;
import com.mengcraft.reload.command.CommandShutdown;
import com.mengcraft.reload.command.CommandUptime;
import com.mengcraft.reload.command.CommandVelocity;
import com.mengcraft.reload.command.at.CommandAt;
import com.mengcraft.reload.command.at.CommandAtq;
import com.mengcraft.reload.command.at.CommandEvery;
import com.mengcraft.reload.command.control.CommandIf;
import com.mengcraft.reload.command.curl.CommandCurl;
import com.mengcraft.reload.text.TextDatetime;
import com.mengcraft.reload.util.AdvanceTilesMechanism;
import com.mengcraft.reload.util.NMS;
import com.sun.management.HotSpotDiagnosticMXBean;
import lombok.Getter;
import lombok.SneakyThrows;
import me.clip.placeholderapi.PlaceholderAPI;
import net.citizensnpcs.api.trait.TraitInfo;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.ProxySelector;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
        async = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setNameFormat("Main-tasks-%d")
                .setDaemon(false)
                .setPriority(5)
                .build());
        saveDefaultConfig();
        reloadConfig();

        int s = getConfig().getInt("bootstrap_watchdog", 600);
        if (s > 0) {
            bootstrapWatchdog = async.schedule(() -> shutdown(true), s, TimeUnit.SECONDS);
            getLogger().info("Schedule bootstrap watchdog task");
        }
        List<Map<?, ?>> trafficRules = getConfig().getMapList("traffic_rules");
        if (!trafficRules.isEmpty()) {
            ProxySelector selector = ProxySelector.getDefault();
            ProxySelector.setDefault(new TrafficRules(selector, trafficRules));
        }
        // cpus
        if (getConfig().getInt("cpus", 0) > 0) {
            Utils.ensureLinux();
            CommandCpus.set(getConfig().getInt("cpus"));
            getLogger().info("Set cpus to " + getConfig().getInt("cpus"));
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
            getLogger().info("Use advance tiles mechanism");
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, new AdvanceTilesMechanism(), 300, 300);// Update per 15s
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

        PluginHelper.addExecutor(this, "uptime", "uptime.use", new CommandUptime());
        PluginHelper.addExecutor(this, "at", "at.use", new CommandAt());
        PluginHelper.addExecutor(this, "atq", "atq.use", new CommandAtq());
        PluginHelper.addExecutor(this, "every", "every.use", new CommandEvery());
        PluginHelper.addExecutor(this, "sudo", "sudo.use", this::sudo);
        PluginHelper.addExecutor(this, "dumpmemory", "dumpmemory.use", (sender, list) -> dump());

        PluginHelper.addExecutor(this, "halt", "halt.use", (who, input) -> shutdown(true));
        CommandShutdown commandStop = new CommandShutdown();
        PluginHelper.addExecutor(this, "shutdown", "shutdown.use", commandStop);
        PluginHelper.addExecutor(this, "stop", "stop.use", commandStop);
        PluginHelper.addExecutor(this, "async", "async.use", this::async);
        PluginHelper.addExecutor(this, "rconnect", "rconnect.use", new CommandConnect());
        PluginHelper.addExecutor(this, "echo", "echo.use", new CommandEcho());
        PluginHelper.addExecutor(this, "curl", "curl.use", new CommandCurl());
        PluginHelper.addExecutor(this, "exit", "exit.use", new CommandExit());
        PluginHelper.addExecutor(this, "velocity", "velocity.use", new CommandVelocity());
        PluginHelper.addExecutor(this, "alias", "alias.use", new CommandAlias());
        PluginHelper.addExecutor(this, "lag", "lag.use", new CommandLag());
        // Cpus command
        PluginHelper.addExecutor(this, "cpus", "cpus.use", new CommandCpus());
        PluginHelper.addExecutor(this, "if", "if.use", new CommandIf());
        PluginHelper.addExecutor(this, "cmdall", "cmdall.use", new CommandCmdAll());
        PluginHelper.addExecutor(this, "cmd", "cmd.use", new CommandCmd());

        config.getStringList("schedule").forEach(this::runCommand);

        // variables
        PluginManager pm = Bukkit.getPluginManager();
        if (pm.getPlugin("PlaceholderAPI") != null) {
            papi = true;
            new TextDatetime(this).register();
        }
        if (pm.getPlugin("Citizens") != null) {
            CitizensManager.addTrait(TraitInfo.create(TermsTrait.class));
            CitizensManager.addTrait(TraitInfo.create(CommandsTrait.class));
            CitizensManager.addTrait(TraitInfo.create(HologramsTrait.class));
            CitizensManager.addTrait(TraitInfo.create(HideTrait.class));
            try {
                Class.forName("net.citizensnpcs.trait.HologramTrait");
            } catch (ClassNotFoundException e) {
                CitizensManager.addTrait(TraitInfo.create(HologramsTrait.class).withName("hologramtrait"));
            }
            pm.registerEvents(CitizensManager.getInstance(), this);
        }

        List<?> altChecker = getConfig().getList("commands_alt_checker");
        if (!altChecker.isEmpty()) {
            pm.registerEvents(new CommandsAltChecker(altChecker), this);
        }
    }

    public void safeShutdown() {
        if (!shutdown) {
            shutdown = true;
            if (Bukkit.getOnlinePlayers().isEmpty()) {
                shutdown();
            } else {
                kickAll();
                if (Bukkit.getOnlinePlayers().isEmpty()) {
                    shutdown();
                } else {
                    // ensure players kicked
                    new AwaitHaltLoop(this).runTaskTimer(this, 20, 20);
                    Bukkit.getPluginManager().registerEvents(new Listener() {
                        @EventHandler(priority = EventPriority.HIGHEST)
                        public void onJoin(AsyncPlayerPreLoginEvent event) {
                            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "");
                        }
                        @EventHandler(priority = EventPriority.HIGHEST)
                        public void onJoin(PlayerLoginEvent event) {
                            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "");
                        }
                    }, this);
                }
            }
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
            String pid = Utils.pid();
            ProcessBuilder b = System.getProperty("os.name").toLowerCase().contains("windows") ?
                    new ProcessBuilder("taskkill", "/f", "/pid", pid) :
                    new ProcessBuilder("kill", "-9", pid);
            b.start();
        } else {
            int ttl = getConfig().getInt("force_wait", 300);
            if (ttl > 0) {
                async.schedule(() -> shutdown(true), ttl, TimeUnit.SECONDS);
            }
            // Try common way first
            Bukkit.shutdown();
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
        if (Utils.isNullOrEmpty(kick)) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.kickPlayer(getConfig().getString("message.notify"));
            }
        } else if (getConfig().getBoolean("extension.kick_fallback")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.kickPlayer("fallback/" + nextKickTo());
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
        if (papi) {
            return PlaceholderAPI.setPlaceholders(p, s);
        }
        s = s.replace("%player_name%", p.getName());
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void log(String msg, Exception e) {
        getInstance().getLogger().log(Level.WARNING, msg, e);
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
