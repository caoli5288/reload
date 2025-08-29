package com.mengcraft.reload;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mengcraft.reload.command.CommandUptime;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.Executors;

public class AliveValidator {

    private boolean killEnable;
    private boolean dumpEnable;
    private int maxKillRetries;
    private long killTimeout;
    private long tickKillMillis;
    private int killRetries;
    private HttpServer http;

    @SneakyThrows
    public void load(FileConfiguration options) {
        killEnable = options.getBoolean("valid_server_alive", false);
        killTimeout = 1000L * options.getInt("healthz.timeout", 10);
        maxKillRetries = options.getInt("healthz.retries", 3);
        dumpEnable = options.getBoolean("extension.auto_dump");
        if (options.getBoolean("healthz.enable", false)) {
            http = HttpServer.create(new InetSocketAddress(options.getInt("healthz.port", 3000)), 0);
            http.setExecutor(Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                    .setNameFormat("AliveValidator Thread")
                    .setDaemon(true)
                    .build()));
            http.createContext("/healthz", new HealthZ());
            http.start();
            Main.log("Healthz endpoint started", null);
        }
    }

    public void disable() {
        Optional.ofNullable(http)
                .ifPresent(l -> l.stop(0));
    }

    public void tick(Main main, Ticker ticker) {
        if (killEnable) {
            tickKill(main, ticker);
        }
    }

    private void tickKill(Main main, Ticker ticker) {
        long millis = System.currentTimeMillis();
        if (millis > tickKillMillis) {
            tickKillMillis = millis + killTimeout;
            if (ticker.getShort() < 1) {
                killRetries++;
                if (killRetries >= maxKillRetries) {
                    killEnable = false;
                    main.getLogger().severe("TPS < 1, preparing kill server");
                    if (dumpEnable) {
                        main.dump(false);
                    }
                    main.getLogger().severe("Kill server");
                    main.shutdown(true);
                }
            } else {
                killRetries = 0;
            }
        }
    }

    public static class HealthZ implements HttpHandler {

        private long oldTicks;
        private JSONObject log;
        private boolean lag;

        @Override
        public void handle(HttpExchange get) throws IOException {
            Ticker ticker = Main.getTicker();
            long ticks = ticker.tick();
            if (oldTicks != ticks) {
                oldTicks = ticks;
                log = new JSONObject();
                lag = ticker.getShort() < 1;
                log.put("status", lag ? "DOWN" : "UP");
                JSONObject details = new JSONObject();
                details.put("uptime", CommandUptime.of().uptime());
                details.put("ticks", ticks);
                JSONObject tps = new JSONObject();
                tps.put("short", ticker.getShort());
                tps.put("medium", ticker.getMedium());
                tps.put("long", ticker.getLong());
                details.put("tps", tps);
                details.put("players", Bukkit.getOnlinePlayers().size());
                JSONObject memory = new JSONObject();
                memory.put("used", Runtime.getRuntime().totalMemory() / 1024 / 1024);
                memory.put("max", Runtime.getRuntime().maxMemory() / 1024 / 1024);
                memory.put("free", Runtime.getRuntime().freeMemory() / 1024 / 1024);
                details.put("memory", memory);
                log.put("details", details);
            }
            byte[] bytes = log.toJSONString().getBytes(StandardCharsets.UTF_8);
            get.getResponseHeaders().set("Content-Type", "application/json");
            get.sendResponseHeaders(lag ? 500 : 200, bytes.length);
            try (OutputStream buf = get.getResponseBody()) {
                buf.write(bytes);
            }
        }
    }
}
