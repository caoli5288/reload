package com.mengcraft.reload.citizens;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mengcraft.reload.Main;
import lombok.Getter;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.persistence.DelegatePersistence;
import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.persistence.Persister;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.api.util.Messaging;
import net.citizensnpcs.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@TraitName("commands")
public class CommandsTrait extends Trait {

    @Persist
    private ExecutionMode executionMode = ExecutionMode.LINEAR;
    @Persist
    @DelegatePersistence(NPCCommandPersister.class)
    private Map<String, NPCCommand> commands = Maps.newHashMap();
    private final Map<UUID, PlayerNPCCommand> cooldowns = Maps.newHashMap();

    public CommandsTrait() {
        super("commands");
    }

    @EventHandler
    public void quit(PlayerQuitEvent event) {
        Player who = event.getPlayer();
        PlayerNPCCommand command = cooldowns.get(who.getUniqueId());
        if (command != null) {
            for (NPCCommand value : commands.values()) {
                command.valid(value);
            }
            if (command.isClean()) {
                cooldowns.remove(who.getUniqueId());
            }
        }
    }

    public void click(Player who) {
        List<NPCCommand> list = Lists.newArrayList(commands.values());
        if (executionMode == ExecutionMode.RANDOM) {
            if (list.size() > 1) {
                runCommand(who, list.get(Util.getFastRandom().nextInt(list.size())));
            }
        } else {
            if (executionMode == ExecutionMode.SEQUENTIAL) {
                list.sort(Comparator.comparingInt(NPCCommand::getId));
            }
            int max = !list.isEmpty() ? list.get(list.size() - 1).id : -1;
            for (NPCCommand value : commands.values()) {
                if (executionMode == ExecutionMode.SEQUENTIAL) {
                    PlayerNPCCommand info = cooldowns.get(who.getUniqueId());
                    if (info != null && value.id <= info.lastUsedId) {
                        if (info.lastUsedId == max) {
                            info.lastUsedId = -1;
                        } else {
                            continue;
                        }
                    }
                }
                runCommand(who, value);
                if (executionMode == ExecutionMode.SEQUENTIAL) {
                    break;
                }
            }
        }
    }

    private void runCommand(final Player player, NPCCommand command) {
        if (command.delay <= 0) {
            runCommand0(player, command);
        } else {
            Bukkit.getScheduler().scheduleSyncDelayedTask(CitizensAPI.getPlugin(), () -> runCommand0(player, command), command.delay);
        }
    }

    private void runCommand0(Player player, NPCCommand command) {
        PlayerNPCCommand info = cooldowns.get(player.getUniqueId());
        if (info == null && PlayerNPCCommand.requiresTracking(command)) {
            cooldowns.put(player.getUniqueId(), info = new PlayerNPCCommand());
        }
        if (info != null && !info.canUse(player, command)) {
            return;
        }
        command.run(npc, player);
    }

    public void setCommands(Map<String, NPCCommand> commands) {
        this.commands = commands;
    }

    public void setExecutionMode(ExecutionMode executionMode) {
        this.executionMode = executionMode;
    }

    private static class PlayerNPCCommand {
        Map<Integer, Long> lastUsed = Maps.newHashMap();
        int lastUsedId = -1;
        Map<Integer, Integer> nUsed = Maps.newHashMap();

        public PlayerNPCCommand() {
        }

        public boolean isClean() {
            return lastUsed.isEmpty() && nUsed.isEmpty();
        }

        public void valid(NPCCommand command) {
            int cid = command.id;
            if (lastUsed.containsKey(cid)) {
                long currentTimeSec = System.currentTimeMillis() / 1000;
                long deadline = ((Number) lastUsed.get(cid)).longValue() + command.cooldown;
                if (currentTimeSec >= deadline) {
                    lastUsed.remove(cid);
                }
            }
        }

        public boolean canUse(Player player, NPCCommand command) {
            for (String perm : command.perms) {
                if (!player.hasPermission(perm)) {
                    return false;
                }
            }
            long currentTimeSec = System.currentTimeMillis() / 1000;
            int commandKey = command.id;
            if (lastUsed.containsKey(commandKey)) {
                long deadline = ((Number) lastUsed.get(commandKey)).longValue() + command.cooldown;
                if (currentTimeSec < deadline) {
                    return false;
                }
                lastUsed.remove(commandKey);
            }
            int previouslyUsed = nUsed.getOrDefault(commandKey, 0);
            if (command.n > 0 && command.n <= previouslyUsed) {
                return false;
            }
            if (command.cooldown > 0) {
                lastUsed.put(commandKey, currentTimeSec);
            }
            if (command.n > 0) {
                nUsed.put(commandKey, previouslyUsed + 1);
            }
            lastUsedId = command.id;
            return true;
        }

        public static boolean requiresTracking(NPCCommand command) {
            return command.cooldown > 0 || command.n > 0
                    || (command.perms != null && !command.perms.isEmpty());
        }
    }

    public enum ExecutionMode {
        LINEAR,
        RANDOM,
        SEQUENTIAL
    }

    private static class NPCCommandPersister implements Persister<NPCCommand> {
        public NPCCommandPersister() {
        }

        @Override
        public NPCCommand create(DataKey root) {
            List<String> perms = Lists.newArrayList();
            for (DataKey key : root.getRelative("permissions").getIntegerSubKeys()) {
                perms.add(key.getString(""));
            }
            return new NPCCommand(Integer.parseInt(root.name()), root.getString("command"),
                    Boolean.parseBoolean(root.getString("player")),
                    Boolean.parseBoolean(root.getString("op")), root.getInt("cooldown"), perms, root.getInt("n"),
                    root.getInt("delay"));
        }

        @Override
        public void save(NPCCommand instance, DataKey root) {
            root.setString("command", instance.command);
            root.setBoolean("player", instance.player);
            root.setBoolean("op", instance.op);
            root.setInt("cooldown", instance.cooldown);
            root.setInt("n", instance.n);
            root.setInt("delay", instance.delay);
            for (int i = 0; i < instance.perms.size(); i++) {
                root.setString("permissions." + i, instance.perms.get(i));
            }
        }
    }

    public static class NPCCommand {
        String command;
        int cooldown;
        int delay;
        @Getter
        int id;
        int n;
        boolean op;
        List<String> perms;
        boolean player;

        public NPCCommand(int id, String command, boolean player, boolean op, int cooldown,
                          List<String> perms, int n, int delay) {
            this.id = id;
            this.command = command;
            this.player = player;
            this.op = op;
            this.cooldown = cooldown;
            this.perms = perms;
            this.n = n;
            this.delay = delay;
        }

        public void run(NPC npc, Player clicker) {
            String interpolatedCommand = Main.format(clicker, command);
            if (Messaging.isDebugging()) {
                Messaging.debug(
                        "Running command " + interpolatedCommand + " on NPC " + npc.getId() + " clicker " + clicker);
            }
            if (!player) {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), interpolatedCommand);
                return;
            }
            boolean wasOp = clicker.isOp();
            if (op) {
                clicker.setOp(true);
            }

            try {
                clicker.chat("/" + interpolatedCommand);
            } catch (Throwable t) {
                t.printStackTrace();
            }

            if (op) {
                clicker.setOp(wasOp);
            }
        }
    }
}
