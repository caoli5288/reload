package com.mengcraft.reload.command.control;

import com.mengcraft.reload.Main;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class CommandCallable implements ICallable {

    private final String command;

    @Override
    public boolean call(CommandSender caller) {
        return Bukkit.dispatchCommand(caller, Main.format(caller instanceof Player ? (Player) caller : null, command));
    }
}
