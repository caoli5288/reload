package com.mengcraft.reload.command.control;

import com.mengcraft.reload.Main;
import com.mengcraft.reload.Utils;
import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class ExprCallable implements ICallable {

    private final String command;

    @Override
    public boolean call(CommandSender caller) {
        String cmd = Main.format(caller instanceof Player ? (Player) caller : null, command);
        return !Utils.isNullOrEmpty(cmd) && Utils.asBoolean(cmd);
    }
}
