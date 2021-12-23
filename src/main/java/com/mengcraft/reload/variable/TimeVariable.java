package com.mengcraft.reload.variable;

import com.mengcraft.reload.Utils;
import org.bukkit.plugin.Plugin;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class TimeVariable extends AbstractVariable {

    public TimeVariable(Plugin owner, String name) {
        super(owner, name);
        addSubject("until", (player, params) -> until(params.get(1), Utils.take(params, 2)));
    }

    private String until(String s, String fmt) {
        LocalDateTime time = LocalDateTime.parse(s);
        long secs = LocalDateTime.now().until(time, ChronoUnit.SECONDS);
        if (secs > 86400) {
            return "";
        }
        if (secs < 0) {
            return "0";
        }
        LocalTime t = LocalTime.ofSecondOfDay(secs);
        if (fmt == null) {
            return t.toString();
        }
        return t.format(DateTimeFormatter.ofPattern(fmt));
    }
}
