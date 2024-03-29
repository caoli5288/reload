package com.mengcraft.reload.command.at;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mengcraft.reload.Main;
import com.mengcraft.reload.Utils;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.val;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public enum AtScheduler {

    INSTANCE;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final Map<Integer, Runner> scheduler = Maps.newHashMap();
    private int nextId;

    public static AtScheduler getInstance() {
        return INSTANCE;
    }

    public void at(CommandSender who, String label, String commands) {
        LocalDateTime at = toTimeAt(label);
        if (at == null) {
            who.sendMessage(String.format("Cannot schedule task at %s", label));
            return;
        }
        val runner = new Runner(at, -1, commands);
        runner.future = Main.executor().schedule(() -> Main.getInstance().runCommand(runner.run), runner.until(), TimeUnit.MILLISECONDS);
        runner.desc = DATE_FORMAT.format(new Date()) + " -> at " + label + " " + runner.run;
        scheduler.put(++nextId, runner);

        who.sendMessage(nextId + " " + runner.desc);
    }

    @Nullable
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
            if (next.isAfter(LocalDateTime.now())) {
                return next;
            }
            return null;
        } catch (DateTimeParseException ign) {
            //
        }

        if (input.matches("\\+[0-9]+[smhd]")) {
            long unit = TIME_UNIT.get(String.valueOf(input.charAt(input.length() - 1)));
            return LocalDateTime.now().plus(Long.parseLong(input.substring(1, input.length() - 1)) * unit, ChronoUnit.MILLIS);
        }

        return null;
    }

    public void every(CommandSender who, String label, String commands) {
        Runner runner = toRunner(label, commands);
        if (runner == null) {
            who.sendMessage(String.format("Cannot schedule task every %s", label));
            return;
        }

        runner.future = Main.executor().scheduleAtFixedRate(() -> Main.getInstance().runCommand(runner.run), runner.until(), runner.period, TimeUnit.MILLISECONDS);
        runner.desc = DATE_FORMAT.format(new Date()) + " -> every " + label + " " + runner.run;
        scheduler.put(++nextId, runner);

        who.sendMessage(nextId + " " + runner.desc);
    }

    private final static Map<String, Long> TIME_UNIT = ImmutableMap.of("s", 1000L, "m", 60000L, "h", 3600000L, "d", 86400000L);

    static Runner toRunner(String input, String run) {
        if (input.matches("[0-9]+[smhd]")) {
            long unit = TIME_UNIT.get(String.valueOf(input.charAt(input.length() - 1)));
            long l = Long.parseLong(input.substring(0, input.length() - 1)) * unit;
            return new Runner(LocalDateTime.now().plus(l, ChronoUnit.MILLIS), l, run);
        }

        try {
            LocalTime clock = LocalTime.parse(input);
            if (clock.isAfter(LocalTime.now())) {
                return new Runner(LocalDateTime.of(LocalDate.now(), clock), TimeUnit.DAYS.toMillis(1), run);
            }

            return new Runner(LocalDateTime.of(LocalDate.now().plusDays(1), clock), TimeUnit.DAYS.toMillis(1), run);
        } catch (Exception ign) {
            //
        }

        return null;
    }

    public void atq(CommandSender who, String label, String param) {
        if (Utils.isNullOrEmpty(label)) {
            scheduler.forEach((id, runner) -> {
                Future<?> future = runner.future;
                if (!(future.isCancelled() || future.isDone())) {
                    who.sendMessage(id + " " + runner.desc);
                }
            });
            who.sendMessage("Type '/atq a' to check all(cancelled and done) or '/atq c <id>' to cancel");
        } else if (label.equals("a")) {
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
        } else if (label.equals("c")) {
            if (param == null) {
                who.sendMessage(ChatColor.RED + "Syntax error");
                return;
            }
            Runner runner = scheduler.get(Integer.valueOf(param));
            if (runner == null) {
                who.sendMessage(ChatColor.RED + "Task " + param +
                        " not found");
                return;
            }
            Future<?> future = runner.future;
            if (future.isDone() || future.isCancelled()) {
                who.sendMessage(ChatColor.RED + "Task already done or cancelled");
                return;
            }

            future.cancel(true);
            who.sendMessage(param + " " + runner.desc + " cancelled");
        }
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
}
