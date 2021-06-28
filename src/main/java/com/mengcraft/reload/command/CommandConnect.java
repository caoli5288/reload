package com.mengcraft.reload.command;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mengcraft.reload.Main;
import com.mengcraft.reload.PluginHelper;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

public class CommandConnect implements PluginHelper.IExec {

    @Override
    public void exec(CommandSender sender, List<String> list) {
        Player p = Main.getInstance().getServer().getPlayerExact(list.get(0));
        Objects.requireNonNull(p);
        connect(p, list.get(1));
    }

    public static void connect(Player p, String server) {
        Main.getInstance().getLogger().info(String.format("Connect %s to %s", p.getName(), server));
        ByteArrayDataOutput buff = ByteStreams.newDataOutput();
        buff.writeUTF("ConnectOther");
        buff.writeUTF(p.getName());
        buff.writeUTF(server);
        p.sendPluginMessage(Main.getInstance(), "BungeeCord", buff.toByteArray());
    }
}
