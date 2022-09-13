package com.mengcraft.reload.command.control;

import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class CommandCallable implements ICallable {

    private final String command;

    @Override
    public boolean call(CommandSender caller) {
        if (caller instanceof Player) {
            ((Player) caller).chat("/" + PlaceholderAPI.setPlaceholders((Player) caller, command));
        }
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), PlaceholderAPI.setPlaceholders(null, command));
    }
}
