package com.mengcraft.reload.command;

import com.google.common.base.Preconditions;
import com.mengcraft.reload.PluginHelper;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandVelocity implements PluginHelper.IExec {

    @Override
    public void exec(CommandSender sender, List<String> list) {
        Player p = Bukkit.getPlayerExact(list.get(0));
        Preconditions.checkNotNull(p, "Player not found");
        if (list.size() > 1) {
            p.setVelocity(p.getLocation().getDirection().multiply(Double.parseDouble(list.get(1))));
        } else {
            p.setVelocity(p.getLocation().getDirection());
        }
    }
}
