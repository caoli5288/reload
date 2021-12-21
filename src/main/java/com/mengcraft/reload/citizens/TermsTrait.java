package com.mengcraft.reload.citizens;

import com.mengcraft.reload.Main;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public class TermsTrait extends Trait {

    private LocalDateTime from;
    private LocalDateTime to;

    public TermsTrait() {
        super("terms");
    }

    @Override
    public void load(DataKey key) {
        if (key.keyExists("from")) {
            from = LocalDateTime.parse(key.getString("from"));
        }
        if (key.keyExists("to")) {
            to = LocalDateTime.parse(key.getString("to"));
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
}
