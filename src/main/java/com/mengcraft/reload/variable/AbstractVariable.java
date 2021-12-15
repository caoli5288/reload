package com.mengcraft.reload.variable;

import com.google.common.collect.Maps;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public abstract class AbstractVariable extends PlaceholderExpansion {

    protected final Plugin owner;
    protected final String name;
    protected final Map<String, BiFunction<Player, List<String>, String>> subjects = Maps.newHashMap();

    public AbstractVariable(Plugin owner, String name) {
        this.owner = owner;
        this.name = name;
    }

    public void addSubject(String subject, BiFunction<Player, List<String>, String> function) {
        subjects.put(subject, function);
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
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        List<String> commands = Arrays.asList(params.split("_"));
        BiFunction<Player, List<String>, String> function = subjects.get(commands.get(0));
        if (function == null) {
            return "null";
        }
        return String.valueOf(function.apply(player, commands));
    }
}
