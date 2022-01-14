package com.mengcraft.reload.citizens;

import com.mengcraft.reload.Main;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import net.citizensnpcs.api.util.DataKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@TraitName("terms")
public class TermsTrait extends Trait implements ITrait {

    private LocalDateTime from;
    private LocalDateTime to;

    public TermsTrait() {
        super("terms");
    }

    @Override
    public void load(DataKey key) {
        if (key.keyExists("from")) {
            Object obj = key.getRaw("from");
            if (obj instanceof Date) {
                from = ((Date) obj).toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
            } else {
                from = LocalDateTime.parse(obj.toString());
            }
        }
        if (key.keyExists("to")) {
            Object obj = key.getRaw("to");
            if (obj instanceof Date) {
                to = ((Date) obj).toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
            } else {
                to = LocalDateTime.parse(obj.toString());
            }
        }
    }

    @Override
    public void save(DataKey key) {
        if (from != null) {
            key.setString("from", from.toString());
        }
        if (to != null) {
            key.setString("to", to.toString());
        }
    }

    @Override
    public void onSpawn() {
        LocalDateTime current = LocalDateTime.now();
        if (from != null && current.isBefore(from)) {
            Location loc = npc.getStoredLocation();
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> npc.despawn());
            Main.executor().schedule(() -> Bukkit.getScheduler().runTask(Main.getInstance(), () -> npc.spawn(loc)),
                    current.until(from, ChronoUnit.MILLIS),
                    TimeUnit.MILLISECONDS);
        }
        if (to != null) {
            if (to.isAfter(current)) {
                Main.executor().schedule(() -> Bukkit.getScheduler().runTask(Main.getInstance(), () -> npc.despawn()),
                        current.until(to, ChronoUnit.MILLIS),
                        TimeUnit.MILLISECONDS);
            } else {
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> npc.despawn());
            }
        }
    }

    @Override
    public void onReload() {
    }
}
