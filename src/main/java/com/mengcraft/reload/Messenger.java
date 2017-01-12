package com.mengcraft.reload;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 16-4-13.
 */
public class Messenger {

    private final static ArrayList<String> EMPTY_LIST = new ArrayList<>();
    private final static String PREFIX = "message.";
    protected final Main main;

    public Messenger(Main main) {
        this.main = main;
    }

    public void send(CommandSender p, String path) {
        send(p, path, "");
    }

    public void send(CommandSender p, String path, String def) {
        sendMessage(p, find(path, def));
    }

    public void sendList(CommandSender p, String path, List<String> def) {
        for (String line : findList(path, def)) {
            sendMessage(p, line);
        }
    }

    public void sendList(CommandSender p, String path) {
        sendList(p, path, EMPTY_LIST);
    }

    public String find(String path) {
        return find(path, "");
    }

    public List<String> findList(String path) {
        return findList(path, EMPTY_LIST);
    }

    public List<String> findList(String path, List<String> def) {
        List<String> found = main.getConfig().getStringList(with(path));
        if (found.isEmpty()) {
            if (!def.isEmpty()) {
                main.getConfig().set(with(path), found = def);
                main.saveConfig();
            }
        }
        return found;
    }

    public String find(String path, String def) {
        String found = main.getConfig().getString(with(path), "");
        if (found.isEmpty()) {
            if (!def.isEmpty()) {
                main.getConfig().set(with(path), found = def);
                main.saveConfig();
            }
        }
        return found;
    }

    public void sendMessage(CommandSender p, String text) {
        p.sendMessage(ChatColor.translateAlternateColorCodes('&', text));
    }

    private String with(String str) {
        return PREFIX + str;
    }

}
