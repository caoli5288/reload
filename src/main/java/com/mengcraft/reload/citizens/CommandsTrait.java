package com.mengcraft.reload.citizens;

import com.google.common.cache.CacheBuilder;
import com.mengcraft.reload.Main;
import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import net.citizensnpcs.api.util.DataKey;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@TraitName("commands")
public class CommandsTrait extends Trait implements ITrait {

    @Persist
    private List<String> commands;
    private Map<UUID, String> cdMap;
    private int cd;

    public CommandsTrait() {
        super("commands");
    }

    @Override
    public void load(DataKey key) {
        cd = key.keyExists("cd") ?
                key.getInt("cd") :
                500;
        if (cd > 0) {
            cdMap = CacheBuilder.newBuilder()
                    .expireAfterWrite(cd, TimeUnit.MILLISECONDS)
                    .<UUID, String>build()
                    .asMap();
        }
    }

    @Override
    public void save(DataKey key) {
        key.setInt("cd", cd);
    }

    @Override
    public void onReload() {
        onDespawn();
    }

    @Override
    public void onClick(Player p) {
        if (cd(p.getUniqueId())) {
            return;
        }
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        for (String s : commands) {
            Bukkit.dispatchCommand(console, Main.format(p, s));
        }
    }

    private boolean cd(UUID uuid) {
        if (cdMap != null) {
            if (cdMap.containsKey(uuid)) {
                return true;
            }
            cdMap.put(uuid, "");
        }
        return false;
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }

    @Override
    public void onSpawn() {
        CitizensService.register(npc.getUniqueId(), this);
    }

    @Override
    public void onDespawn() {
        CitizensService.unregister(npc.getUniqueId());
    }
}
