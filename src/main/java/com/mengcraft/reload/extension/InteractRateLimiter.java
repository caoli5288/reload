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
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class InteractRateLimiter {

    public static class Listener extends PacketAdapter {

        private final LoadingCache<UUID, Count> cache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.SECONDS)
                .build(CacheLoader.from(() -> new Count(0)));

        public Listener() {
            super(JavaPlugin.getPlugin(Main.class), PacketType.Play.Client.USE_ENTITY);
        }

        @Override
        public void onPacketReceiving(PacketEvent event) {
            try {
                Count count = cache.get(event.getPlayer().getUniqueId());
                if (count.count >= 20) {
                    ProtocolLibrary.getPlugin().getLogger().log(Level.WARNING, String.format("player %s interact too quickly!", event.getPlayer().getName()));
                    event.setCancelled(true);
                } else {
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
