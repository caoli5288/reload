package com.mengcraft.reload;

import com.google.common.collect.Sets;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

public class CommandsAltChecker implements Listener {

    private final Set<String> commands;
    private CommandMap commandMap;

    public CommandsAltChecker(List<String> altChecker) {
        commands = Sets.newHashSet(altChecker);
        Server server = Bukkit.getServer();
        try {
            Field field = server.getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            commandMap = (CommandMap) field.get(server);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();
        if (p.isOp()) {
            return;
        }
        String cmd = event.getMessage().split(" ", 2)[0].substring(1);
        Command command = commandMap.getCommand(cmd);
        if (command == null) {
            return;
        }
        String cmdName = command.getName();
        if (commands.contains(cmdName) && !p.hasPermission("command." + cmdName + ".use")) {
            event.setCancelled(true);
        }
    }
}
