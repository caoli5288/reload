package com.mengcraft.reload.citizens;

import com.mengcraft.reload.Main;
import com.mengcraft.reload.Utils;
import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import org.bukkit.Bukkit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@TraitName("timelines")
public class TimelinesTrait extends Trait {

    @Persist
    private Mode mode = Mode.DATETIME;
    @Persist
    private String from;
    @Persist
    private String to;

    public TimelinesTrait() {
        super("timelines");
    }

    @Override
    public void onSpawn() {
        if (mode == Mode.DATETIME) {
            LocalDateTime time = LocalDateTime.now();
            if (from != null) {
                LocalDateTime f = LocalDateTime.parse(from);
                if (time.isBefore(f)) {
                    Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> npc.despawn(), 1);
                }
            }
            if (to != null) {
                LocalDateTime t = LocalDateTime.parse(to);
                if (t.isAfter(time)) {
                    Main.executor().schedule(() -> Bukkit.getScheduler().runTask(Main.getInstance(), () -> npc.despawn()),
                            time.until(t, ChronoUnit.MILLIS),
                            TimeUnit.MILLISECONDS);
                } else {
                    Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> npc.despawn(), 1);
                }
            }
        } else if (mode == Mode.TIME) {
            LocalTime time = LocalTime.now();
            LocalTime f = LocalTime.MIN;
            if (!Utils.isNullOrEmpty(from)) {
                f = LocalTime.parse(from);
            }
            if (time.isBefore(f)) {
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> npc.despawn(), 1);
            } else {
                LocalTime t = LocalTime.MAX;
                if (to != null) {
                    t = LocalTime.parse(to);
                }
                if (time.isBefore(t)) {
                    Main.executor().schedule(() -> Bukkit.getScheduler().runTask(Main.getInstance(), () -> npc.despawn()),
                            time.until(t, ChronoUnit.MILLIS),
                            TimeUnit.MILLISECONDS);
                } else {
                    Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> npc.despawn(), 1);
                }
            }
        }
    }

    @Override
    public void onDespawn() {
        if (mode == Mode.DATETIME) {
            if (from != null) {
                LocalDateTime time = LocalDateTime.now();
                LocalDateTime f = LocalDateTime.parse(from);
                if (time.isBefore(f)) {
                    Main.executor().schedule(() -> Bukkit.getScheduler().runTask(Main.getInstance(), () -> npc.spawn(npc.getStoredLocation())),
                            f.until(time, ChronoUnit.MILLIS),
                            TimeUnit.MILLISECONDS);
                }
            }
        } else if (mode == Mode.TIME) {
            LocalDateTime time = LocalDateTime.now();
            LocalDate date = LocalDate.now();
            LocalDateTime f = date.atStartOfDay();
            if (from != null) {
                f = LocalDateTime.of(date, LocalTime.parse(from));
            }
            LocalDateTime t = LocalDateTime.of(date, LocalTime.MAX);
            if (to != null) {
                t = LocalDateTime.of(date, LocalTime.parse(to));
            }
            // logic
            if (time.isBefore(f)) {
                // time is before 'from'
                Main.executor().schedule(() -> Bukkit.getScheduler().runTask(Main.getInstance(), () -> npc.spawn(npc.getStoredLocation())),
                        time.until(f, ChronoUnit.MILLIS),
                        TimeUnit.MILLISECONDS);
            } else {
                // time is after or equal 'from'
                if (time.isBefore(t)) {
                    // time is before 'to', so spawn it immediately
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> npc.spawn(npc.getStoredLocation()));
                } else {
                    // time is after or equal 'to', so spawn it tomorrow
                    Main.executor().schedule(() -> Bukkit.getScheduler().runTask(Main.getInstance(), () -> npc.spawn(npc.getStoredLocation())),
                            time.until(f.plusDays(1), ChronoUnit.MILLIS),
                            TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    public enum Mode {
        DATETIME,
        TIME
    }
}
