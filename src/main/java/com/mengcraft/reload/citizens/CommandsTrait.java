package com.mengcraft.reload.citizens;

import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.mengcraft.reload.Main;
import com.mengcraft.reload.Utils;
import lombok.Data;
import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.persistence.PersistenceLoader;
import net.citizensnpcs.api.persistence.Persister;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import net.citizensnpcs.api.util.DataKey;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@TraitName("commands")
public class CommandsTrait extends Trait implements IClickable {

    @Persist
    @NotNull
    private List<String> commands = Lists.newArrayList();
    @NotNull
    private Mode mode = Mode.CONSOLE;
    private int cd;
    private Map<UUID, String> cdMap;
    @Persist
    private List<Rule> rules = Lists.newArrayList();

    static {
        PersistenceLoader.registerPersistDelegate(Rule.class, Rule.class);
    }

    public CommandsTrait() {
        super("commands");
    }

    @Override
    public void load(DataKey key) {
        setCd(key.keyExists("cd") ?
                key.getInt("cd") :
                500);
        setMode(Mode.valueOf(key.getString("mode", Mode.CONSOLE.name())));
    }

    @Override
    public void save(DataKey key) {
        key.setInt("cd", cd);
        key.setString("mode", mode.name());
    }

    @Override
    public void onClick(Player p) {
        if (cd(p.getUniqueId())) {
            return;
        }
        if (!Utils.isNullOrEmpty(commands)) {
            mode.accept(p, commands);
        }
        if (!Utils.isNullOrEmpty(rules)) {
            for (Rule rule : rules) {
                if (rule.check(p)) {
                    mode.accept(p, rule.getCmd());
                    if (!rule.isContinuous()) {
                        break;
                    }
                }
            }
        }
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    private boolean cd(UUID uuid) {
        if (cd > 0) {
            if (cdMap.containsKey(uuid)) {
                return true;
            }
            cdMap.put(uuid, "");
        }
        return false;
    }

    @NotNull
    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(@NotNull List<String> commands) {
        this.commands = commands;
    }

    @NotNull
    public Mode getMode() {
        return mode;
    }

    public void setMode(@NotNull Mode mode) {
        this.mode = mode;
    }

    public int getCd() {
        return cd;
    }

    public void setCd(int cd) {
        this.cd = cd;
        if (cd > 0) {
            cdMap = CacheBuilder.newBuilder()
                    .expireAfterWrite(cd, TimeUnit.MILLISECONDS)
                    .<UUID, String>build()
                    .asMap();
        }
    }

    @Data
    public static class Rule implements Persister<Rule> {

        private String ifScript;
        private List<String> cmd;
        private boolean continuous = true;

        @Override
        public Rule create(DataKey data) {
            Rule rule = new Rule();
            rule.ifScript = data.getString("if");
            rule.cmd = Arrays.asList(data.getString("cmd").split("\n"));
            rule.continuous = data.getBoolean("continuous", true);
            return rule;
        }

        @Override
        public void save(Rule rule, DataKey data) {
            data.setString("if", rule.ifScript);
            data.setString("cmd", String.join("\n", rule.cmd));
            data.setBoolean("continuous", rule.continuous);
        }

        public boolean check(Player p) {
            // if script is null or empty, always return true
            if (Utils.isNullOrEmpty(ifScript)) {
                return true;
            }
            String content = Main.format(p, ifScript);
            if (Utils.isNullOrEmpty(content)) {
                return false;
            }
            try {
                Object result = Utils.SCRIPT_ENGINE.eval(content);
                if (result instanceof Boolean) {
                    return (boolean) result;
                }
            } catch (ScriptException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    public enum Mode implements BiConsumer<Player, List<String>> {

        CONSOLE {
            @Override
            public void accept(Player p, List<String> cmd) {
                ConsoleCommandSender console = Bukkit.getConsoleSender();
                for (String s : cmd) {
                    Bukkit.dispatchCommand(console, Main.format(p, s));
                }
            }
        },

        PLAYER {
            @Override
            public void accept(Player p, List<String> cmd) {
                for (String s : cmd) {
                    p.chat("/" + Main.format(p, s));
                }
            }
        };

        @Override
        public void accept(Player p, List<String> cmd) {

        }
    }
}
