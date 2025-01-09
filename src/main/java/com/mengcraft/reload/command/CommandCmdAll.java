package com.mengcraft.reload.command;

import com.mengcraft.reload.Main;
import com.mengcraft.reload.PluginHelper;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

public class CommandCmdAll implements PluginHelper.IExec {

    @Override
    public void exec(CommandSender sender, List<String> list) {
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        if (!onlinePlayers.isEmpty() && !list.isEmpty()) {
            execute(sender, onlinePlayers, String.join(" ", list));
        }
    }

    public static void execute(CommandSender console, Collection<? extends Player> list, String commands) {
        for (Player it : list) {
            String cmdline = Main.format(it, commands);
            try {
                Bukkit.dispatchCommand(console, cmdline);
            } catch (Exception e) {
                Main.log("execute " + cmdline, e);
            }
        }
    }
}
