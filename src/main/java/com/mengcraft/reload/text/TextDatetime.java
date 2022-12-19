package com.mengcraft.reload.text;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mengcraft.reload.Utils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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

    public String until(Player who, String cmd) {
        // %datetime_until_2099-01-01T00:00% => 16384
        LocalDateTime time = LocalDateTime.parse(cmd);
        LocalDateTime now = LocalDateTime.now();
        if (time.isAfter(now)) {
            return String.valueOf(now.until(time, ChronoUnit.SECONDS));
        }
        return String.valueOf(-time.until(now, ChronoUnit.SECONDS));
    }

    public String now(Player who, String cmd) {
        // %datetime_now% => 2023-01-01T00:00
        // %datetime_now_yyyyMMdd% => 20230101
        if (Utils.isNullOrEmpty(cmd)) {
            return LocalDateTime.now().toString();
        }
        return LocalDateTime.now().format(formatters.getUnchecked(cmd));
    }
}
