package com.mengcraft.reload;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Created on 17-2-5.
 */
public class Uptime extends Command {

    private static final long MIN = 60;
    private static final long H = 3600;
    private static final long DAY = 86400;

    private String colour(Object i, char l) {
        return "§" + l + i + "§r";
    }

    private final LocalDateTime up = LocalDateTime.now();

    Uptime() {
        super("uptime");
        setPermission("uptime.use");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] i) {
        if (testPermission(sender)) {
            Ticker ticker = Main.getTicker();
            sender.sendMessage(up.toLocalDate() + " up " + time() + ", " + ticker.tick() + " tick(s); " +
                    "Load avg: " + colour(ticker.getShort()) + ", " + colour(ticker.getMedium()) +
                    ", " + colour(ticker.getLong()));
            return true;
        }
        return false;
    }

    private String colour(float num) {
        if (num > 18) return colour(num, 'a');
        if (num > 14) return colour(num, 'e');
        return colour(num, ChatColor.RED.getChar());
    }

    private String time() {
        long l = ChronoUnit.SECONDS.between(up, LocalDateTime.now());
        return toTimeString(l);
    }

    public static String toTimeString(long l) {
        if (l > DAY) {
            return (l / DAY) + " day(s), " + hour(l);
        } else if (l > H) {
            return hour(l);
        }
        return (l / MIN) + " min(s)";
    }

    private static String hour(long l) {
        return ((l % DAY) / H) + ":" + ((l % H) / MIN);
    }
}
