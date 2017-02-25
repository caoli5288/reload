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

    private final Ticker ticker;
    private final String date;
    private final long up;

    public Uptime(Ticker ticker) {
        super("uptime");
        this.ticker = ticker;
        up = System.currentTimeMillis();
        date = LocalDate.now().toString();
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] i) {
        if (sender.hasPermission("uptime.use")) {
            sender.sendMessage(date + " up " + time() + ", " + ticker.tick() + " tick(s); " +
                    "Load avg: " + t(ticker.getShort()) + ", " + t(ticker.getMed()) +
                    ", " + t(ticker.getLong()));
            return true;
        }
        return false;
    }

    private String t(float t) {
        if (t > 18) return l(t, 'a');
        if (t > 12) return l(t, 'e');
        return l(t, ChatColor.RED.getChar());
    }

    private String l(Object i, char l) {
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
