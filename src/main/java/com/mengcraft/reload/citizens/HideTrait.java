package com.mengcraft.reload.citizens;

import com.google.common.collect.Lists;
import com.mengcraft.reload.Main;
import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.List;

public class HideTrait extends Trait {

    @Persist
    private List<Rule> rules = Lists.newArrayList();

    public HideTrait() {
        super("visible");
    }

    @Override
    public void onSpawn() {
        if (npc.getEntity().getType() == EntityType.PLAYER) {
            CitizensManager.getInstance().getHides().put(npc.getUniqueId(), npc);
        }
    }

    @Override
    public void onRemove() {
        onDespawn();
    }

    @Override
    public void onDespawn() {
        if (npc.getEntity().getType() == EntityType.PLAYER) {
            CitizensManager.getInstance().getHides().remove(npc.getUniqueId());
        }
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    public void onJoinWorld(Player p) {
        Entity entity = npc.getEntity();
        if (!p.getWorld().equals(entity.getWorld())) {
            return;
        }
        for (Rule rule : rules) {
            if (rule.check(p)) {
                if (rule.isHide()) {
                    p.hidePlayer(Main.getInstance(), (Player) entity);
                }
                // just return
                return;
            }
        }
    }
}
