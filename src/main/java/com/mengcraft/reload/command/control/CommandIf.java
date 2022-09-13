package com.mengcraft.reload.command.control;

import com.mengcraft.reload.PluginHelper;
import org.bukkit.command.CommandSender;

import java.util.List;

public class CommandIf implements PluginHelper.IExec {

    @Override
    public void exec(CommandSender sender, List<String> list) {// if su ;
        String command = String.join(" ", list);
        String[] parts = command.split(";");// re-split with ';'
        If parser = new If();
        parser.parse(parts);
        parser.call(sender);
    }

}
