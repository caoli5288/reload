package com.mengcraft.reload.command.control;

import com.mengcraft.reload.Main;
import com.mengcraft.reload.Utils;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class ExprCallable implements ICallable {

    private final String command;

    @Override
    public boolean call(CommandSender caller) {
        return call(caller instanceof Player ? (Player) caller : null, command);
    }

    public static boolean call(Player caller, String command) {
        String cmd = Main.format(caller, command);
        return !Utils.isNullOrEmpty(cmd) && Utils.asBoolean(cmd);
    }
}
