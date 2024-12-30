package com.mengcraft.reload.text;

import com.google.common.collect.Maps;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public abstract class TextAbstract extends PlaceholderExpansion {

    protected final Plugin owner;
    protected final String name;
    protected final Map<String, BiFunction<Player, List<String>, String>> commands = Maps.newHashMap();

    public TextAbstract(Plugin owner, String name) {
        this.owner = owner;
        this.name = name;
    }

    public void addSubcommand(String command, BiFunction<Player, List<String>, String> function) {
        commands.put(command, function);
    }

    @Override
    public @NotNull String getIdentifier() {
        return name;
    }

    @Override
    public @NotNull String getAuthor() {
        return owner.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return owner.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String cmd) {
        String[] split = cmd.split("_");
        BiFunction<Player, List<String>, String> function = commands.get(split[0]);
        if (function == null) {
            function = commands.get("");
        }
        if (function == null) {
            return "null";
        }
        return String.valueOf(function.apply(player, Arrays.asList(split)));
    }
}
