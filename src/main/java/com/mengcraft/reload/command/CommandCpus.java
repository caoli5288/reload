package com.mengcraft.reload.command;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.mengcraft.reload.Main;
import com.mengcraft.reload.PluginHelper;
import com.mengcraft.reload.Utils;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.join;

public class CommandCpus implements PluginHelper.IExec {

    @Override
    public void exec(CommandSender sender, List<String> list) {
        Utils.ensureLinux();
        if (list.isEmpty()) {
            try {
                Process exec = Runtime.getRuntime().exec("taskset -pc " + Utils.pid());
                exec.waitFor();
                sender.sendMessage(new String(ByteStreams.toByteArray(exec.getInputStream())));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            set(Integer.parseInt(list.get(0)));
            sender.sendMessage("Set cpus to " + list.get(0));
        }
    }

    public static void set(int cpus) {
        Utils.ensureLinux();
        String cmd = "taskset -pc %s %s";
        int processors = Runtime.getRuntime().availableProcessors();
        if (cpus <= 0 || cpus >= processors) {
            cmd = String.format(cmd, "0-" + (processors - 1), Utils.pid());
        } else {
            List<Integer> cpuList = Lists.newArrayList(processors);
            for (int i = 0; i < processors; i++) {
                cpuList.add(i);
            }
            Collections.shuffle(cpuList);
            cpuList = cpuList.subList(0, cpus);
            cmd = String.format(cmd, join(cpuList, ","), Utils.pid());
        }
        try {
            Process exec = Runtime.getRuntime().exec(cmd);
            exec.waitFor();
            Main.getInstance().getLogger().info(new String(ByteStreams.toByteArray(exec.getInputStream())));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
