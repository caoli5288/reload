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
        cdMap = CacheBuilder.newBuilder()
                .expireAfterWrite(Math.max(0, cd), TimeUnit.MILLISECONDS)
                .<UUID, String>build()
                .asMap();
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
        UUID uuid = p.getUniqueId();
        if (cdMap.containsKey(uuid)) {
            return;
        }
        cdMap.put(uuid, "");
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        for (String s : commands) {
            Bukkit.dispatchCommand(console, Main.format(p, s));
        }
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
