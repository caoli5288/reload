package com.mengcraft.reload.command;

import com.mengcraft.reload.Main;
import com.mengcraft.reload.PluginHelper;
import org.bukkit.command.CommandSender;

import java.util.List;

public class CommandShutdown implements PluginHelper.IExec {

    @Override
    public void exec(CommandSender sender, List<String> list) {
        Main main = Main.getInstance();
        main.getLogger().info("Server is shutdown");
        main.safeShutdown();
    }
}
