package com.mengcraft.reload.citizens;

import com.mengcraft.reload.Main;
import com.mengcraft.reload.Utils;
import lombok.Data;
import net.citizensnpcs.api.persistence.Persister;
import net.citizensnpcs.api.util.DataKey;
import org.bukkit.entity.Player;

import javax.script.ScriptException;
import java.util.Arrays;
import java.util.List;

@Data
public class Rule implements Persister<Rule> {

    private String check;
    private List<String> cmd;
    private boolean continuous = true;
    private boolean hide;

    @Override
    public Rule create(DataKey data) {
        Rule rule = new Rule();
        rule.check = data.getString("if");
        rule.cmd = Arrays.asList(data.getString("cmd").split("\n"));
        rule.continuous = data.getBoolean("continuous", true);
        rule.hide = data.getBoolean("hide", true);
        return rule;
    }

    @Override
    public void save(Rule rule, DataKey data) {
        data.setString("if", rule.check);
        data.setString("cmd", String.join("\n", rule.cmd));
        data.setBoolean("continuous", rule.continuous);
        data.setBoolean("hide", rule.hide);
    }

    public boolean check(Player p) {
        // if script is null or empty, always return true
        if (Utils.isNullOrEmpty(check)) {
            return true;
        }
        String content = Main.format(p, check);
        if (Utils.isNullOrEmpty(content)) {
            return false;
        }
        return Utils.asBoolean(content);
    }
}
