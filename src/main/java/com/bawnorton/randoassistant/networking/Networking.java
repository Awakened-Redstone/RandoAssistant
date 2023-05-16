package com.bawnorton.randoassistant.networking;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class Networking {
    public static final Object SERVER_LOCK = new Object();
    public static MinecraftServer server;
    private static boolean initialized = false;

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(NetworkingConstants.HANDSHAKE_PACKET, (server, player, handler, buf, responseSender) -> sendHandshakePacket(player));
        ServerPlayNetworking.registerGlobalReceiver(NetworkingConstants.STATS_PACKET, (server, player, handler, buf, responseSender) -> waitForServer(() -> player.getStatHandler().sendStats(player)));
    }

    public static void sendSerializeablePacket(ServerPlayerEntity player, Serializeable serializeable) {
        waitForServer(() -> {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBytes(serializeable.toBytes());
            ServerPlayNetworking.send(player, serializeable.getTypePacket(), buf);
        });
    }

    public static void sendClearCachePacket(ServerPlayerEntity serverPlayer) {
        waitForServer(() -> {
            PacketByteBuf buf = PacketByteBufs.create();
            ServerPlayNetworking.send(serverPlayer, NetworkingConstants.CLEAR_CACHE_PACKET, buf);
        });
    }

    public static void sendDebugPacket(ServerPlayerEntity player, Item item) {
        waitForServer(() -> {
            PacketByteBuf buf = PacketByteBufs.create();
            Block block = Block.getBlockFromItem(item);
            buf.writeItemStack(item.getDefaultStack());
            buf.writeRegistryValue(Registries.CUSTOM_STAT, block.getLootTableId());
            ServerPlayNetworking.send(player, NetworkingConstants.DEBUG_PACKET, buf);
        });
    }

    public static void sendHandshakePacket(ServerPlayerEntity player) {
        waitForServer(() -> {
            ServerPlayNetworking.send(player, NetworkingConstants.HANDSHAKE_PACKET, PacketByteBufs.create());
            player.getStatHandler().sendStats(player);
        });
    }

    private static void waitForServer(Runnable runnable) {
        if(server == null || !initialized) {
            new Thread(() -> {
                synchronized (SERVER_LOCK) {
                    while (server == null) {
                        try {
                            SERVER_LOCK.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                initialized = true;
                runnable.run();
            }).start();
        } else {
            runnable.run();
        }
    }
}
