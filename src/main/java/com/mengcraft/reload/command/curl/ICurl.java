package com.mengcraft.reload.command.curl;

import org.bukkit.command.CommandSender;

import java.net.URI;

public interface ICurl {

    void call(CommandSender cmd, URI context, String contents);
}
