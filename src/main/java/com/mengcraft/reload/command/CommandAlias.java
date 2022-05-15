package com.mengcraft.reload.command;

import com.mengcraft.reload.PluginHelper;
import com.mengcraft.reload.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

public class CommandAlias implements PluginHelper.IExec {

    @Override
    public void exec(CommandSender sender, List<String> cmd) {
        String s = cmd.get(0);
        Command command = Utils.getCommandMap().getCommand(s);
        if (command == null) {
            sender.sendMessage("§cCommand §e" + s + " §cnot found.");
        } else {
            sender.sendMessage("§aCommand §e" + s + " §ais alias of §e" + command.getName());
        }
    }
}
