package com.mengcraft.reload.citizens;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.mengcraft.reload.Main;
import com.mengcraft.reload.Utils;
import lombok.Getter;
import lombok.Setter;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.exception.NPCLoadException;
import net.citizensnpcs.api.npc.MemoryNPCDataStore;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.api.util.Messaging;
import net.citizensnpcs.trait.ArmorStandTrait;
import net.citizensnpcs.util.BoundingBox;
import net.citizensnpcs.util.NMS;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Collection;
import java.util.List;

/**
 * Persists a hologram attached to the NPC.
 */
@TraitName("holograms")
public class HologramsTrait extends Trait implements IReloadable {

    private final List<NPC> lineHolograms = Lists.newArrayList();
    @Persist
    private final List<String> lines = Lists.newArrayList();
    private final NPCRegistry registry = CitizensAPI.createAnonymousNPCRegistry(new MemoryNPCDataStore());
    private Location currentLoc;
    @Persist
    private HologramDirection direction = HologramDirection.TOP_DOWN;
    private boolean lastNameplateVisible;
    @Persist
    private double lineHeight = -1;
    @Getter
    @Setter
    private Refresh refresh = Refresh.SLOW;
    private int ticks;

    public HologramsTrait() {
        super("holograms");
    }

    /**
     * Adds a new hologram line which will displayed over an NPC's head.
     *
     * @param text The new line to add
     */
    public void addLine(String text) {
        lines.add(text);
        onDespawn();
        onSpawn();
    }

    /**
     * Clears all hologram lines
     */
    public void clear() {
        onDespawn();
        lines.clear();
    }

    @Override
    public void onReload() {
        onDespawn();
    }

    private NPC createHologram(String line, double heightOffset) {
        NPC hologramNPC = registry.createNPC(EntityType.ARMOR_STAND, line);
        ArmorStandTrait trait = hologramNPC.getTrait(ArmorStandTrait.class);
        trait.setVisible(false);
        trait.setSmall(true);
        trait.setMarker(true);
        trait.setGravity(false);
        trait.setHasArms(false);
        trait.setHasBaseplate(false);
        hologramNPC.spawn(currentLoc.clone().add(0,
                getEntityHeight()
                        + (direction == HologramDirection.BOTTOM_UP ? heightOffset : getMaxHeight() - heightOffset),
                0));
        return hologramNPC;
    }

    /**
     * @return The direction that hologram lines are displayed in
     */
    public HologramDirection getDirection() {
        return direction;
    }

    /**
     * @param direction The new direction
     * @see #getDirection()
     */
    public void setDirection(HologramDirection direction) {
        this.direction = direction;
        onDespawn();
        onSpawn();
    }

    private double getEntityHeight() {
        BoundingBox box = NMS.getBoundingBox(npc.getEntity());
        return box.maxY - box.minY;
    }

    private double getHeight(int lineNumber) {
        return (lineHeight == -1 ? 0.4D : lineHeight)
                * (lastNameplateVisible ? lineNumber + 1 : lineNumber);
    }

    /**
     * Note: this is implementation-specific and may be removed at a later date.
     */
    public Collection<ArmorStand> getHologramEntities() {
        return Collections2.transform(lineHolograms, (n) -> (ArmorStand) n.getEntity());
    }

    /**
     * @return The line height between each hologram line, in blocks
     */
    public double getLineHeight() {
        return lineHeight;
    }

    /**
     * Sets the line height
     *
     * @param height The line height in blocks
     * @see #getLineHeight()
     */
    public void setLineHeight(double height) {
        lineHeight = height;
        onDespawn();
        onSpawn();
    }

    @Override
    public void load(DataKey key) throws NPCLoadException {
        if (key.keyExists("refresh")) {
            refresh = Refresh.valueOf(key.getString("refresh"));
        }
    }

    @Override
    public void save(DataKey key) {
        key.setString("refresh", refresh.name());
    }

    /**
     * @return the hologram lines, in bottom-up order
     */
    public List<String> getLines() {
        return lines;
    }

    private double getMaxHeight() {
        return (lineHeight == -1 ? 0.4D : lineHeight)
                * (lines.size() + 1);
    }

    @Override
    public void onDespawn() {
        for (NPC npc : lineHolograms) {
            npc.destroy();
        }
        lineHolograms.clear();
    }

    @Override
    public void onRemove() {
        onDespawn();
    }

    @Override
    public void onSpawn() {
        if (!npc.isSpawned())
            return;
        lastNameplateVisible = Boolean
                .parseBoolean(npc.data().<Object>get(NPC.NAMEPLATE_VISIBLE_METADATA, true).toString());
        currentLoc = npc.getStoredLocation();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            lineHolograms.add(createHologram(Main.format(Utils.castPlayer(npc.getEntity()), line), getHeight(i)));
        }
    }

    /**
     * Removes the line at the specified index
     *
     * @param idx
     */
    public void removeLine(int idx) {
        lines.remove(idx);
        onDespawn();
        onSpawn();
    }

    @Override
    public void run() {
        if (!npc.isSpawned()) {
            onDespawn();
            return;
        }
        if (ticks++ % refresh.getValue() != 0) {// Call every ${updateTicks} ticks
            return;
        }
        if (currentLoc == null) {
            currentLoc = npc.getStoredLocation();
        }
        boolean nameplateVisible = Boolean
                .parseBoolean(npc.data().<Object>get(NPC.NAMEPLATE_VISIBLE_METADATA, true).toString());
        boolean update = currentLoc.getWorld() != npc.getStoredLocation().getWorld()
                || currentLoc.distance(npc.getStoredLocation()) >= 0.001 || lastNameplateVisible != nameplateVisible;
        lastNameplateVisible = nameplateVisible;

        if (update) {
            currentLoc = npc.getStoredLocation();
        }
        for (int i = 0; i < lineHolograms.size(); i++) {
            NPC hologramNPC = lineHolograms.get(i);
            if (!hologramNPC.isSpawned())
                continue;
            if (update) {
                hologramNPC.teleport(currentLoc.clone().add(0, getEntityHeight() + getHeight(i), 0),
                        PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
            if (i >= lines.size()) {
                Messaging.severe("More hologram NPCs than lines for ID", npc.getId(), "lines", lines);
                break;
            }
            String s = Main.format(Utils.castPlayer(npc.getEntity()), lines.get(i));
            if (s != null && !ChatColor.stripColor(s).isEmpty()) {
                hologramNPC.setName(s);
                hologramNPC.data().set(NPC.NAMEPLATE_VISIBLE_METADATA, true);
            } else {
                hologramNPC.setName("");
                hologramNPC.data().set(NPC.NAMEPLATE_VISIBLE_METADATA, false);
            }
        }
    }

    /**
     * Sets the hologram line at a specific index
     *
     * @param idx  The index
     * @param text The new line
     */
    public void setLine(int idx, String text) {
        if (idx == lines.size()) {
            lines.add(text);
        } else {
            lines.set(idx, text);
            if (idx < lineHolograms.size()) {
                lineHolograms.get(idx).setName(Main.format(Utils.castPlayer(npc.getEntity()), text));
                return;
            }
        }
        onDespawn();
        onSpawn();
    }

    public enum HologramDirection {
        BOTTOM_UP,
        TOP_DOWN;
    }

    public enum Refresh {
        FASTEST(2),
        FAST(10),
        MEDIUM(20),
        SLOW(100),
        SLOWEST(200);

        private final int value;

        Refresh(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}