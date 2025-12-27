package com.mengcraft.reload.toolkit;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.mengcraft.reload.Main;
import com.mengcraft.reload.Utils;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CommandsAltChecker implements Listener {

    private static CommandsAltChecker instance;
    private final Map<String, AltCheckInfo> commands = Maps.newHashMap();

    public CommandsAltChecker(List<?> alts) {
        for (Object alt : alts) {
            if (alt instanceof String) {
                String altName = alt.toString();
                commands.put(altName, new AltCheckInfo(altName, null, "command." + altName + ".use"));
            } else if (alt instanceof Map) {
                Map<String, String> map = (Map<String, String>) alt;
                Optional.ofNullable(map.get("name"))
                        .map(altName -> new AltCheckInfo(altName,
                                map.get("message"),
                                map.getOrDefault("permission", "command." + altName + ".use")))
                        .ifPresent(l -> commands.put(l.getName(), l));
            }
        }
    }

    public static void load(Plugin plugin, FileConfiguration config) {
        Preconditions.checkState(instance == null);
        List<?> list = config.getList("commands_alt_checker");
        if (!list.isEmpty()) {
            Bukkit.getPluginManager().registerEvents(instance = new CommandsAltChecker(list), plugin);
            Bukkit.getLogger().info("[CommandsAltChecker] Enabled");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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
        Optional.ofNullable(commands.get(cmdName))
                .filter(l -> !p.hasPermission(l.getPermission()))
                .ifPresent(l -> {
                    event.setCancelled(true);
                    if (!Utils.isNullOrEmpty(l.getMessage())) {
                        p.sendMessage(Main.format(p, l.getMessage()));
                    }
                });
    }

    @Data
    static class AltCheckInfo {

        private final String name;
        private final String message;
        private final String permission;
    }
}
