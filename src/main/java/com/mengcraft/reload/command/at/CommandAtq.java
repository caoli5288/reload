package com.mengcraft.reload.command.at;

import com.mengcraft.reload.PluginHelper;
import com.mengcraft.reload.Utils;
import org.bukkit.command.CommandSender;

import java.util.List;

public class CommandAtq implements PluginHelper.IExec {

    @Override
    public void exec(CommandSender sender, List<String> list) {
        AtScheduler.getInstance().atq(sender, Utils.pick(list, 0), Utils.pick(list, 1));
    }
}
