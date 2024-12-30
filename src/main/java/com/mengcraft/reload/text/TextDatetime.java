package com.mengcraft.reload.text;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TextDatetime extends TextAbstract {

    private final LoadingCache<String, DateTimeFormatter> formatters = CacheBuilder.newBuilder()
            .softValues()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(CacheLoader.from(DateTimeFormatter::ofPattern));

    public TextDatetime(Plugin owner) {
        super(owner, "datetime");
        addSubcommand("now", this::now);
        addSubcommand("until", this::until);
    }

    public String until(Player who, List<String> cmd) {
        // %datetime_until_2099-01-01T00:00% => 16384
        if (cmd.size() == 1) {
            return "0";
        }
        LocalDateTime time = LocalDateTime.parse(cmd.get(1));
        LocalDateTime now = LocalDateTime.now();
        ChronoUnit unit = ChronoUnit.SECONDS;
        if (cmd.size() > 2) {
            unit = ChronoUnit.valueOf(cmd.get(2).toUpperCase());
        }
        if (time.isAfter(now)) {
            return String.valueOf(now.until(time, unit));
        }
        return String.valueOf(-time.until(now, unit));
    }

    public String now(Player who, List<String> cmd) {
        // %datetime_now% => 2023-01-01T00:00
        // %datetime_now_yyyyMMdd% => 20230101
        if (cmd.size() == 1) {
            return LocalDateTime.now().toString();
        }
        return LocalDateTime.now().format(formatters.getUnchecked(cmd.get(1)));
    }
}
