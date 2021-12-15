package com.mengcraft.reload.command.at;

import com.google.common.base.Preconditions;
import com.mengcraft.reload.PluginHelper;
import org.bukkit.command.CommandSender;

import java.util.List;

public class CommandAt implements PluginHelper.IExec {

    @Override
    public void exec(CommandSender sender, List<String> list) {// at 0:label 1:commands
        Preconditions.checkArgument(list.size() > 1);
        AtScheduler.getInstance().at(sender, list.get(0), String.join(" ", list.subList(1, list.size())));
    }
}
