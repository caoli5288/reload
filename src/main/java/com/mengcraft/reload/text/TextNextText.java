package com.mengcraft.reload.text;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.mengcraft.reload.Main;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TextNextText extends TextAbstract {

    final Map<String, List<String>> fileCache = Maps.newHashMap();

    public TextNextText(Plugin owner) {
        super(owner, "nexttext");
        addSubcommand("", this::gen);
    }

    private String gen(Player player, List<String> list) {
        // %nexttext_@file_name.txt%
        // %nexttext_a_b_c%
        if (list.get(0).charAt(0) == '@') {
            String line = StringUtils.join(list, '_');
            List<String> lines = fileCache.computeIfAbsent(line.substring(1), this::load);
            return lines.get(RandomUtils.nextInt(0, lines.size()));
        }
        List<String> all = Lists.newArrayList();
        for (String line : list) {
            if (line.charAt(0) == '@') {
                all.addAll(fileCache.computeIfAbsent(line.substring(1), this::load));
            } else {
                all.add(line);
            }
        }
        return all.get(RandomUtils.nextInt(0, all.size()));
    }

    @SneakyThrows
    private List<String> load(String filename) {
        File file = new File(Main.getInstance().getDataFolder(), filename);
        if (file.isFile()) {
            return Files.readLines(file, StandardCharsets.UTF_8);
        }
        return Collections.emptyList();
    }
}
