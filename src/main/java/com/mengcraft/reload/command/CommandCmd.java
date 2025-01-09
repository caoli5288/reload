package com.mengcraft.reload.command;

import com.google.common.collect.Lists;
import com.mengcraft.reload.PluginHelper;
import com.mengcraft.reload.Utils;
import org.apache.commons.lang.math.RandomUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.NumberConversions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CommandCmd implements PluginHelper.IExec {

    @Override
    public void exec(CommandSender sender, List<String> list) {
        // cmd <player_name> <command...>
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        if (!onlinePlayers.isEmpty() && list.size() > 1) {
            String seg = list.get(0);
            Collection<? extends Player> all = null;
            if (seg.charAt(0) == '@') {
                if (seg.equals("@a")) {
                    all = onlinePlayers;
                } else if (seg.equals("@r")) {
                    Collection<? extends Player> coll = Bukkit.getOnlinePlayers();
                    Player let = coll.stream()
                            .skip(RandomUtils.nextInt(coll.size()))
                            .findFirst()
                            .orElse(null);
                    if (let != null) {
                        all = Collections.singleton(let);
                    }
                } else {
                    int pMax = NumberConversions.toInt(seg.substring(1));
                    if (pMax >= onlinePlayers.size()) {
                        all = onlinePlayers;
                    } else {
                        ArrayList<? extends Player> shuffling = Lists.newArrayList(onlinePlayers);
                        Collections.shuffle(shuffling);
                        all = shuffling.subList(0, pMax);
                    }
                }
            } else {
                Player exact = Bukkit.getPlayerExact(seg);
                if (exact != null) {
                    all = Collections.singleton(exact);
                }
            }
            if (!Utils.isNullOrEmpty(all)) {
                String commands = String.join(" ", list.subList(1, list.size()));
                CommandCmdAll.execute(sender, all, commands);
            }
        }
    }
}
