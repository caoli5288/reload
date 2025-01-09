package com.mengcraft.reload.command;

import com.mengcraft.reload.PluginHelper;
import com.mengcraft.reload.Utils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

public class CommandMsg implements PluginHelper.IExec {

    @Override
    public void exec(CommandSender console, List<String> list) {
        Player let = Bukkit.getPlayerExact(list.get(0));
        Objects.requireNonNull(let, "Player " + list.get(0) +
                " not found");
        String text = StringUtils.join(list.subList(1, list.size()), ' ');
        // Don't compile placeholders, do it in the wrappers
        text = ChatColor.translateAlternateColorCodes('&', text);
        Utils.split(text, let::sendMessage);
    }
}
