package com.mengcraft.reload.text;

import com.google.common.collect.Maps;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.BiFunction;

public abstract class TextAbstract extends PlaceholderExpansion {

    protected final Plugin owner;
    protected final String name;
    protected final Map<String, BiFunction<Player, String, String>> commands = Maps.newHashMap();

    public TextAbstract(Plugin owner, String name) {
        this.owner = owner;
        this.name = name;
    }

    public void addSubcommand(String command, BiFunction<Player, String, String> function) {
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
        String[] split = cmd.split("_", 2);
        BiFunction<Player, String, String> function = this.commands.get(split[0]);
        if (function == null) {
            return "null";
        }
        if (split.length == 1) {
            return String.valueOf(function.apply(player, ""));
        }
        return String.valueOf(function.apply(player, split[1]));
    }
}
