package com.mengcraft.reload.text;

import org.apache.commons.lang.math.RandomUtils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class TextNextInt extends TextAbstract {

    public TextNextInt(Plugin plugin) {
        super(plugin, "nextint");
        addSubcommand("", this::nextInt);
    }

    public String nextInt(Player who, List<String> cmd) {
        // %nextint_100% => 42
        // %nextint_1_100% => 42
        if (cmd.isEmpty()) {
            return "0";
        }
        if (cmd.size() == 1) {
            int max = Integer.parseInt(cmd.get(0));
            return String.valueOf(RandomUtils.nextInt(max));
        } else {
            int min = Integer.parseInt(cmd.get(0));
            int max = Integer.parseInt(cmd.get(1));
            return String.valueOf(min + RandomUtils.nextInt(max - min));
        }
    }
}
