package com.mengcraft.reload.extension;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mengcraft.reload.Main;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class ProtocolFilter {

    public static class InteractLimiter extends PacketAdapter {

        private final LoadingCache<UUID, Count> cache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.SECONDS)
                .build(CacheLoader.from(() -> new Count(0)));
        private final int deny;
        private final int kick;
        private final String message;

        public InteractLimiter(Configuration config) {
            super(JavaPlugin.getPlugin(Main.class), PacketType.Play.Client.USE_ENTITY);
            deny = config.getInt("extension.interact_rate_deny", 5) << 2;
            kick = config.getInt("extension.interact_rate_kick", 20) << 2;
            message = config.getString("extension.interact_rate_kick_message");
        }

        @Override
        public void onPacketReceiving(PacketEvent event) {
            try {
                Count count = cache.get(event.getPlayer().getUniqueId());
                if (count.count >= deny) {
                    event.setCancelled(true);
                    if (count.count >= kick) {
                        Plugin lib = ProtocolLibrary.getPlugin();
                        lib.getLogger().log(Level.WARNING, String.format("kick player %s interact too quickly! ", event.getPlayer().getName()));
                        Bukkit.getScheduler().runTask(lib, () -> event.getPlayer().kickPlayer(message));
                        return;// fast return
                    }
                }
                EnumWrappers.EntityUseAction action = event.getPacket().getEntityUseActions().read(0);
                switch (action) {
                    case ATTACK:
                        count.count += 4;
                        break;
                    case INTERACT:
                    case INTERACT_AT:
                        count.count++;
                        break;
                }
            } catch (ExecutionException ignore) {
            }
        }
    }

    private static class Count {

        private int count;

        Count(int count) {
            this.count = count;
        }
    }
}
