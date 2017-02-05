package com.mengcraft.reload;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.sql.Timestamp;

/**
 * Created on 17-2-5.
 */
public class Uptime extends Command {

    private static final long DAY = 86400000;
    private static final long HOUR = 3600000;
    private static final long MIN = 60000;

    private final Timestamp boot;
    private final Ticker ticker;

    public Uptime(Ticker ticker) {
        super("uptime");
        this.ticker = ticker;
        boot = new Timestamp(System.currentTimeMillis());
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] i) {
        if (sender.hasPermission("uptime.use")) {
            sender.sendMessage(String.valueOf(boot) + " up " + min() + ", " + ticker.i() + " tick(s); " +
                    "Load average " + ticker.get1() + ", " + ticker.get2() + ", " + ticker.get3());
            return true;
        }
        return false;
    }

    private String min() {
        long l = System.currentTimeMillis() - boot.getTime();
        if (l > DAY) {
            return (l / DAY) + " day(s), " + ((l % DAY) / HOUR) + ":" + ((l % HOUR) / MIN);
        } else if (l > HOUR) {
            return ((l % DAY) / HOUR) + ":" + ((l % HOUR) / MIN);
        }
        return (l / MIN) + " min(s)";
    }

}
