package com.mengcraft.reload;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.sun.management.HotSpotDiagnosticMXBean;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.logging.Level;

/**
 * Created on 16-8-7.
 */
public class Main extends JavaPlugin {

    private ScheduledExecutorService async;
    boolean shutdown;
    private List<String> kick;

    private final Map<Integer, Runner> scheduler = new HashMap<>();
    private int id;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    @Getter
    private static Ticker ticker;
    private Thread primary;
    private Future<?> bootstrapWatchdog;
    private boolean serverValid;

    @Override
    public void onLoad() {
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

        if (bootstrapWatchdog != null) {
            Bukkit.getScheduler().runTask(this, () -> {
                bootstrapWatchdog.cancel(false);
                getLogger().info("Cancel bootstrap watchdog task");
            });
        }

        ticker = new Ticker();

        String expr = config.getString("control.expr");

        if (config.getBoolean("control.enable", true) && !Utils.isNullOrEmpty(expr)) {
            MainListener l = new MainListener(this, Machine.build(expr));

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
            if (serverValid && ticker.getShort() < 1) {
                getLogger().log(Level.SEVERE, "TPS < 1, preparing kill server");
                for (StackTraceElement element : primary.getStackTrace()) {
                    getLogger().warning("\tat " + element);
                }
                if (config.getBoolean("extension.auto_dump")) {
                    dump();
                }
                getLogger().log(Level.SEVERE, "Kill server");
                shutdown(true);
            }
        }, 15, 15, TimeUnit.SECONDS);

        getServer().getScheduler().runTaskTimer(this, ticker, config.getInt("wait") * 20L, 20);

        PluginHelper.addExecutor(this, new Uptime());
        PluginHelper.addExecutor(this, "at", "at.use", this::at);
        PluginHelper.addExecutor(this, "atq", "atq.use", this::atq);
        PluginHelper.addExecutor(this, "every", "every.use", this::every);
        PluginHelper.addExecutor(this, "sudo", "sudo.use", this::sudo);
        PluginHelper.addExecutor(this, "dumpmemory", "dumpmemory.use", (sender, list) -> dump());

        PluginHelper.addExecutor(this, "halt", "halt.use", (who, input) -> shutdown(true));
        PluginHelper.addExecutor(this, "shutdown", "shutdown.use", (who, input) -> {
            who.sendMessage(ChatColor.RED + "System shutdown...");
            if (!shutdown) {
                shutdown = true;
                if (!Utils.isNullOrEmpty(kick)) {
                    shutdown();
                    return;
                }
                new AwaitHaltLoop(this).runTaskTimer(this, 20, 20);
            }
        });
        PluginHelper.addExecutor(this, "async", this::async);

        config.getStringList("schedule").forEach(l -> {
            val itr = Arrays.asList(l.trim().split(" ", 2)).iterator();
            try {
                Type type = Type.valueOf(itr.next().toUpperCase());
                List<String> input = Arrays.asList(itr.next().split(" "));
                if (type == Type.AT) {
                    at(Bukkit.getConsoleSender(), input);
                } else {
                    every(Bukkit.getConsoleSender(), input);
                }
            } catch (Exception ign) {
                getLogger().warning("!!! Err schedule line -> " + l);
            }
        });
    }

    private void async(CommandSender sender, List<String> params) {
        String joins = String.join(" ", params);
        async.execute(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), joins));
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

    void atq(CommandSender who, List<String> input) {
        if (input.isEmpty()) {
            scheduler.forEach((id, runner) -> {
                Future<?> future = runner.future;
                if (!(future.isCancelled() || future.isDone())) {
                    who.sendMessage(id + " " + runner.desc);
                }
            });
            who.sendMessage("Type '/atq a' to check all(cancelled and done) or '/atq c <id>' to cancel");
        } else {
            atq(who, input.iterator());
        }
    }

    private final Map<String, BiConsumer<CommandSender, String>> subProcessor = ImmutableMap.of(
            "/atq a", (who, __i) -> {
                scheduler.forEach((i, runner) -> {
                    Future<?> future = runner.future;
                    String l = i + " " + runner.desc;
                    if (future.isCancelled()) {
                        l += " <- cancelled";
                    } else if (future.isDone()) {
                        l += " <- done";
                    }
                    who.sendMessage(l);
                });
            },
            "/atq c", (who, del) -> {
                if (del == null) {
                    who.sendMessage(ChatColor.RED + "Syntax error");
                    return;
                }
                Runner runner = scheduler.get(Integer.valueOf(del));
                if (runner == null) {
                    who.sendMessage(ChatColor.RED + "Id not found error");
                    return;
                }
                Future<?> future = runner.future;
                if (future.isDone() || future.isCancelled()) {
                    who.sendMessage(ChatColor.RED + "Already done or cancelled error");
                    return;
                }

                future.cancel(true);
                who.sendMessage(del + " " + runner.desc + " cancelled");
            }
    );

    void atq(CommandSender who, Iterator<String> itr) {
        BiConsumer<CommandSender, String> consumer = subProcessor.get("/atq " + itr.next());
        if (consumer == null) {
            who.sendMessage(ChatColor.RED + "Unknown command error");
        } else {
            consumer.accept(who, itr.hasNext() ? itr.next() : null);
        }
    }

    void at(CommandSender who, List<String> input) {
        val itr = input.iterator();
        val label = itr.next();
        if (!itr.hasNext()) {
            throw new IllegalArgumentException("no command");
        }

        val runner = new Runner(toTimeAt(label), -1, join(itr, ' '));
        runner.future = async.schedule(() -> runCommand(runner.run), runner.until(), TimeUnit.MILLISECONDS);
        runner.desc = dateFormat.format(new Date()) + " -> at " + label + " " + runner.run;
        scheduler.put(++id, runner);

        who.sendMessage(id + " " + runner.desc);
    }

    private void runCommand(String command) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(this, () -> runCommand(command));
            return;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    static LocalDateTime toTimeAt(String input) {
        try {
            LocalTime clock = LocalTime.parse(input);
            if (clock.isAfter(LocalTime.now())) {
                return LocalDateTime.of(LocalDate.now(), clock);
            }

            return LocalDateTime.of(LocalDate.now().plusDays(1), clock);
        } catch (Exception ign) {
            //
        }

        try {
            LocalDateTime next = LocalDateTime.parse(input);
            if (!next.isAfter(LocalDateTime.now())) {
                throw new IllegalArgumentException("invalid datetime" + input);
            }
            return next;
        } catch (DateTimeParseException ign) {
            //
        }

        if (input.matches("\\+[0-9]+[smhd]")) {
            long unit = timeUnit.get(String.valueOf(input.charAt(input.length() - 1)));
            return LocalDateTime.now().plus(Long.parseLong(input.substring(1, input.length() - 1)) * unit, ChronoUnit.MILLIS);
        }

        throw new IllegalArgumentException("syntax err " + input);
    }

    void every(CommandSender who, List<String> input) {
        val itr = input.iterator();
        val label = itr.next();
        if (!itr.hasNext()) {
            throw new IllegalArgumentException("no command");
        }

        Runner runner = toRunner(label, join(itr, ' '));
        if (runner.period == -1) {
            runner.future = async.scheduleAtFixedRate(() -> runCommand(runner.run), runner.until(), TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
        } else {
            runner.future = async.scheduleAtFixedRate(() -> runCommand(runner.run), runner.until(), runner.period, TimeUnit.MILLISECONDS);
        }

        runner.desc = dateFormat.format(new Date()) + " -> every " + label + " " + runner.run;
        scheduler.put(++id, runner);

        who.sendMessage(id + " " + runner.desc);
    }

    static Map<String, Long> timeUnit = ImmutableMap.of("s", 1000L, "m", 60000L, "h", 3600000L, "d", 86400000L);

    @SneakyThrows
    static Runner toRunner(String input, String run) {
        if (input.matches("[0-9]+[smhd]")) {
            long unit = timeUnit.get(String.valueOf(input.charAt(input.length() - 1)));
            long l = Long.parseLong(input.substring(0, input.length() - 1)) * unit;
            return new Runner(LocalDateTime.now().plus(l, ChronoUnit.MILLIS), l, run);
        }

        LocalTime clock = LocalTime.parse(input);
        if (clock.isAfter(LocalTime.now())) {
            return new Runner(LocalDateTime.of(LocalDate.now(), clock), TimeUnit.DAYS.toMillis(1), run);
        }

        return new Runner(LocalDateTime.of(LocalDate.now().plusDays(1), clock), -1, run);
    }

    static <E> String join(Iterator<E> i, char separator) {
        val buf = new StringBuilder();
        i.forEachRemaining(l -> {
            if (buf.length() > 0) buf.append(separator);
            buf.append(l);
        });
        return buf.toString();
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

    enum Type {

        AT,
        EVERY;
    }

    @Data
    static class Runner {

        private final LocalDateTime nextTime;
        private final long period;
        private final String run;
        private Future<?> future;
        private String desc;

        public long until() {
            return LocalDateTime.now().until(nextTime, ChronoUnit.MILLIS);
        }
    }

    public void run(Runnable r, int delay, int i) {
        getServer().getScheduler().runTaskTimer(this, r, delay, i);
    }

    public void run(Runnable r, int delay) {
        getServer().getScheduler().runTaskLater(this, r, delay);
    }

}
