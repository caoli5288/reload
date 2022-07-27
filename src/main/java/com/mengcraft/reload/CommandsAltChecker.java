package com.mengcraft.reload;

import com.google.common.collect.Sets;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;
import java.util.Set;

public class CommandsAltChecker implements Listener {

    private final Set<String> commands;

    public CommandsAltChecker(List<String> altChecker) {
        commands = Sets.newHashSet(altChecker);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();
        if (p.isOp()) {
            return;
        }
        String cmdName = event.getMessage().split(" ", 2)[0].substring(1);
        Command command = Utils.getCommandMap().getCommand(cmdName);
        if (command == null) {
            event.setCancelled(true);// silently
            return;
        }
        cmdName = command.getName();
        if (commands.contains(cmdName) && !p.hasPermission("command." + cmdName + ".use")) {
            event.setCancelled(true);
        }
    }
}
