package com.mengcraft.reload.util;

import lombok.RequiredArgsConstructor;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class DelegateTask extends BukkitRunnable {

    private final Consumer<BukkitRunnable> task;

    @Override
    public void run() {
        task.accept(this);
    }
}
