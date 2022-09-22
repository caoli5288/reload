package com.mengcraft.reload.command;

import com.mengcraft.reload.Main;
import com.mengcraft.reload.PluginHelper;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

public class CommandCmd implements PluginHelper.IExec {

    @Override
    public void exec(CommandSender sender, List<String> list) {
        // cmd <player_name> <command...>
        if (list.size() > 1) {
            Player exact = Bukkit.getPlayerExact(list.get(0));
            Objects.requireNonNull(exact);
            String commands = String.join(" ", list.subList(1, list.size()));
            Bukkit.dispatchCommand(sender, Main.format(exact, commands));
        }
    }
}
