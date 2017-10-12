package com.mengcraft.reload;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.time.LocalDate;

/**
 * Created on 17-2-5.
 */
public class Uptime extends Command {

    private static final long DAY = 86400000;
    private static final long HOUR = 3600000;
    private static final long MIN = 60000;

    private final String date;
    private final long up;

    Uptime() {
        super("uptime");
        setPermission("uptime.use");
        up = System.currentTimeMillis();
        date = LocalDate.now().toString();
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] i) {
        if (testPermission(sender)) {
            Ticker ticker = Ticker.INST;
            sender.sendMessage(date + " up " + time() + ", " + ticker.tick() + " tick(s); " +
                    "Load avg: " + colour(ticker.getShort()) + ", " + colour(ticker.getMed()) +
                    ", " + colour(ticker.getLong()));
            return true;
        }
        return false;
    }

    private String colour(float num) {
        if (num > 18) return colour(num, 'a');
        if (num > 12) return colour(num, 'e');
        return colour(num, ChatColor.RED.getChar());
    }

    private String colour(Object i, char l) {
        return "§" + l + i + "§r";
    }

    private String time() {
        long l = System.currentTimeMillis() - up;
        if (l > DAY) {
            return (l / DAY) + " day(s), " + hour(l);
        } else if (l > HOUR) {
            return hour(l);
        }
        return (l / MIN) + " min(s)";
    }

    private String hour(long l) {
        return ((l % DAY) / HOUR) + ":" + ((l % HOUR) / MIN);
    }

}
