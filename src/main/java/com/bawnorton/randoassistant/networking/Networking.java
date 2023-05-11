package com.bawnorton.randoassistant.networking;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class Networking {
    public static final Object SERVER_LOCK = new Object();
    public static MinecraftServer server;

    private static boolean initialized = false;

    public static void sendLootTablePacket(ServerPlayerEntity player, SerializeableLootTable lootTable) {
        waitForServer(() -> {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBytes(lootTable.toBytes());
            ServerPlayNetworking.send(player, NetworkingConstants.LOOT_TABLE_PACKET, buf);
        });
    }

    public static void sendInteractionPacket(ServerPlayerEntity player, SerializeableInteraction interaction) {
        waitForServer(() -> {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBytes(interaction.toBytes());
            ServerPlayNetworking.send(player, NetworkingConstants.INTERACTION_PACKET, buf);
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
            buf.writeItemStack(item.getDefaultStack());
            ServerPlayNetworking.send(player, NetworkingConstants.DEBUG_PACKET, buf);
        });
    }

    private static void waitForServer(Runnable runnable) {
        if(!initialized) {
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
