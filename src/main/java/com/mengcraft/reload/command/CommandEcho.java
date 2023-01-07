package com.mengcraft.reload.command;

import com.mengcraft.reload.Main;
import com.mengcraft.reload.PluginHelper;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

public class CommandEcho implements PluginHelper.IExec {

    @Override
    public void exec(CommandSender sender, List<String> list) {
        if (list.size() < 2) {
            return;
        }
        Player p = Main.getInstance().getServer().getPlayerExact(list.get(0));
        Objects.requireNonNull(p);
        String msg = String.join(" ", list.subList(1, list.size()));
        sender.sendMessage(Main.format(p, msg));
    }
}
