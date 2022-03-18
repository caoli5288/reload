package com.mengcraft.reload.command;

import com.mengcraft.reload.Main;
import com.mengcraft.reload.PluginHelper;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.List;

public class CommandExit implements PluginHelper.IExec {

    @Override
    public void exec(CommandSender sender, List<String> list) {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            Main.getInstance().shutdown();
        }
    }
}
