package com.mengcraft.reload.citizens;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.TraitFactory;
import net.citizensnpcs.api.trait.TraitInfo;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CitizensManager implements Listener {

    private static final CitizensManager INSTANCE = new CitizensManager();

    @EventHandler
    public void onLeftClick(NPCLeftClickEvent event) {
        NPC npc = event.getNPC();
        if (npc.hasTrait(CommandsTrait.class)) {
            npc.getTrait(CommandsTrait.class).onClick(event.getClicker());
        }
    }

    @EventHandler
    public void onRightClick(NPCRightClickEvent event) {
        NPC npc = event.getNPC();
        if (npc.hasTrait(CommandsTrait.class)) {
            npc.getTrait(CommandsTrait.class).onClick(event.getClicker());
        }
    }

    public static void addTrait(TraitInfo info) {
        TraitFactory tf = CitizensAPI.getTraitFactory();
        tf.deregisterTrait(info);
        tf.registerTrait(info);
    }

    public static CitizensManager getInstance() {
        return INSTANCE;
    }
}
