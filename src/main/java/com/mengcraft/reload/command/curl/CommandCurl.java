package com.mengcraft.reload.command.curl;

import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.mengcraft.reload.PluginHelper;
import com.mengcraft.reload.Utils;
import lombok.SneakyThrows;
import org.bukkit.command.CommandSender;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class CommandCurl implements PluginHelper.IExec {

    private final Map<String, ICurl> processors = Maps.newHashMap();

    public CommandCurl() {
        ICurl simpleCurl = (cmd, context, contents) -> cmd.sendMessage(connect(context, contents));
        processors.put("http", simpleCurl);
        processors.put("https", simpleCurl);
    }

    @Override
    public void exec(CommandSender sender, List<String> list) {// curl 0:url 1:data
        URI context = URI.create(list.get(0));
        processors.get(context.getScheme()).call(sender, context, Utils.take(list, 1));
    }

    @SneakyThrows
    public static String connect(URI context, String contents) {
        HttpURLConnection conn = (HttpURLConnection) context.toURL().openConnection();
        conn.setRequestProperty("Host", context.getHost());
        conn.setRequestProperty("User-Agent", "curl/6.6.6");
        conn.setRequestProperty("Accept", "*/*");
        if (Utils.isNullOrEmpty(contents)) {
            try {
                conn.connect();
                return new String(ByteStreams.toByteArray(conn.getInputStream()));
            } finally {
                conn.disconnect();
            }
        } else {
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            try {
                conn.connect();
                OutputStream out = conn.getOutputStream();
                out.write(contents.getBytes(StandardCharsets.UTF_8));
                out.flush();
                return new String(ByteStreams.toByteArray(conn.getInputStream()), StandardCharsets.UTF_8);
            } finally {
                conn.disconnect();
            }
        }
    }
}
