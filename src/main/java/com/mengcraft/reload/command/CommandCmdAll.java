package com.mengcraft.reload.command;

import com.mengcraft.reload.Main;
import com.mengcraft.reload.PluginHelper;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandCmdAll implements PluginHelper.IExec {

    @Override
    public void exec(CommandSender sender, List<String> list) {
        if (!list.isEmpty()) {
            String commands = String.join(" ", list);
            for (Player online : Bukkit.getOnlinePlayers()) {
                String cmdline = Main.format(online, commands);
                try {
                    Bukkit.dispatchCommand(sender, cmdline);
                } catch (Exception e) {
                    Main.log("execute " + cmdline, e);
                }
            }
        }
    }
}
