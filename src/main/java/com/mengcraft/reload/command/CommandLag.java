package com.mengcraft.reload.command;

import com.mengcraft.reload.Main;
import com.mengcraft.reload.PluginHelper;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class CommandLag implements PluginHelper.IExec {

    private Task task;

    @Override
    public void exec(CommandSender sender, List<String> list) {
        if (list.isEmpty()) {
            if (task == null) {
                task = new Task();
                task.runTaskTimer(Main.getInstance(), 1, 1);
                sender.sendMessage("§aLag started.");
            } else {
                task.cancel();
                task = null;
                sender.sendMessage("§cLag stopped.");
            }
        } else {
            int time = Integer.parseInt(list.get(0));
            if (task == null) {
                task = new Task();
                task.runTaskTimer(Main.getInstance(), 1, 1);
            }
            task.time = time;
            sender.sendMessage("§aLag started with " + time +
                    "ms per ticks.");
        }
    }

    static class Task extends BukkitRunnable {

        private int time = 20;

        @Override
        public void run() {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
