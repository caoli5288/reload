package com.mengcraft.reload.citizens;

import org.bukkit.entity.Player;

public interface ITrait {

    void onReload();

    void onClick(Player p);
}
