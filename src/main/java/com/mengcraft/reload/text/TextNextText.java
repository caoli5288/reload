package com.mengcraft.reload.text;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.mengcraft.reload.Main;
import lombok.SneakyThrows;
import org.apache.commons.lang.math.RandomUtils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TextNextText extends TextAbstract {

    final Map<String, Collection<String>> fileCache = Maps.newHashMap();

    public TextNextText(Plugin owner) {
        super(owner, "nexttext");
        addSubcommand("", this::gen);
    }

    private String gen(Player player, List<String> list) {
        // %nexttext_@file_name.txt%
        // %nexttext_a_b_c%
        List<String> all = Lists.newArrayList();
        for (String line : list) {
            if (line.charAt(0) == '@') {
                all.addAll(fileCache.computeIfAbsent(line.substring(1), this::load));
            } else {
                all.add(line);
            }
        }
        return all.get(RandomUtils.nextInt(all.size()));
    }

    @SneakyThrows
    private Collection<String> load(String filename) {
        File file = new File(Main.getInstance().getDataFolder(), filename);
        if (file.isFile()) {
            return Files.readLines(file, StandardCharsets.UTF_8);
        }
        return Collections.emptySet();
    }
}
