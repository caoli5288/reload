package com.mengcraft.reload.command.control;

import com.mengcraft.reload.PluginHelper;
import org.bukkit.command.CommandSender;

import java.util.List;

public class CommandIf implements PluginHelper.IExec {

    @Override
    public void exec(CommandSender sender, List<String> list) {
        String command = String.join(" ", list);
        If op = new If();
        op.compile(command);
        op.call(sender);
    }

}
