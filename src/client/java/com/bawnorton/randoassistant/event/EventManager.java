package com.bawnorton.randoassistant.event;

import com.bawnorton.randoassistant.RandoAssistant;
import com.bawnorton.randoassistant.file.FileManager;
import com.bawnorton.randoassistant.file.config.ConfigManager;
import com.bawnorton.randoassistant.graph.LootTableMap;
import com.bawnorton.randoassistant.keybind.KeybindManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;

public class EventManager {
    public static void init() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ConfigManager.saveConfig();
            try {
                Files.write(FileManager.getLootTablePath(), FileManager.GSON.toJson(RandoAssistant.lootTableMap.getSerializedLootTableMap()).getBytes());
            } catch (Exception e) {
                RandoAssistant.LOGGER.error("Failed to save loot tables to json", e);
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            try {
                //noinspection unchecked
                RandoAssistant.lootTableMap = LootTableMap.fromSerialized(FileManager.GSON.fromJson(Files.newBufferedReader(FileManager.getLootTablePath()), Map.class));
            } catch (Exception e) {
                RandoAssistant.LOGGER.error("Failed to load loot tables from json", e);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KeybindManager.revealKeyBinding.wasPressed()) {
                MinecraftClient.getInstance().setScreen(new ConfirmScreen((result) -> {
                    if (result) {
                        RandoAssistant.addAllLootTables(Objects.requireNonNull(client.player));
                    }
                    MinecraftClient.getInstance().setScreen(null);
                }, Text.of("Add all loot tables?"), Text.of("This may cause the game client and world to lag briefly.")));
            }
            while (KeybindManager.resetKeyBinding.wasPressed()) {
                MinecraftClient.getInstance().setScreen(new ConfirmScreen((result) -> {
                    if (result) {
                        RandoAssistant.lootTableMap = new LootTableMap();
                    }
                    MinecraftClient.getInstance().setScreen(null);
                }, Text.of("Reset all loot tables?"), Text.of("This will clear all loot tables from the graph.")));
            }
        });
    }
}
