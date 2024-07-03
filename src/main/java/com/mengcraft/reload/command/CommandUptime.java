package com.mengcraft.reload.command;

import com.mengcraft.reload.Main;
import com.mengcraft.reload.PluginHelper;
import com.mengcraft.reload.Ticker;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class CommandUptime extends PlaceholderExpansion implements PluginHelper.IExec {

    private static final long MIN = 60;
    private static final long H = 3600;
    private static final long DAY = 86400;

    private final LocalDateTime time = LocalDateTime.now();

    public CommandUptime() {
        register();
    }

    @Override
    public void exec(CommandSender sender, List<String> list) {
        Ticker ticker = Main.getTicker();
        sender.sendMessage(time.toLocalDate() + " up " + time() + ", " + ticker.tick() + " tick(s); " +
                "Load avg: " + colour(ticker.getShort()) + ", " + colour(ticker.getMedium()) +
                ", " + colour(ticker.getLong()));
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        // %uptime_time%
        // %uptime_ticks%
        // %uptime_tps1%
        // %uptime_tps2%
        // %uptime_tps3%
        switch (params) {
            case "time":
                return String.valueOf(time.until(LocalDateTime.now(), ChronoUnit.SECONDS));
            case "ticks":
                return String.valueOf(Main.getTicker().tick());
            case "tps":
            case "tps1":
                return String.valueOf(Main.getTicker().getShort());
            case "tps2":
                return String.valueOf(Main.getTicker().getMedium());
            case "tps3":
                return String.valueOf(Main.getTicker().getLong());
            default:
                return "0";
        }
    }

    @Override
    public @NotNull String getIdentifier() {
        return "uptime";
    }

    @Override
    public @NotNull String getAuthor() {
        return "caoli5288";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    private String colour(float num) {
        if (num > 18) return colour(num, 'a');
        if (num > 14) return colour(num, 'e');
        return colour(num, ChatColor.RED.getChar());
    }

    private String colour(Object i, char l) {
        return "§" + l + i + "§r";
    }

    private String time() {
        long l = ChronoUnit.SECONDS.between(time, LocalDateTime.now());
        return toTimeString(l);
    }

    public static String toTimeString(long l) {
        if (l > DAY) {
            return (l / DAY) + " day(s), " + hour(l);
        } else if (l > H) {
            return hour(l);
        }
        return (l / MIN) + " minute(s)";
    }

    private static String hour(long l) {
        return ((l % DAY) / H) + "h" + ((l % H) / MIN) + "m";
    }
}
