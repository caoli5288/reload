package com.mengcraft.reload.command.control;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

@RequiredArgsConstructor
public class CommandCallable implements ICallable {

    private final String command;

    @Override
    public boolean call(CommandSender caller) {
        return Bukkit.dispatchCommand(caller, command);
    }
}
